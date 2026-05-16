package com.pathshashtra.backend.chat;

import com.pathshashtra.backend.common.GroqClient;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BE-04 FIX: ChatService now uses the shared GroqClient instead of
 * duplicating its own RestTemplate. All timeout config, error handling,
 * and API key injection are centralised in GroqClient.
 */
@Service
public class ChatService {

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final GroqClient groqClient;

    private static final String SYSTEM_PROMPT =
        "You are PathShashtra AI Assistant — a helpful, friendly tutor for Indian students. " +
        "You specialize in: DSA, competitive programming, career guidance, study planning, and placement prep. " +
        "Keep answers concise, practical, and encouraging. Use examples when explaining concepts. " +
        "If asked about non-academic topics, politely redirect to study/career topics. " +
        "Format code blocks with proper language tags. Use bullet points for lists.";

    public ChatService(ChatSessionRepository sessionRepo,
                       ChatMessageRepository messageRepo,
                       UserRepository userRepo,
                       GroqClient groqClient) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.groqClient = groqClient;
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

        // PERF-04 fix: fetch only last 20 messages directly from DB, reverse for chronological order
        List<ChatMessage> history = messageRepo.findTop20BySessionIdOrderByCreatedAtDesc(sessionId);
        java.util.Collections.reverse(history);

        String aiResponse = callAI(history);

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

    /**
     * Build the full conversation prompt including system prompt and history,
     * then delegate to the shared GroqClient.
     */
    private String callAI(List<ChatMessage> history) {
        // Build a text prompt from the conversation history
        StringBuilder prompt = new StringBuilder();
        prompt.append(SYSTEM_PROMPT).append("\n\n--- Conversation so far ---\n");
        for (ChatMessage msg : history) {
            String role = "USER".equals(msg.getRole()) ? "Student" : "Assistant";
            prompt.append(role).append(": ").append(msg.getContent()).append("\n");
        }
        prompt.append("\nAssistant:");

        return groqClient.call(prompt.toString(), 1500);
    }
}
