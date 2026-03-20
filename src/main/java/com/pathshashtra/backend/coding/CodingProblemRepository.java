package com.pathshashtra.backend.coding;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CodingProblemRepository extends JpaRepository<CodingProblem, Long> {
    List<CodingProblem> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CodingProblem> findByUserIdAndTopic(Long userId, String topic);
    List<CodingProblem> findByUserIdAndDifficulty(Long userId, String difficulty);
    Optional<CodingProblem> findByIdAndUserId(Long id, Long userId);
}
