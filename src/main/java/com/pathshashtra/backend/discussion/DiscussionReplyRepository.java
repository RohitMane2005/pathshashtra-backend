package com.pathshashtra.backend.discussion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface DiscussionReplyRepository extends JpaRepository<DiscussionReply, Long> {
    List<DiscussionReply> findByPostIdOrderByCreatedAtAsc(Long postId);
    void deleteByUserId(Long userId);
    void deleteByPostId(Long postId);

    /** HIGH-03 FIX: Atomic vote updates — prevents lost updates under concurrent voting. */
    @Modifying
    @Query("UPDATE DiscussionReply r SET r.upvotes = GREATEST(0, r.upvotes + 1) WHERE r.id = :id")
    void incrementUpvotes(Long id);

    @Modifying
    @Query("UPDATE DiscussionReply r SET r.upvotes = GREATEST(0, r.upvotes - 1) WHERE r.id = :id")
    void decrementUpvotes(Long id);
}
