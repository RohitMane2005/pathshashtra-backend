package com.pathshashtra.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    // PERF-04 fix: fetch only last 20 messages to bound context window size
    List<ChatMessage> findTop20BySessionIdOrderByCreatedAtDesc(Long sessionId);
    void deleteBySessionId(Long sessionId);
    void deleteByUserId(Long userId);
}
