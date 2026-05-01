package com.pathshashtra.backend.contest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ContestSubmissionRepository extends JpaRepository<ContestSubmission, Long> {
    List<ContestSubmission> findByContestIdAndUserId(Long contestId, Long userId);

    @Query("SELECT cs FROM ContestSubmission cs WHERE cs.contestId = :contestId ORDER BY cs.score DESC, cs.submittedAt ASC")
    List<ContestSubmission> findLeaderboard(Long contestId);

    void deleteByUserId(Long userId);
}
