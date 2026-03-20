package com.pathshashtra.backend.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathshashtra.backend.profile.UserProfile;
import com.pathshashtra.backend.profile.UserProfileRepository;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final ClaudeApiService claudeApiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizService(QuizRepository quizRepository,
                       UserRepository userRepository,
                       UserProfileRepository profileRepository,
                       ClaudeApiService claudeApiService) {
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.claudeApiService = claudeApiService;
    }

    // ── START QUIZ: generate questions from Claude ───────────────────────
    public QuizStartResponse startQuiz(String email) {
        User user = getUser(email);

        // Build profile context for Claude
        String profileContext = buildProfileContext(user);

        // Call Claude to generate questions
        String questionsJson = claudeApiService.generateQuizQuestions(profileContext);

        // Save quiz session
        QuizSession session = new QuizSession();
        session.setUser(user);
        session.setQuestionsJson(questionsJson);
        session.setStatus(QuizSession.QuizStatus.STARTED);
        quizRepository.save(session);

        // Parse and return questions to frontend
        return parseQuestionsResponse(session.getId(), questionsJson);
    }

    // ── SUBMIT QUIZ: analyze answers, return full result ────────────────
    public QuizResult submitQuiz(String email, QuizSubmitRequest request) {
        User user = getUser(email);

        QuizSession session = quizRepository
                .findByIdAndUserId(request.getSessionId(), user.getId())
                .orElseThrow(() -> new RuntimeException("Quiz session not found"));

        if (session.getStatus() == QuizSession.QuizStatus.COMPLETED) {
            throw new RuntimeException("Quiz already completed");
        }

        // Save answers
        try {
            String answersJson = objectMapper.writeValueAsString(request.getAnswers());
            session.setAnswersJson(answersJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save answers");
        }

        // Call Claude to analyze
        String profileContext = buildProfileContext(user);
        String resultJson = claudeApiService.analyzeQuizAnswers(
                profileContext,
                session.getQuestionsJson(),
                session.getAnswersJson()
        );

        // Save result and mark completed
        session.setResultJson(resultJson);
        session.setStatus(QuizSession.QuizStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        quizRepository.save(session);

        // Parse and return result
        return parseQuizResult(resultJson);
    }

    // ── GET PAST RESULTS ─────────────────────────────────────────────────
    public List<QuizResultSummary> getMyResults(String email) {
        User user = getUser(email);
        List<QuizSession> sessions = quizRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId());

        List<QuizResultSummary> summaries = new ArrayList<>();
        for (QuizSession s : sessions) {
            if (s.getStatus() == QuizSession.QuizStatus.COMPLETED && s.getResultJson() != null) {
                try {
                    QuizResult result = parseQuizResult(s.getResultJson());
                    String topCareer = result.getCareerMatches().isEmpty()
                            ? "N/A"
                            : result.getCareerMatches().get(0).getTitle();
                    summaries.add(new QuizResultSummary(
                            s.getId(), topCareer, result.getSummary(), s.getCompletedAt()
                    ));
                } catch (Exception ignored) {}
            }
        }
        return summaries;
    }

    // ── GET SINGLE RESULT ────────────────────────────────────────────────
    public QuizResult getResult(String email, Long sessionId) {
        User user = getUser(email);
        QuizSession session = quizRepository
                .findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new RuntimeException("Result not found"));

        if (session.getResultJson() == null) {
            throw new RuntimeException("Quiz not completed yet");
        }
        return parseQuizResult(session.getResultJson());
    }

    // ── HELPERS ──────────────────────────────────────────────────────────
    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String buildProfileContext(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(user.getName()).append("\n");

        profileRepository.findByUserId(user.getId()).ifPresent(profile -> {
            sb.append("Education: ").append(profile.getEducationLevel()).append("\n");
            sb.append("Career Goal: ").append(profile.getCareerGoal()).append("\n");
            sb.append("Experience Level: ").append(profile.getExperienceLevel()).append("\n");
            sb.append("Current Skills: ").append(profile.getSkills()).append("\n");
        });

        return sb.toString();
    }

    private QuizStartResponse parseQuestionsResponse(Long sessionId, String json) {
        try {
            // Strip markdown code fences if Claude adds them
            String clean = json.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode root = objectMapper.readTree(clean);
            JsonNode questionsNode = root.path("questions");

            List<QuizStartResponse.QuizQuestion> questions = new ArrayList<>();
            for (JsonNode q : questionsNode) {
                List<String> options = new ArrayList<>();
                for (JsonNode opt : q.path("options")) {
                    options.add(opt.asText());
                }
                questions.add(new QuizStartResponse.QuizQuestion(
                        q.path("number").asInt(),
                        q.path("question").asText(),
                        options
                ));
            }
            return new QuizStartResponse(sessionId, questions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse questions from Claude: " + e.getMessage());
        }
    }

    private QuizResult parseQuizResult(String json) {
        try {
            String clean = json.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode root = objectMapper.readTree(clean);

            // Career matches
            List<QuizResult.CareerMatch> careers = new ArrayList<>();
            for (JsonNode c : root.path("careerMatches")) {
                List<String> skills = new ArrayList<>();
                for (JsonNode s : c.path("requiredSkills")) skills.add(s.asText());
                careers.add(new QuizResult.CareerMatch(
                        c.path("title").asText(),
                        c.path("matchPercent").asInt(),
                        c.path("reason").asText(),
                        skills
                ));
            }

            // Skill gaps
            List<String> gaps = new ArrayList<>();
            for (JsonNode g : root.path("skillGaps")) gaps.add(g.asText());

            // Roadmap
            List<QuizResult.RoadmapStep> roadmap = new ArrayList<>();
            for (JsonNode r : root.path("roadmap")) {
                List<String> actions = new ArrayList<>();
                List<String> resources = new ArrayList<>();
                for (JsonNode a : r.path("actions")) actions.add(a.asText());
                for (JsonNode res : r.path("resources")) resources.add(res.asText());
                roadmap.add(new QuizResult.RoadmapStep(
                        r.path("phase").asInt(),
                        r.path("title").asText(),
                        r.path("duration").asText(),
                        actions,
                        resources
                ));
            }

            // Salary
            JsonNode sal = root.path("salaryInfo");
            QuizResult.SalaryInfo salary = new QuizResult.SalaryInfo(
                    sal.path("role").asText(),
                    sal.path("entryLevel").asText(),
                    sal.path("midLevel").asText(),
                    sal.path("seniorLevel").asText(),
                    sal.path("growthOutlook").asText()
            );

            return QuizResult.builder()
                    .summary(root.path("summary").asText())
                    .careerMatches(careers)
                    .skillGaps(gaps)
                    .roadmap(roadmap)
                    .salaryInfo(salary)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Claude result: " + e.getMessage());
        }
    }
}
