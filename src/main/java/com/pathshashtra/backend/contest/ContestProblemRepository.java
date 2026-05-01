package com.pathshashtra.backend.contest;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContestProblemRepository extends JpaRepository<ContestProblem, Long> {
    List<ContestProblem> findByContestIdOrderByOrderIndexAsc(Long contestId);
}
