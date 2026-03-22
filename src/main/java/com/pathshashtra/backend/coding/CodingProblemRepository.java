package com.pathshashtra.backend.coding;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CodingProblemRepository extends JpaRepository<CodingProblem, Long> {
    List<CodingProblem> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<CodingProblem> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<CodingProblem> findByUserIdAndTopic(Long userId, String topic);
    List<CodingProblem> findByUserIdAndDifficulty(Long userId, String difficulty);
    Optional<CodingProblem> findByIdAndUserId(Long id, Long userId);
    void deleteByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM CodingProblem p WHERE p.user.id = :userId AND p.status = 'SOLVED'")
    long countSolvedByUserId(Long userId);
}
