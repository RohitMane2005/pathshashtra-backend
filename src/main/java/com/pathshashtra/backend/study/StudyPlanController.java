package com.pathshashtra.backend.study;

import com.pathshashtra.backend.ratelimit.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/study")
public class StudyPlanController {

    private final StudyPlanService studyPlanService;
    private final RateLimiter rateLimiter;

    public StudyPlanController(StudyPlanService studyPlanService, RateLimiter rateLimiter) {
        this.studyPlanService = studyPlanService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/plan/generate")
    public ResponseEntity<?> generatePlan(@RequestBody StudyPlanRequest request, Authentication auth) {
        String email = auth.getName();
        if (!rateLimiter.allowStudyPlanGenerate(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Daily limit reached. You can generate up to 3 study plans per day."));
        }
        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(rateLimiter.remaining("ai_study:", email, 3)))
                .body(studyPlanService.generatePlan(email, request));
    }

    @GetMapping("/plan")
    public ResponseEntity<Map<String, Object>> getActivePlan(Authentication auth) {
        return ResponseEntity.ok(studyPlanService.getActivePlan(auth.getName()));
    }

    @GetMapping("/today")
    public ResponseEntity<List<StudyTopic>> getTodaysTopics(Authentication auth) {
        return ResponseEntity.ok(studyPlanService.getTodaysTopics(auth.getName()));
    }

    @PutMapping("/topic/progress")
    public ResponseEntity<StudyTopic> updateProgress(@RequestBody TopicProgressRequest request, Authentication auth) {
        return ResponseEntity.ok(studyPlanService.updateTopicProgress(auth.getName(), request));
    }

    @GetMapping("/weak-topics")
    public ResponseEntity<List<StudyTopic>> getWeakTopics(Authentication auth) {
        return ResponseEntity.ok(studyPlanService.getWeakTopics(auth.getName()));
    }

    @GetMapping("/progress")
    public ResponseEntity<StudyProgressResponse> getProgress(Authentication auth) {
        return ResponseEntity.ok(studyPlanService.getProgress(auth.getName()));
    }
}
