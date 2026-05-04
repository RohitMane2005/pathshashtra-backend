package com.pathshashtra.backend.chat;

import com.pathshashtra.backend.exception.ServiceUnavailableException;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;
    /**
     * FIX M1: Use a RestTemplate with proper timeouts instead of the default
     * infinite-timeout RestTemplate that could block threads forever.
     */
    private final RestTemplate restTemplate;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            You are PathShashtra AI Assistant — a helpful, friendly tutor for Indian students.
            You specialize in: DSA, competitive programming, career guidance, study planning, and placement prep.
            Keep answers concise, practical, and encouraging. Use examples when explaining concepts.
            If asked about non-academic topics, politely redirect to study/career topics.
            Format code blocks with proper language tags. Use bullet points for lists.
            """;

    public ChatService(ChatSessionRepository sessionRepo,
                       ChatMessageRepository messageRepo,
                       UserRepository userRepo,
                       ObjectMapper objectMapper) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.objectMapper = objectMapper;
        // FIX M1: Configure timeouts to prevent thread exhaustion
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public List<ChatSession> getSessions(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return sessionRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public ChatSession createSession(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ChatSession session = new ChatSession();
        session.setUserId(user.getId());
        session.setTitle("New Chat");
        session.setCreatedAt(LocalDateTime.now());
        return sessionRepo.save(session);
    }

    public List<ChatMessage> getMessages(String email, Long sessionId) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!session.getUserId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public ChatMessage sendMessage(String email, Long sessionId, String content) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!session.getUserId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Save user message
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setUserId(user.getId());
        userMsg.setRole("USER");
        userMsg.setContent(content);
        userMsg.setCreatedAt(LocalDateTime.now());
        messageRepo.save(userMsg);

        // Update session title from first message
        if ("New Chat".equals(session.getTitle())) {
            session.setTitle(content.length() > 50 ? content.substring(0, 50) + "..." : content);
            sessionRepo.save(session);
        }

        // Build conversation history
        List<ChatMessage> history = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
        String aiResponse = callGroqApi(history);

        // Save AI response
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setUserId(user.getId());
        aiMsg.setRole("ASSISTANT");
        aiMsg.setContent(aiResponse);
        aiMsg.setCreatedAt(LocalDateTime.now());
        return messageRepo.save(aiMsg);
    }

    @Transactional
    public void deleteSession(String email, Long sessionId) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        if (!session.getUserId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        messageRepo.deleteBySessionId(sessionId);
        sessionRepo.delete(session);
    }

    private String callGroqApi(List<ChatMessage> history) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

            // Use last 20 messages for context window
            List<ChatMessage> recent = history.size() > 20
                    ? history.subList(history.size() - 20, history.size())
                    : history;

            for (ChatMessage msg : recent) {
                messages.add(Map.of(
                        "role", "USER".equals(msg.getRole()) ? "user" : "assistant",
                        "content", msg.getContent()
                ));
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("max_tokens", 1500);
            body.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    groqApiUrl, HttpMethod.POST, entity, String.class);

            if (response.getBody() == null)
                throw new RuntimeException("AI returned empty response");
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("Groq API call failed for chat: {}", e.getMessage());
            throw new ServiceUnavailableException(
                    "AI service temporarily unavailable. Please try again.");
        }
    }
}

