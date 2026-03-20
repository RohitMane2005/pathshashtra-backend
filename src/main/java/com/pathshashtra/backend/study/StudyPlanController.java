package com.pathshashtra.backend.study;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/study")
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    public StudyPlanController(StudyPlanService studyPlanService) {
        this.studyPlanService = studyPlanService;
    }

    // POST /api/study/plan/generate → AI generates full study plan
    @PostMapping("/plan/generate")
    public ResponseEntity<StudyPlan> generatePlan(
            @RequestBody StudyPlanRequest request,
            Authentication auth) {
        return ResponseEntity.ok(studyPlanService.generatePlan(auth.getName(), request));
    }

    // GET /api/study/plan → get active plan with full details
    @GetMapping("/plan")
    public ResponseEntity<Map<String, Object>> getActivePlan(Authentication auth) {
        return ResponseEntity.ok(studyPlanService.getActivePlan(auth.getName()));
    }

    // GET /api/study/today → get today's topics
    @GetMapping("/today")
    public ResponseEntity<List<StudyTopic>> getTodaysTopics(Authentication auth) {
        return ResponseEntity.ok(studyPlanService.getTodaysTopics(auth.getName()));
    }

    // PUT /api/study/topic/progress → mark topic done/struggling + confidence
    @PutMapping("/topic/progress")
    public ResponseEntity<StudyTopic> updateProgress(
            @RequestBody TopicProgressRequest request,
            Authentication auth) {
        return ResponseEntity.ok(
                studyPlanService.updateTopicProgress(auth.getName(), request));
    }

    // GET /api/study/weak-topics → get all auto-flagged weak topics
    @GetMapping("/weak-topics")
    public ResponseEntity<List<StudyTopic>> getWeakTopics(Authentication auth) {
        return ResponseEntity.ok(studyPlanService.getWeakTopics(auth.getName()));
    }

    // GET /api/study/progress → get % completed per subject + overall
    @GetMapping("/progress")
    public ResponseEntity<StudyProgressResponse> getProgress(Authentication auth) {
        return ResponseEntity.ok(studyPlanService.getProgress(auth.getName()));
    }
}