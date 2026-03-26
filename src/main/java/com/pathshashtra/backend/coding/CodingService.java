package com.pathshashtra.backend.coding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathshashtra.backend.common.JsonCleaner;
import com.pathshashtra.backend.profile.UserProfileRepository;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CodingService {

    private static final Logger log = LoggerFactory.getLogger(CodingService.class);


    private final CodingProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final GrokCodingService grokCodingService;
    private final JsonCleaner jsonCleaner;
    private final ObjectMapper objectMapper;

    public CodingService(CodingProblemRepository problemRepository,
                         UserRepository userRepository,
                         UserProfileRepository profileRepository,
                         GrokCodingService grokCodingService,
                         JsonCleaner jsonCleaner,
                         ObjectMapper objectMapper) {
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.grokCodingService = grokCodingService;
        this.jsonCleaner = jsonCleaner;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> generateProblem(String email, ProblemGenerateRequest request) {
        User user = getUser(email);
        String problemJson = grokCodingService.generateProblem(
                request.getTopic(), request.getDifficulty(), request.getLanguage());
        String cleaned = jsonCleaner.clean(problemJson);
        String title = extractField(cleaned, "title", "DSA Problem");

        CodingProblem problem = new CodingProblem();
        problem.setUser(user);
        problem.setTopic(request.getTopic());
        problem.setDifficulty(request.getDifficulty());
        problem.setLanguage(request.getLanguage());
        problem.setProblemTitle(title);
        problem.setProblemJson(cleaned);
        problem.setStatus(CodingProblem.ProblemStatus.GENERATED);
        problemRepository.save(problem);

        Map<String, Object> response = new HashMap<>();
        response.put("problemId", problem.getId());
        response.put("problem", parseJson(cleaned));
        return response;
    }

    @Transactional
    public Map<String, Object> getHint(String email, HintRequest request) {
        User user = getUser(email);
        CodingProblem problem = problemRepository
                .findByIdAndUserId(request.getProblemId(), user.getId())
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        if (problem.getHintsUsed() >= 3) {
            throw new RuntimeException("Maximum 3 hints allowed per problem");
        }

        String hintJson = grokCodingService.generateHint(
                problem.getProblemJson(),
                request.getCurrentCode() != null ? request.getCurrentCode() : "",
                problem.getHintsUsed()
        );

        problem.setHintsUsed(problem.getHintsUsed() + 1);
        if (problem.getStatus() == CodingProblem.ProblemStatus.GENERATED) {
            problem.setStatus(CodingProblem.ProblemStatus.ATTEMPTED);
        }
        problemRepository.save(problem);

        Map<String, Object> response = new HashMap<>();
        response.put("hintsUsed", problem.getHintsUsed());
        response.put("hintsRemaining", 3 - problem.getHintsUsed());
        response.put("hint", parseJson(jsonCleaner.clean(hintJson)));
        return response;
    }

    @Transactional
    public CodeFeedback submitCode(String email, CodeSubmitRequest request) {
        User user = getUser(email);
        CodingProblem problem = problemRepository
                .findByIdAndUserId(request.getProblemId(), user.getId())
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        String feedbackJson = grokCodingService.reviewCode(
                problem.getProblemJson(), request.getCode(), request.getLanguage());

        problem.setSubmittedCode(request.getCode());
        problem.setFeedbackJson(feedbackJson);
        problem.setStatus(CodingProblem.ProblemStatus.REVIEWED);

        CodeFeedback feedback = parseFeedback(jsonCleaner.clean(feedbackJson));
        if (feedback.isCorrect()) {
            problem.setStatus(CodingProblem.ProblemStatus.SOLVED);
            problem.setSolvedAt(LocalDateTime.now());
        }
        problemRepository.save(problem);
        return feedback;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getMyProblems(String email, Pageable pageable) {
        User user = getUser(email);
        Page<CodingProblem> page = problemRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        List<Map<String, Object>> result = new ArrayList<>();
        for (CodingProblem p : page.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("title", p.getProblemTitle());
            item.put("topic", p.getTopic());
            item.put("difficulty", p.getDifficulty());
            item.put("language", p.getLanguage());
            item.put("status", p.getStatus());
            item.put("hintsUsed", p.getHintsUsed());
            item.put("createdAt", p.getCreatedAt());
            item.put("problemJson", parseJson(p.getProblemJson()));
            item.put("submittedCode", p.getSubmittedCode());
            result.add(item);
        }
        return new PageImpl<>(result, pageable, page.getTotalElements());
    }



    /** Load a single saved problem by ID — used to resume from the problems list. */
    @Transactional(readOnly = true)
    public Map<String, Object> getProblemById(String email, Long problemId) {
        User user = getUser(email);
        log.debug("Loading problem {} for user {} (id={})", problemId, email, user.getId());
        CodingProblem problem = problemRepository
                .findByIdAndUserId(problemId, user.getId())
                .orElseThrow(() -> new RuntimeException("Problem not found: id=" + problemId + " for user=" + email));

        Map<String, Object> response = new HashMap<>();
        response.put("problemId", problem.getId());
        response.put("problem", parseJson(problem.getProblemJson()));
        response.put("submittedCode", problem.getSubmittedCode());
        response.put("feedbackJson", parseJson(problem.getFeedbackJson()));
        response.put("hintsUsed", problem.getHintsUsed());
        response.put("status", problem.getStatus());
        response.put("language", problem.getLanguage());
        response.put("topic", problem.getTopic());
        response.put("difficulty", problem.getDifficulty());
        return response;
    }

    /** Reset a problem so the user can attempt it again fresh. Clears code, feedback, hints. */
    @Transactional
    public Map<String, Object> retryProblem(String email, Long problemId) {
        User user = getUser(email);
        CodingProblem problem = problemRepository
                .findByIdAndUserId(problemId, user.getId())
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        problem.setSubmittedCode(null);
        problem.setFeedbackJson(null);
        problem.setHintsUsed(0);
        problem.setStatus(CodingProblem.ProblemStatus.GENERATED);
        problem.setSolvedAt(null);
        problemRepository.save(problem);

        Map<String, Object> response = new HashMap<>();
        response.put("problemId", problem.getId());
        response.put("problem", parseJson(problem.getProblemJson()));
        return response;
    }

    @Transactional
    public Map<String, Object> getDsaRoadmap(String email, String targetGoal) {
        User user = getUser(email);
        final String[] level = {"Beginner"};
        profileRepository.findByUserId(user.getId()).ifPresent(p -> {
            if (p.getExperienceLevel() != null) level[0] = p.getExperienceLevel();
        });

        String roadmapJson = grokCodingService.generateRoadmap(user.getName(), level[0], targetGoal);
        Map<String, Object> response = new HashMap<>();
        response.put("roadmap", parseJson(jsonCleaner.clean(roadmapJson)));
        return response;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String extractField(String json, String field, String defaultValue) {
        try {
            return objectMapper.readTree(json).path(field).asText(defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Object parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return json;
        }
    }

    private CodeFeedback parseFeedback(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> strengths = new ArrayList<>();
            List<String> improvements = new ArrayList<>();
            List<String> bugs = new ArrayList<>();
            for (JsonNode s : root.path("strengths")) strengths.add(s.asText());
            for (JsonNode i : root.path("improvements")) improvements.add(i.asText());
            for (JsonNode b : root.path("bugs")) bugs.add(b.asText());

            return CodeFeedback.builder()
                    .isCorrect(root.path("isCorrect").asBoolean())
                    .score(root.path("score").asInt())
                    .overallFeedback(root.path("overallFeedback").asText())
                    .strengths(strengths).improvements(improvements).bugs(bugs)
                    .optimizedApproach(root.path("optimizedApproach").asText())
                    .timeComplexity(root.path("timeComplexity").asText())
                    .spaceComplexity(root.path("spaceComplexity").asText())
                    .suggestedTimeComplexity(root.path("suggestedTimeComplexity").asText())
                    .suggestedSpaceComplexity(root.path("suggestedSpaceComplexity").asText())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse feedback: " + e.getMessage());
        }
    }
}