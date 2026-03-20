package com.pathshashtra.backend.roadmap;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roadmap")
public class RoadmapController {

    private final RoadmapService roadmapService;

    public RoadmapController(RoadmapService roadmapService) {
        this.roadmapService = roadmapService;
    }

    // Generate a new roadmap
    @PostMapping("/generate")
    public ResponseEntity<JsonNode> generateRoadmap(
            @RequestBody RoadmapRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        JsonNode roadmap = roadmapService.generateRoadmap(request, email);
        return ResponseEntity.ok(roadmap);
    }

    // Get all roadmaps for current user
    @GetMapping("/my")
    public ResponseEntity<List<Roadmap>> getMyRoadmaps(Authentication authentication) {
        String email = authentication.getName();
        List<Roadmap> roadmaps = roadmapService.getUserRoadmaps(email);
        return ResponseEntity.ok(roadmaps);
    }

    // Get a specific roadmap by ID
    @GetMapping("/{id}")
    public ResponseEntity<JsonNode> getRoadmap(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        JsonNode roadmap = roadmapService.getRoadmapById(id, email);
        return ResponseEntity.ok(roadmap);
    }
}
