package com.pathshashtra.backend.quiz;

import com.pathshashtra.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_sessions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
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

    public enum QuizStatus {
        STARTED, COMPLETED
    }
}
