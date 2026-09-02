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
 * Single RestTemplate instance — connection pool is reused across all AI services.
 * Configured with connect + read timeouts to prevent thread exhaustion.
 *
 * M-06 FIX: Added retry with exponential backoff (2 retries).
 * M-12 FIX: Uses Spring-managed ObjectMapper instead of creating a duplicate.
 */
@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);
    private static final int MAX_RETRIES = 2;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** M-12 FIX: Inject Spring-managed ObjectMapper instead of creating a duplicate. */
    public GroqClient(ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000); // 15s connect
        factory.setReadTimeout(60_000);    // 60s read (LLM can be slow)
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = objectMapper;
    }

    /**
     * Call Groq API with a prompt. Returns raw text response.
     *
     * FIX-5 (retry): Retries up to 2 times with exponential backoff + jitter.
     * Jitter (±25%) prevents thundering herd when multiple concurrent requests
     * fail simultaneously and all retry at the same interval.
     *
     * ⚠ BLOCKING: Thread.sleep() runs on the calling servlet thread.
     * Max block time per call = 60s (read timeout) × 3 attempts + 1s + 2s backoff
     * = ~185s worst case. Tomcat default pool is 200 threads — size accordingly
     * (e.g., max-threads: 50 for AI endpoints + separate thread pool for others).
     *
     * Context management contract: this method is single-turn only (user prompt → response).
     * Multi-turn chat history MUST be stitched together by the caller (e.g., ChatService)
     * before passing a single combined prompt string. FIX-13: documented here explicitly.
     *
     * @param prompt    user prompt (include conversation history for multi-turn)
     * @param maxTokens response token limit
     */
    public String call(String prompt, int maxTokens) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 0) {
                    // FIX-5: Exponential backoff with ±25% jitter to prevent thundering herd.
                    // Base: 1s (attempt 1), 2s (attempt 2). Jitter range: ×0.75–1.25.
                    long baseMs = 1000L * (1L << (attempt - 1));
                    long jitter  = (long) (baseMs * 0.25 * (Math.random() * 2 - 1)); // ±25%
                    long backoffMs = Math.max(100, baseMs + jitter);
                    log.warn("Groq API retry {}/{}, backing off {}ms (base={}ms, jitter={}ms)",
                            attempt, MAX_RETRIES, backoffMs, baseMs, jitter);
                    Thread.sleep(backoffMs);
                }

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
                    throw new com.pathshashtra.backend.exception.ServiceUnavailableException("AI returned empty response");
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("choices").get(0).path("message").path("content").asText();

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new ServiceUnavailableException("AI request was interrupted");
            } catch (Exception e) {
                lastException = e;
                log.warn("Groq API attempt {}/{} failed: {}", attempt + 1, MAX_RETRIES + 1, e.getMessage());
            }
        }

        log.error("Groq API call failed after {} attempts: {}", MAX_RETRIES + 1,
                lastException != null ? lastException.getMessage() : "unknown");
        throw new ServiceUnavailableException("AI service temporarily unavailable. Please try again.");
    }
}

