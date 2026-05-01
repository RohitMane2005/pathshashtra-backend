package com.pathshashtra.backend.discussion;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DiscussionReplyRepository extends JpaRepository<DiscussionReply, Long> {
    List<DiscussionReply> findByPostIdOrderByCreatedAtAsc(Long postId);
    void deleteByUserId(Long userId);
    void deleteByPostId(Long postId);
}
