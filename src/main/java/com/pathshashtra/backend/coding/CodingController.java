package com.pathshashtra.backend.coding;

import com.pathshashtra.backend.ratelimit.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/coding")
public class CodingController {

    private final CodingService codingService;
    private final RateLimiter rateLimiter;

    public CodingController(CodingService codingService, RateLimiter rateLimiter) {
        this.codingService = codingService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/problem/generate")
    public ResponseEntity<?> generateProblem(@RequestBody ProblemGenerateRequest request, Authentication auth) {
        String email = auth.getName();
        if (!rateLimiter.allowCodingGenerate(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Daily limit reached. You can generate up to 20 problems per day."));
        }
        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(rateLimiter.remaining("ai_coding_gen:", email, 20)))
                .body(codingService.generateProblem(email, request));
    }

    @PostMapping("/hint")
    public ResponseEntity<?> getHint(@RequestBody HintRequest request, Authentication auth) {
        String email = auth.getName();
        if (!rateLimiter.allowCodingHint(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Daily hint limit reached (30/day)."));
        }
        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(rateLimiter.remaining("ai_coding_hint:", email, 30)))
                .body(codingService.getHint(email, request));
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitCode(@RequestBody CodeSubmitRequest request, Authentication auth) {
        String email = auth.getName();
        if (!rateLimiter.allowCodingSubmit(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Daily submit limit reached (30/day)."));
        }
        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(rateLimiter.remaining("ai_coding_submit:", email, 30)))
                .body(codingService.submitCode(email, request));
    }

    @GetMapping("/problems")
    public ResponseEntity<List<Map<String, Object>>> getMyProblems(Authentication auth) {
        return ResponseEntity.ok(codingService.getMyProblems(auth.getName()));
    }

    @GetMapping("/roadmap")
    public ResponseEntity<?> getRoadmap(
            @RequestParam(defaultValue = "Campus Placement") String goal, Authentication auth) {
        String email = auth.getName();
        if (!rateLimiter.allowCodingGenerate(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Daily limit reached."));
        }
        return ResponseEntity.ok(codingService.getDsaRoadmap(email, goal));
    }
}
