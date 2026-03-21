package com.pathshashtra.backend.roadmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathshashtra.backend.common.JsonCleaner;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoadmapService {

    private final RoadmapRepository roadmapRepository;
    private final GroqRoadmapService groqRoadmapService;
    private final UserRepository userRepository;
    private final JsonCleaner jsonCleaner;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoadmapService(RoadmapRepository roadmapRepository,
                          GroqRoadmapService groqRoadmapService,
                          UserRepository userRepository,
                          JsonCleaner jsonCleaner) {
        this.roadmapRepository = roadmapRepository;
        this.groqRoadmapService = groqRoadmapService;
        this.userRepository = userRepository;
        this.jsonCleaner = jsonCleaner;
    }

    public JsonNode generateRoadmap(RoadmapRequest request, String email) {
        User user = getUser(email);
        String rawJson = groqRoadmapService.generateRoadmap(request, user.getName());
        String cleaned = jsonCleaner.clean(rawJson);

        try {
            JsonNode roadmapNode = objectMapper.readTree(cleaned);

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
        User user = getUser(email);
        return roadmapRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    /** Fixed: verify the roadmap belongs to the requesting user before returning it. */
    public JsonNode getRoadmapById(Long id, String email) {
        User user = getUser(email);
        Roadmap roadmap = roadmapRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Roadmap not found"));

        // Ownership check — prevents user A from reading user B's roadmap
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Roadmap not found");
        }

        try {
            return objectMapper.readTree(roadmap.getRoadmapJson());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse roadmap");
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
