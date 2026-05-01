package com.pathshashtra.backend.discussion;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "discussion_votes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_disc_vote", columnNames = {"user_id", "target_type", "target_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DiscussionVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** POST or REPLY */
    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;
}
