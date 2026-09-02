package com.pathshashtra.backend.coding;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CodingProblemRepository extends JpaRepository<CodingProblem, Long> {
    List<CodingProblem> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<CodingProblem> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<CodingProblem> findByIdAndUserId(Long id, Long userId);
    void deleteByUserId(Long userId);

    @Query("SELECT COUNT(p) FROM CodingProblem p WHERE p.user.id = :userId AND p.status = 'SOLVED'")
    long countSolvedByUserId(Long userId);

    /** FIX: grouped aggregate for leaderboard — avoids N+1 */
    @Query("SELECT p.user.id, COUNT(p) FROM CodingProblem p WHERE p.status = 'SOLVED' GROUP BY p.user.id")
    List<Object[]> countSolvedGroupedByUser();

    /** Weekly report: count problems by status within a date range */
    @Query("SELECT COUNT(p) FROM CodingProblem p WHERE p.user.id = :userId AND p.status = :status " +
           "AND p.createdAt BETWEEN :start AND :end")
    long countByUserIdAndStatusAndCreatedAtBetween(Long userId, String status,
                                                    java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * PERF M3: Lightweight projection query for the problems list view.
     * FIX-9: Now returns Page<CodingProblemSummary> (type-safe projection interface)
     * instead of Page<Object[]> (unsafe index access). Spring Data JPA maps each
     * getter to the corresponding SELECT column. Query shape changes fail at startup.
     */
    @Query("SELECT p.id, p.problemTitle, p.topic, p.difficulty, p.language, p.status, p.hintsUsed, p.createdAt " +
           "FROM CodingProblem p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    Page<CodingProblemSummary> findProblemSummariesByUserId(Long userId, Pageable pageable);
}

