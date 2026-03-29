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
}
