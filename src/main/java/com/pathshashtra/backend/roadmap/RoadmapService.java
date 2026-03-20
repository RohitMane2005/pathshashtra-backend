package com.pathshashtra.backend.roadmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoadmapService {

    private final RoadmapRepository roadmapRepository;
    private final GroqRoadmapService groqRoadmapService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoadmapService(RoadmapRepository roadmapRepository,
                          GroqRoadmapService groqRoadmapService,
                          UserRepository userRepository) {
        this.roadmapRepository = roadmapRepository;
        this.groqRoadmapService = groqRoadmapService;
        this.userRepository = userRepository;
    }

    public JsonNode generateRoadmap(RoadmapRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate from AI
        String rawJson = groqRoadmapService.generateRoadmap(request, user.getName());

        // Clean JSON
        String cleaned = rawJson.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        cleaned = cleaned.trim();

        try {
            JsonNode roadmapNode = objectMapper.readTree(cleaned);

            // Save to DB
            Roadmap roadmap = new Roadmap();
            roadmap.setUser(user);
            roadmap.setGoal(request.getGoal());
            roadmap.setCurrentLevel(request.getCurrentLevel());
            roadmap.setTimeframe(request.getTimeframe());
            roadmap.setFocusArea(request.getFocusArea());
            roadmap.setRoadmapJson(cleaned);
            roadmapRepository.save(roadmap);

            return roadmapNode;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse roadmap: " + e.getMessage());
        }
    }

    public List<Roadmap> getUserRoadmaps(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return roadmapRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public JsonNode getRoadmapById(Long id, String email) {
        Roadmap roadmap = roadmapRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Roadmap not found"));
        try {
            return objectMapper.readTree(roadmap.getRoadmapJson());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse roadmap");
        }
    }
}
