package com.pathshashtra.backend.contest;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contest_submissions", indexes = {
    @Index(name = "idx_csub_contest_user", columnList = "contest_id, user_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ContestSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contest_id", nullable = false)
    private Long contestId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(columnDefinition = "TEXT")
    private String code;

    private String language;
    private int score = 0;

    private String userName;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();
}
