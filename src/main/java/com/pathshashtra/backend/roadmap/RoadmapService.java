package com.pathshashtra.backend.roadmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathshashtra.backend.common.JsonCleaner;
import com.pathshashtra.backend.exception.ForbiddenException;
import com.pathshashtra.backend.exception.NotFoundException;
import com.pathshashtra.backend.exception.ServiceUnavailableException;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoadmapService {

    private final RoadmapRepository roadmapRepository;
    private final GroqRoadmapService groqRoadmapService;
    private final UserRepository userRepository;
    private final JsonCleaner jsonCleaner;
    private final ObjectMapper objectMapper;

    public RoadmapService(RoadmapRepository roadmapRepository,
                          GroqRoadmapService groqRoadmapService,
                          UserRepository userRepository,
                          JsonCleaner jsonCleaner,
                          ObjectMapper objectMapper) {
        this.roadmapRepository = roadmapRepository;
        this.groqRoadmapService = groqRoadmapService;
        this.userRepository = userRepository;
        this.jsonCleaner = jsonCleaner;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public JsonNode generateRoadmap(RoadmapRequest request, String email) {
        User user = getUser(email);
        String rawJson = groqRoadmapService.generateRoadmap(request, user.getName());
        String cleaned = jsonCleaner.clean(rawJson);
        if (cleaned == null) throw new ServiceUnavailableException("AI returned empty roadmap response");

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
            throw new ServiceUnavailableException("Failed to parse roadmap: " + e.getMessage());
        }
    }

    /**
     * FIX: Returns plain Map instead of Page<Roadmap>.
     * PageImpl is not Jackson-serializable without spring-data-web, causing HTTP 500.
     * Roadmap entity also exposes roadmapJson (large TEXT column) — only return summary fields.
     */
    public Map<String, Object> getUserRoadmaps(String email, Pageable pageable) {
        User user = getUser(email);
        Page<Roadmap> page = roadmapRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Roadmap r : page.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("goal", r.getGoal());
            item.put("currentLevel", r.getCurrentLevel());
            item.put("timeframe", r.getTimeframe());
            item.put("focusArea", r.getFocusArea());
            item.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
            items.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", items);
        response.put("totalElements", page.getTotalElements());
        return response;
    }

    /** Fixed: verify the roadmap belongs to the requesting user before returning it. */
    public JsonNode getRoadmapById(Long id, String email) {
        User user = getUser(email);
        Roadmap roadmap = roadmapRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Roadmap not found"));

        // Ownership check — prevents user A from reading user B's roadmap
        if (!roadmap.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Roadmap not found");
        }

        try {
            return objectMapper.readTree(roadmap.getRoadmapJson());
        } catch (Exception e) {
            throw new ServiceUnavailableException("Failed to parse roadmap");
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
