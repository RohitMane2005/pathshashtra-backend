package com.pathshashtra.backend.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class ClaudeApiService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateQuizQuestions(String userProfile) {
        String prompt = """
            You are a career guidance expert for Indian college students.
            
            Based on this student profile:
            %s
            
            Generate exactly 10 psychometric career assessment questions.
            Each question should reveal personality traits, work preferences,
            strengths, and interests relevant to Indian tech/business careers.
            
            Respond ONLY with valid JSON in this exact format, no extra text:
            {
              "questions": [
                {
                  "number": 1,
                  "question": "Question text here?",
                  "options": ["A) Option one", "B) Option two", "C) Option three", "D) Option four"]
                }
              ]
            }
            """.formatted(userProfile);

        return callGroq(prompt);
    }

    public String analyzeQuizAnswers(String userProfile, String questionsJson, String answersJson) {
        String prompt = """
            You are an expert AI career counselor for Indian college students.
            
            Student Profile:
            %s
            
            Quiz Questions:
            %s
            
            Student's Answers:
            %s
            
            Analyze the answers and provide a comprehensive career guidance report.
            
            Respond ONLY with valid JSON in this exact format, no extra text:
            {
              "summary": "2-3 sentence personalized summary of the student",
              "careerMatches": [
                {
                  "title": "Career title",
                  "matchPercent": 87,
                  "reason": "Why this career suits them",
                  "requiredSkills": ["Skill 1", "Skill 2", "Skill 3"]
                }
              ],
              "skillGaps": ["Gap 1", "Gap 2", "Gap 3", "Gap 4", "Gap 5"],
              "roadmap": [
                {
                  "phase": 1,
                  "title": "Phase title",
                  "duration": "3 months",
                  "actions": ["Action 1", "Action 2", "Action 3"],
                  "resources": ["Resource 1", "Resource 2"]
                }
              ],
              "salaryInfo": {
                "role": "Top matched career title",
                "entryLevel": "₹4-6 LPA",
                "midLevel": "₹10-18 LPA",
                "seniorLevel": "₹25-50 LPA",
                "growthOutlook": "High demand description"
              }
            }
            
            Rules:
            - Return exactly 3 career matches ranked by match percentage
            - Return exactly 4 roadmap phases
            - All salary figures in Indian LPA format
            - Be specific and actionable for Indian students
            - Do not include markdown, only pure JSON
            """.formatted(userProfile, questionsJson, answersJson);

        return callGroq(prompt);
    }

    private String callGroq(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> message = Map.of("role", "user", "content", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", 2000);
            body.put("messages", List.of(message));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, request, String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0)
                    .path("message").path("content").asText();

        } catch (Exception e) {
            throw new RuntimeException("Groq API call failed: " + e.getMessage());
        }
    }
}