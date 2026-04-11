package com.pathshashtra.backend.quiz;

import com.pathshashtra.backend.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_sessions", indexes = {
        @Index(name = "idx_quiz_user_id", columnList = "user_id"),
        @Index(name = "idx_quiz_share_token", columnList = "share_token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({ "password", "hibernateLazyInitializer", "handler" })
    private User user;

    // JSON string of generated questions from Claude
    @Column(columnDefinition = "TEXT")
    private String questionsJson;

    // JSON string of user's answers
    @Column(columnDefinition = "TEXT")
    private String answersJson;

    // Full AI result JSON: careers + skill gaps + roadmap + salary
    @Column(columnDefinition = "TEXT")
    private String resultJson;

    @Enumerated(EnumType.STRING)
    private QuizStatus status = QuizStatus.STARTED;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    /** URL-safe share token generated when quiz completes. Null until completed. */
    @Column(unique = true)
    private String shareToken;

    public enum QuizStatus {
        STARTED, COMPLETED
    }
}
