package com.pathshashtra.backend.discussion;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DiscussionVoteRepository extends JpaRepository<DiscussionVote, Long> {
    Optional<DiscussionVote> findByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, Long targetId);
    void deleteByUserId(Long userId);
}
