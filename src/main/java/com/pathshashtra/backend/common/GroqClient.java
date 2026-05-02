package com.pathshashtra.backend.common;

import com.pathshashtra.backend.exception.ServiceUnavailableException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared Groq/LLM HTTP client.
 * Single RestTemplate instance — connection pool is reused across all AI
 * services.
 * Configured with connect + read timeouts to prevent thread exhaustion.
 */
@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroqClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000); // 15s connect
        factory.setReadTimeout(60_000); // 60s read (LLM can be slow)
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Call Groq API with a prompt. Returns raw text response.
     * 
     * @param prompt    user prompt
     * @param maxTokens response token limit
     */
    public String call(String prompt, int maxTokens) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("temperature", 0.7);
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            if (response.getBody() == null)
                throw new RuntimeException("AI returned empty response");
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
            throw new ServiceUnavailableException("AI service temporarily unavailable. Please try again.");
        }
    }
}
