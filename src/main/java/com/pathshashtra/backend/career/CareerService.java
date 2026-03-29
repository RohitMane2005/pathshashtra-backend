package com.pathshashtra.backend.career;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathshashtra.backend.common.JsonCleaner;
import com.pathshashtra.backend.profile.UserProfileRepository;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CareerService {

    private final CareerAssessmentRepository assessmentRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final CareerAIService careerAIService;
    private final JsonCleaner jsonCleaner;
    private final ObjectMapper objectMapper;

    public CareerService(CareerAssessmentRepository assessmentRepository,
                         UserRepository userRepository,
                         UserProfileRepository profileRepository,
                         CareerAIService careerAIService,
                         JsonCleaner jsonCleaner,
                         ObjectMapper objectMapper) {
        this.assessmentRepository = assessmentRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.careerAIService = careerAIService;
        this.jsonCleaner = jsonCleaner;
        this.objectMapper = objectMapper;
    }

    /** Returns AI-generated psychometric questions */
    public Map<String, Object> getQuestions(String email) {
        User user = getUser(email);
        String profileCtx = buildProfileContext(user);
        String questionsJson = careerAIService.generateAssessmentQuestions(profileCtx);

        try {
            JsonNode root = objectMapper.readTree(jsonCleaner.clean(questionsJson));
            return Map.of("questions", root.path("questions"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse career questions: " + e.getMessage());
        }
    }

    /** Saves answers and triggers AI analysis, returns full result */
    @Transactional
    public CareerResult submitAssessment(String email, CareerSubmitRequest request) {
        User user = getUser(email);
        String profileCtx = buildProfileContext(user);

        // Fetch questions from cache or regenerate (same profile = same cache key)
        String questionsJson = careerAIService.generateAssessmentQuestions(profileCtx);

        String answersJson;
        try {
            answersJson = objectMapper.writeValueAsString(request.getAnswers());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize answers");
        }

        String resultJson = careerAIService.analyzeAssessment(profileCtx, questionsJson, answersJson);
        CareerResult result = parseResult(resultJson);

        // Persist assessment
        CareerAssessment assessment = new CareerAssessment();
        assessment.setUser(user);
        assessment.setAnswersJson(answersJson);
        assessment.setResultJson(resultJson);
        assessment.setStatus(CareerAssessment.AssessmentStatus.COMPLETED);
        assessment.setCompletedAt(LocalDateTime.now());

        if (!result.getTopCareers().isEmpty()) {
            assessment.setTopCareer(result.getTopCareers().get(0).getTitle());
            assessment.setTopMatchScore(result.getTopCareers().get(0).getMatchPercent());
        }

        assessmentRepository.save(assessment);
        return result;
    }

    /** Lists all past assessments for a user */
    public List<CareerAssessmentSummary> getMyAssessments(String email) {
        User user = getUser(email);
        List<CareerAssessment> assessments =
                assessmentRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<CareerAssessmentSummary> summaries = new ArrayList<>();
        for (CareerAssessment a : assessments) {
            if (a.getStatus() == CareerAssessment.AssessmentStatus.COMPLETED && a.getResultJson() != null) {
                try {
                    CareerResult result = parseResult(a.getResultJson());
                    summaries.add(new CareerAssessmentSummary(
                            a.getId(),
                            a.getTopCareer(),
                            a.getTopMatchScore(),
                            result.getPersonalitySummary(),
                            a.getCompletedAt()
                    ));
                } catch (Exception ignored) {}
            }
        }
        return summaries;
    }

    /** Fetches a specific assessment result by ID */
    public CareerResult getResult(String email, Long assessmentId) {
        User user = getUser(email);
        CareerAssessment assessment = assessmentRepository
                .findByIdAndUserId(assessmentId, user.getId())
                .orElseThrow(() -> new RuntimeException("Assessment not found"));

        if (assessment.getResultJson() == null)
            throw new RuntimeException("Assessment not completed yet");

        return parseResult(assessment.getResultJson());
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String buildProfileContext(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(user.getName()).append("\n");
        profileRepository.findByUserId(user.getId()).ifPresent(profile -> {
            sb.append("Education Level: ").append(profile.getEducationLevel()).append("\n");
            sb.append("Career Goal: ").append(profile.getCareerGoal()).append("\n");
            sb.append("Experience Level: ").append(profile.getExperienceLevel()).append("\n");
            sb.append("Current Skills: ").append(profile.getSkills()).append("\n");
        });
        return sb.toString();
    }

    private CareerResult parseResult(String json) {
        try {
            JsonNode root = objectMapper.readTree(jsonCleaner.clean(json));

            // Parse top careers
            List<CareerResult.CareerPath> careers = new ArrayList<>();
            for (JsonNode c : root.path("topCareers")) {
                List<String> skills = new ArrayList<>();
                for (JsonNode s : c.path("keySkills")) skills.add(s.asText());
                careers.add(new CareerResult.CareerPath(
                        c.path("title").asText(),
                        c.path("matchPercent").asInt(),
                        c.path("whyItFits").asText(),
                        skills,
                        c.path("indianMarketOutlook").asText(),
                        c.path("topCompanies").asText()
                ));
            }

            // Parse skill gaps
            List<String> gaps = new ArrayList<>();
            for (JsonNode g : root.path("skillGaps")) gaps.add(g.asText());

            // Parse personality traits
            List<String> traits = new ArrayList<>();
            for (JsonNode t : root.path("personalityTraits")) traits.add(t.asText());

            // Parse salary insight
            JsonNode sal = root.path("salaryInsight");
            CareerResult.SalaryInsight salary = new CareerResult.SalaryInsight(
                    sal.path("role").asText(),
                    sal.path("entryLevel").asText(),
                    sal.path("midLevel").asText(),
                    sal.path("seniorLevel").asText(),
                    sal.path("growthOutlook").asText(),
                    sal.path("topHiringCities").asText()
            );

            // Parse next steps
            List<CareerResult.ActionStep> steps = new ArrayList<>();
            for (JsonNode s : root.path("nextSteps")) {
                steps.add(new CareerResult.ActionStep(
                        s.path("step").asInt(),
                        s.path("title").asText(),
                        s.path("description").asText(),
                        s.path("timeframe").asText()
                ));
            }

            return CareerResult.builder()
                    .personalitySummary(root.path("personalitySummary").asText())
                    .strengthsOverview(root.path("strengthsOverview").asText())
                    .personalityTraits(traits)
                    .topCareers(careers)
                    .skillGaps(gaps)
                    .salaryInsight(salary)
                    .nextSteps(steps)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse career result: " + e.getMessage());
        }
    }
}
