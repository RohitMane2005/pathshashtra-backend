package com.pathshashtra.backend.discussion;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "discussion_replies", indexes = {
    @Index(name = "idx_disc_reply_post", columnList = "post_id"),
    @Index(name = "idx_disc_reply_user", columnList = "user_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DiscussionReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private int upvotes = 0;

    private String authorName;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
