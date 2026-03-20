package com.pathshashtra.backend.roadmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GroqRoadmapService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateRoadmap(RoadmapRequest request, String studentName) {
        String prompt = """
            You are an expert career and learning roadmap generator for Indian college students.
            
            Student: %s
            Goal: %s
            Current Level: %s
            Timeframe: %s
            Focus Area: %s
            Current Skills: %s
            
            Generate a detailed, actionable learning roadmap. Respond ONLY with valid JSON, no extra text, no markdown:
            {
              "roadmapTitle": "Roadmap title",
              "goal": "%s",
              "timeframe": "%s",
              "overview": "2-3 sentence overview of this roadmap",
              "totalPhases": 4,
              "phases": [
                {
                  "phase": 1,
                  "title": "Phase title",
                  "duration": "Month 1-2",
                  "objective": "What they will achieve",
                  "topics": ["topic1", "topic2", "topic3", "topic4", "topic5"],
                  "projects": ["Build X project", "Create Y app"],
                  "resources": [
                    {"type": "video", "name": "Resource name", "url": "url or platform"},
                    {"type": "book", "name": "Book name", "author": "Author"},
                    {"type": "practice", "name": "Platform", "url": "url"}
                  ],
                  "milestone": "Milestone checkpoint",
                  "skills": ["skill1", "skill2"],
                  "estimatedHours": 60,
                  "difficulty": "Beginner"
                }
              ],
              "keySkills": ["skill1", "skill2", "skill3", "skill4", "skill5", "skill6"],
              "careerOutcome": "What jobs/roles this roadmap leads to",
              "salaryRange": "Expected salary range after completion",
              "tips": ["tip1", "tip2", "tip3"],
              "dailySchedule": {
                "weekday": "2-3 hours on theory and practice",
                "weekend": "4-5 hours on projects and revision"
              }
            }
            
            Make it realistic for Indian college students. Include free resources like YouTube channels, free courses.
            Generate exactly 4 phases covering the full %s timeframe.
            """.formatted(
                studentName, request.getGoal(), request.getCurrentLevel(),
                request.getTimeframe(), request.getFocusArea(),
                request.getCurrentSkills() != null ? request.getCurrentSkills() : "None specified",
                request.getGoal(), request.getTimeframe(), request.getTimeframe()
            );

        return callGroq(prompt, 4000);
    }

    private String callGroq(String prompt, int maxTokens) {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(30000);
            factory.setReadTimeout(60000);
            RestTemplate restTemplate = new RestTemplate(factory);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("messages", List.of(message));
            body.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            throw new RuntimeException("AI roadmap generation failed: " + e.getMessage());
        }
    }
}
