package com.pathshashtra.backend.chat;

import com.pathshashtra.backend.ratelimit.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final RateLimiter rateLimiter;

    public ChatController(ChatService chatService, RateLimiter rateLimiter) {
        this.chatService = chatService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSession>> getSessions(Authentication auth) {
        return ResponseEntity.ok(chatService.getSessions(auth.getName()));
    }

    @PostMapping("/sessions")
    public ResponseEntity<ChatSession> createSession(Authentication auth) {
        return ResponseEntity.ok(chatService.createSession(auth.getName()));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(chatService.getMessages(auth.getName(), id));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Map<String, String>> deleteSession(
            @PathVariable Long id, Authentication auth) {
        chatService.deleteSession(auth.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Session deleted"));
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> body, Authentication auth) {
        String email = auth.getName();
        if (!rateLimiter.allowRequest("ai_chat:", email, 50)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Daily chat limit reached (50 messages/day)."));
        }
        Long sessionId = Long.valueOf(body.get("sessionId").toString());
        String content = (String) body.get("content");
        return ResponseEntity.ok(chatService.sendMessage(email, sessionId, content));
    }
}
