package com.pathshashtra.backend.roadmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.pathshashtra.backend.ratelimit.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/roadmap")
public class RoadmapController {

    private final RoadmapService roadmapService;
    private final RateLimiter rateLimiter;

    public RoadmapController(RoadmapService roadmapService, RateLimiter rateLimiter) {
        this.roadmapService = roadmapService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateRoadmap(@Valid @RequestBody RoadmapRequest request, Authentication auth) {
        String email = auth.getName();
        if (!rateLimiter.allowRoadmapGenerate(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Daily limit reached. You can generate up to 5 roadmaps per day."));
        }
        JsonNode roadmap = roadmapService.generateRoadmap(request, email);
        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(rateLimiter.remaining("ai_roadmap:", email, 5)))
                .body(roadmap);
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyRoadmaps(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        // MED-09 FIX: Clamp to valid ranges — negative values cause Spring Data exceptions
        page = Math.max(0, page);
        size = Math.max(1, Math.min(size, 20));
        return ResponseEntity.ok(roadmapService.getUserRoadmaps(auth.getName(), PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JsonNode> getRoadmap(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(roadmapService.getRoadmapById(id, auth.getName()));
    }
}
