package com.pathshashtra.backend.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<QuizSession, Long> {
    List<QuizSession> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<QuizSession> findByIdAndUserId(Long id, Long userId);
    void deleteByUserId(Long userId);
    Optional<QuizSession> findByShareToken(String shareToken);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(q) FROM QuizSession q WHERE q.user.id = :userId AND q.status = 'COMPLETED'")
    long countCompletedByUserId(Long userId);
}
