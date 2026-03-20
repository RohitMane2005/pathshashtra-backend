package com.pathshashtra.backend.study;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.*;

@Service
public class GrokStudyPlanService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url:https://api.x.ai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.model:grok-3-mini}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateStudyPlan(StudyPlanRequest request, String studentName) {
        String prompt = """
            You are an expert academic planner for Indian students.
            
            Student: %s
            Exam: %s
            Exam Type: %s
            Current Level: %s
            Subjects: %s
            Days Until Exam: %d
            Daily Study Hours: %d hours/day
            
            Create a week-by-week study plan. Keep it concise.
            
            STRICT RULES:
            - Maximum 4 weeks total (not 12)
            - Maximum 2 topics per subject per week
            - Maximum 3 days per week per subject
            - Response must be complete valid JSON, never cut off
            
            Respond ONLY with valid JSON in this exact format:
            {
              "planSummary": "Brief overview",
              "totalWeeks": 4,
              "subjects": [
                {
                  "name": "Subject Name",
                  "totalTopics": 8,
                  "priority": "High",
                  "weeklyTopics": [
                    {
                      "week": 1,
                      "topics": [
                        {
                          "day": 1,
                          "topicName": "Topic name",
                          "estimatedHours": 2,
                          "description": "Brief description"
                        }
                      ]
                    }
                  ]
                }
              ],
              "studyTips": ["Tip 1", "Tip 2"],
              "weeklyGoals": ["Week 1 goal", "Week 2 goal"]
            }
            
            Do not include markdown, only pure JSON.
            """.formatted(
                studentName,
                request.getPlanTitle(),
                request.getExamType(),
                request.getCurrentLevel(),
                String.join(", ", request.getSubjects()),
                java.time.temporal.ChronoUnit.DAYS.between(
                        java.time.LocalDate.now(), request.getExamDate()),
                request.getDailyHours()
        );

        return callGrok(prompt);
    }

    private String callGrok(String prompt) {
        try {
            // Add timeout
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(30000);  // 30 seconds
            factory.setReadTimeout(60000);     // 60 seconds
            RestTemplate restTemplateWithTimeout = new RestTemplate(factory);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> message = Map.of(
                    "role", "user",
                    "content", prompt
            );

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", 8000);
            body.put("messages", List.of(message));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplateWithTimeout.exchange(
                    apiUrl, HttpMethod.POST, request, String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0)
                    .path("message").path("content").asText();

        } catch (Exception e) {
            throw new RuntimeException("Grok API call failed: " + e.getMessage());
        }
    }
}