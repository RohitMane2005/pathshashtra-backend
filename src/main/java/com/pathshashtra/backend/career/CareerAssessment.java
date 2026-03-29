package com.pathshashtra.backend.career;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pathshashtra.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "career_assessments", indexes = {
    @Index(name = "idx_career_user_id", columnList = "user_id"),
    @Index(name = "idx_career_user_status", columnList = "user_id, status")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CareerAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "hibernateLazyInitializer", "handler"})
    private User user;

    @Column(columnDefinition = "TEXT")
    private String answersJson;

    @Column(columnDefinition = "TEXT")
    private String resultJson;

    private String topCareer;
    private Integer topMatchScore;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    private AssessmentStatus status = AssessmentStatus.PENDING;

    public enum AssessmentStatus { PENDING, COMPLETED }
}
