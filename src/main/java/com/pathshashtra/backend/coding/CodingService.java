package com.pathshashtra.backend.coding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathshashtra.backend.common.JsonCleaner;
import com.pathshashtra.backend.exception.BadRequestException;
import com.pathshashtra.backend.exception.NotFoundException;
import com.pathshashtra.backend.profile.UserProfileRepository;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CodingService {

    private static final Logger log = LoggerFactory.getLogger(CodingService.class);


    private final CodingProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final CodingAIService codingAIService;
    private final JsonCleaner jsonCleaner;
    private final ObjectMapper objectMapper;

    public CodingService(CodingProblemRepository problemRepository,
                         UserRepository userRepository,
                         UserProfileRepository profileRepository,
                         CodingAIService codingAIService,
                         JsonCleaner jsonCleaner,
                         ObjectMapper objectMapper) {
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.codingAIService = codingAIService;
        this.jsonCleaner = jsonCleaner;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> generateProblem(String email, ProblemGenerateRequest request) {
        User user = getUser(email);
        String problemJson = codingAIService.generateProblem(
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
                .orElseThrow(() -> new NotFoundException("Problem not found"));

        if (problem.getHintsUsed() >= 3) {
            throw new BadRequestException("Maximum 3 hints allowed per problem");
        }

        String hintJson = codingAIService.generateHint(
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
                .orElseThrow(() -> new NotFoundException("Problem not found"));

        String feedbackJson = codingAIService.reviewCode(
                problem.getProblemJson(), request.getCode(), request.getLanguage());

        problem.setSubmittedCode(request.getCode());
        // FIX: Store feedbackJson AFTER cleaning so it is always valid JSON in the DB.
        // Previously stored raw LLM output (may contain ```json fences) which caused
        // JSON.parse to fail when loading old problems from the problems list.
        String cleanedFeedback = jsonCleaner.clean(feedbackJson);
        problem.setFeedbackJson(cleanedFeedback);
        problem.setStatus(CodingProblem.ProblemStatus.REVIEWED);

        CodeFeedback feedback = parseFeedback(cleanedFeedback);
        if (feedback.isCorrect()) {
            problem.setStatus(CodingProblem.ProblemStatus.SOLVED);
            problem.setSolvedAt(LocalDateTime.now());
        }
        problemRepository.save(problem);
        return feedback;
    }

    /**
     * FIX: Returns plain List instead of PageImpl.
     * PageImpl is not Jackson-serializable without spring-data-web dependency,
     * causing HTTP 500 on GET /coding/problems. Plain List serializes cleanly.
     * The controller wraps it in a Map with "content" and "totalElements" keys
     * to preserve the same response shape the frontend expects.
     *
     * PERF M3: Uses projection query to avoid loading heavy TEXT columns
     * (problemJson, submittedCode, feedbackJson — potentially 10-50 KB each).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMyProblems(String email, Pageable pageable) {
        User user = getUser(email);
        Page<Object[]> page = problemRepository
                .findProblemSummariesByUserId(user.getId(), pageable);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : page.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[0]);
            item.put("title", row[1]);
            item.put("topic", row[2]);
            item.put("difficulty", row[3]);
            item.put("language", row[4]);
            item.put("status", row[5] != null ? row[5].toString() : null); // enum → String
            item.put("hintsUsed", row[6]);
            item.put("createdAt", row[7] != null ? row[7].toString() : null);
            result.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", result);
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("number", page.getNumber());
        response.put("size", page.getSize());
        return response;
    }



    /** Load a single saved problem by ID — used to resume from the problems list. */
    @Transactional(readOnly = true)
    public Map<String, Object> getProblemById(String email, Long problemId) {
        User user = getUser(email);
        log.debug("Loading problem {} for user {} (id={})", problemId, email, user.getId());
        CodingProblem problem = problemRepository
                .findByIdAndUserId(problemId, user.getId())
                .orElseThrow(() -> new NotFoundException("Problem not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("problemId", problem.getId());
        response.put("problem", parseJson(problem.getProblemJson()));
        response.put("submittedCode", problem.getSubmittedCode());
        // parseJson() already runs JsonCleaner internally for backward compatibility
        // with old records stored before the cleaning fix — no need to double-clean.
        response.put("feedbackJson", parseJson(problem.getFeedbackJson()));
        response.put("hintsUsed", problem.getHintsUsed());
        response.put("status", problem.getStatus().name());
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
                .orElseThrow(() -> new NotFoundException("Problem not found"));

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

    @Transactional(readOnly = true)
    public Map<String, Object> getDsaRoadmap(String email, String targetGoal) {
        User user = getUser(email);
        final String[] level = {"Beginner"};
        profileRepository.findByUserId(user.getId()).ifPresent(p -> {
            if (p.getExperienceLevel() != null) level[0] = p.getExperienceLevel();
        });

        String roadmapJson = codingAIService.generateRoadmap(user.getName(), level[0], targetGoal);
        Map<String, Object> response = new HashMap<>();
        response.put("roadmap", parseJson(jsonCleaner.clean(roadmapJson)));
        return response;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private String extractField(String json, String field, String defaultValue) {
        try {
            return objectMapper.readTree(json).path(field).asText(defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * FIX: Null-safe JSON parser that also runs JsonCleaner for backward compatibility.
     * Old records stored before the cleaning fix may have ```json fences in the DB.
     * objectMapper.readTree(null) throws NPE — now guarded and cleaned first.
     */
    private Object parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            // Run through JsonCleaner to handle old records with LLM markdown fences
            String cleaned = jsonCleaner.clean(json);
            if (cleaned == null || cleaned.isBlank()) return null;
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.warn("Could not parse JSON field: {}", e.getMessage());
            return null;
        }
    }

    private CodeFeedback parseFeedback(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<String> strengths = new ArrayList<>();
            List<String> improvements = new ArrayList<>();
            List<String> bugs = new ArrayList<>();
            for (JsonNode s : root.path("strengths")) strengths.add(s.asText());
            for (JsonNode imp : root.path("improvements")) improvements.add(imp.asText());
            for (JsonNode bug : root.path("bugs")) bugs.add(bug.asText());

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
            throw new com.pathshashtra.backend.exception.ServiceUnavailableException("Failed to parse feedback: " + e.getMessage());
        }
    }
}