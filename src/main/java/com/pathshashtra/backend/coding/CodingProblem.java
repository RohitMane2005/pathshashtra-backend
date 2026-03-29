package com.pathshashtra.backend.coding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pathshashtra.backend.user.User;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coding_problems", indexes = {
    @Index(name = "idx_coding_user_id", columnList = "user_id"),
    @Index(name = "idx_coding_user_status", columnList = "user_id, status")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CodingProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "hibernateLazyInitializer", "handler"})
    private User user;

    private String topic;
    private String difficulty;
    private String language;
    private String problemTitle;

    @Column(columnDefinition = "TEXT")
    private String problemJson;

    @Column(columnDefinition = "TEXT")
    private String submittedCode;

    @Column(columnDefinition = "TEXT")
    private String feedbackJson;

    @Enumerated(EnumType.STRING)
    private ProblemStatus status = ProblemStatus.GENERATED;

    private int hintsUsed = 0;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime solvedAt;

    public enum ProblemStatus {
        GENERATED, ATTEMPTED, SOLVED, REVIEWED
    }
}
