package com.pathshashtra.backend.quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<QuizSession, Long> {
    List<QuizSession> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<QuizSession> findByIdAndUserId(Long id, Long userId);
    void deleteByUserId(Long userId);
    Optional<QuizSession> findByShareToken(String shareToken);

    @Query("SELECT COUNT(q) FROM QuizSession q WHERE q.user.id = :userId AND q.status = 'COMPLETED'")
    long countCompletedByUserId(Long userId);

    @Query("SELECT COUNT(q) FROM QuizSession q WHERE q.user.id = :userId")
    long countByUserId(Long userId);

    /** FIX: grouped aggregate for leaderboard — avoids N+1 */
    @Query("SELECT q.user.id, COUNT(q) FROM QuizSession q WHERE q.status = 'COMPLETED' GROUP BY q.user.id")
    List<Object[]> countCompletedGroupedByUser();

    /** Weekly report: count quizzes within a date range */
    @Query("SELECT COUNT(q) FROM QuizSession q WHERE q.user.id = :userId AND q.createdAt BETWEEN :start AND :end")
    long countByUserIdAndCreatedAtBetween(Long userId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
