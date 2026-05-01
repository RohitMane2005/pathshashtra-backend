package com.pathshashtra.backend.discussion;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "discussion_posts", indexes = {
    @Index(name = "idx_disc_post_user", columnList = "user_id"),
    @Index(name = "idx_disc_post_created", columnList = "created_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DiscussionPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** Comma-separated tags, e.g. "arrays,dp,help" */
    private String tags;

    private int upvotes = 0;
    private int replyCount = 0;

    /** Author name denormalized for fast display */
    private String authorName;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
