package com.pathshashtra.backend.coding;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coding")
public class CodingController {

    private final CodingService codingService;

    public CodingController(CodingService codingService) {
        this.codingService = codingService;
    }

    @PostMapping("/problem/generate")
    public ResponseEntity<Map<String, Object>> generateProblem(
            @RequestBody ProblemGenerateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(codingService.generateProblem(auth.getName(), request));
    }

    @PostMapping("/hint")
    public ResponseEntity<Map<String, Object>> getHint(
            @RequestBody HintRequest request,
            Authentication auth) {
        return ResponseEntity.ok(codingService.getHint(auth.getName(), request));
    }

    @PostMapping("/submit")
    public ResponseEntity<CodeFeedback> submitCode(
            @RequestBody CodeSubmitRequest request,
            Authentication auth) {
        return ResponseEntity.ok(codingService.submitCode(auth.getName(), request));
    }

    @GetMapping("/problems")
    public ResponseEntity<List<Map<String, Object>>> getMyProblems(Authentication auth) {
        return ResponseEntity.ok(codingService.getMyProblems(auth.getName()));
    }

    @GetMapping("/roadmap")
    public ResponseEntity<Map<String, Object>> getRoadmap(
            @RequestParam(defaultValue = "Campus Placement") String goal,
            Authentication auth) {
        return ResponseEntity.ok(codingService.getDsaRoadmap(auth.getName(), goal));
    }
}
