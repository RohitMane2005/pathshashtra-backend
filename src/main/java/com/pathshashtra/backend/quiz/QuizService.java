package com.pathshashtra.backend.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathshashtra.backend.common.JsonCleaner;
import com.pathshashtra.backend.profile.UserProfileRepository;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;

/**
 * FIX (CRITICAL): submitQuizWithToken() and getPublicResult() were declared
 * OUTSIDE the class body — the closing brace on line 193 ended the class,
 * leaving orphaned methods after it. Java compile error masked by pre-built
 * .class files in /target. Fixed: closing brace moved to after all methods.
 */
@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final ClaudeApiService claudeApiService;
    private final JsonCleaner jsonCleaner;
    private final ObjectMapper objectMapper;

    public QuizService(QuizRepository quizRepository,
                       UserRepository userRepository,
                       UserProfileRepository profileRepository,
                       ClaudeApiService claudeApiService,
                       JsonCleaner jsonCleaner,
                       ObjectMapper objectMapper) {
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.claudeApiService = claudeApiService;
        this.jsonCleaner = jsonCleaner;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public QuizStartResponse startQuiz(String email) {
        User user = getUser(email);
        String profileContext = buildProfileContext(user);
        String questionsJson = claudeApiService.generateQuizQuestions(profileContext);

        QuizSession session = new QuizSession();
        session.setUser(user);
        session.setQuestionsJson(questionsJson);
        session.setStatus(QuizSession.QuizStatus.STARTED);
        quizRepository.save(session);

        return parseQuestionsResponse(session.getId(), questionsJson);
    }

    @Transactional
    public QuizResult submitQuiz(String email, QuizSubmitRequest request) {
        User user = getUser(email);
        QuizSession session = quizRepository
                .findByIdAndUserId(request.getSessionId(), user.getId())
                .orElseThrow(() -> new RuntimeException("Quiz session not found"));

        if (session.getStatus() == QuizSession.QuizStatus.COMPLETED) {
            throw new RuntimeException("Quiz already completed");
        }

        try {
            session.setAnswersJson(objectMapper.writeValueAsString(request.getAnswers()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save answers");
        }

        String profileContext = buildProfileContext(user);
        String resultJson = claudeApiService.analyzeQuizAnswers(
                profileContext, session.getQuestionsJson(), session.getAnswersJson());

        session.setResultJson(resultJson);
        session.setStatus(QuizSession.QuizStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());

        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        session.setShareToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        quizRepository.save(session);

        return parseQuizResult(resultJson);
    }

    public List<QuizResultSummary> getMyResults(String email) {
        User user = getUser(email);
        List<QuizSession> sessions = quizRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<QuizResultSummary> summaries = new ArrayList<>();
        for (QuizSession s : sessions) {
            if (s.getStatus() == QuizSession.QuizStatus.COMPLETED && s.getResultJson() != null) {
                try {
                    QuizResult result = parseQuizResult(s.getResultJson());
                    String topCareer = result.getCareerMatches().isEmpty()
                            ? "N/A" : result.getCareerMatches().get(0).getTitle();
                    summaries.add(new QuizResultSummary(
                            s.getId(), topCareer, result.getSummary(), s.getCompletedAt()));
                } catch (Exception ignored) {}
            }
        }
        return summaries;
    }

    public QuizResult getResult(String email, Long sessionId) {
        User user = getUser(email);
        QuizSession session = quizRepository
                .findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new RuntimeException("Result not found"));

        if (session.getResultJson() == null) throw new RuntimeException("Quiz not completed yet");
        return parseQuizResult(session.getResultJson());
    }

    /**
     * FIX BUG 14: Inlined submission logic to avoid double DB lookups.
     * Previously called submitQuiz() then re-fetched the same session and user.
     * Now fetches each resource exactly once.
     */
    @Transactional
    public Map<String, Object> submitQuizWithToken(String email, QuizSubmitRequest request) {
        User user = getUser(email);
        QuizSession session = quizRepository
                .findByIdAndUserId(request.getSessionId(), user.getId())
                .orElseThrow(() -> new RuntimeException("Quiz session not found"));

        if (session.getStatus() == QuizSession.QuizStatus.COMPLETED) {
            throw new RuntimeException("Quiz already completed");
        }

        try {
            session.setAnswersJson(objectMapper.writeValueAsString(request.getAnswers()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save answers");
        }

        String profileContext = buildProfileContext(user);
        String resultJson = claudeApiService.analyzeQuizAnswers(
                profileContext, session.getQuestionsJson(), session.getAnswersJson());

        session.setResultJson(resultJson);
        session.setStatus(QuizSession.QuizStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());

        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        session.setShareToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        quizRepository.save(session);

        QuizResult result = parseQuizResult(resultJson);

        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("shareToken", session.getShareToken());
        resp.put("summary", result.getSummary());
        resp.put("careerMatches", result.getCareerMatches());
        resp.put("skillGaps", result.getSkillGaps());
        resp.put("roadmap", result.getRoadmap());
        resp.put("salaryInfo", result.getSalaryInfo());
        return resp;
    }

    /** Public read-only result by share token — no auth required. */
    public Map<String, Object> getPublicResult(String shareToken) {
        QuizSession session = quizRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new RuntimeException("Result not found"));
        if (session.getResultJson() == null) throw new RuntimeException("Quiz not completed");

        QuizResult result = parseQuizResult(session.getResultJson());
        Map<String, Object> resp = new HashMap<>();
        resp.put("summary", result.getSummary());
        resp.put("careerMatches", result.getCareerMatches());
        resp.put("salaryInfo", result.getSalaryInfo());
        resp.put("completedAt", session.getCompletedAt());
        return resp;
    }

    // ─── private helpers ──────────────────────────────────────────────────────

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
            JsonNode root = jsonCleaner.cleanAndParse(json);
            List<QuizStartResponse.QuizQuestion> questions = new ArrayList<>();
            for (JsonNode q : root.path("questions")) {
                List<String> options = new ArrayList<>();
                for (JsonNode opt : q.path("options")) options.add(opt.asText());
                questions.add(new QuizStartResponse.QuizQuestion(
                        q.path("number").asInt(), q.path("question").asText(), options));
            }
            return new QuizStartResponse(sessionId, questions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse quiz questions: " + e.getMessage());
        }
    }

    private QuizResult parseQuizResult(String json) {
        try {
            JsonNode root = jsonCleaner.cleanAndParse(json);

            List<QuizResult.CareerMatch> careers = new ArrayList<>();
            for (JsonNode c : root.path("careerMatches")) {
                List<String> skills = new ArrayList<>();
                for (JsonNode s : c.path("requiredSkills")) skills.add(s.asText());
                careers.add(new QuizResult.CareerMatch(
                        c.path("title").asText(), c.path("matchPercent").asInt(),
                        c.path("reason").asText(), skills));
            }

            List<String> gaps = new ArrayList<>();
            for (JsonNode g : root.path("skillGaps")) gaps.add(g.asText());

            List<QuizResult.RoadmapStep> roadmap = new ArrayList<>();
            for (JsonNode r : root.path("roadmap")) {
                List<String> actions = new ArrayList<>();
                List<String> resources = new ArrayList<>();
                for (JsonNode a : r.path("actions")) actions.add(a.asText());
                for (JsonNode res : r.path("resources")) resources.add(res.asText());
                roadmap.add(new QuizResult.RoadmapStep(
                        r.path("phase").asInt(), r.path("title").asText(),
                        r.path("duration").asText(), actions, resources));
            }

            JsonNode sal = root.path("salaryInfo");
            QuizResult.SalaryInfo salary = new QuizResult.SalaryInfo(
                    sal.path("role").asText(), sal.path("entryLevel").asText(),
                    sal.path("midLevel").asText(), sal.path("seniorLevel").asText(),
                    sal.path("growthOutlook").asText());

            return QuizResult.builder()
                    .summary(root.path("summary").asText())
                    .careerMatches(careers).skillGaps(gaps)
                    .roadmap(roadmap).salaryInfo(salary).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse quiz result: " + e.getMessage());
        }
    }
} // class closes here — after all methods
