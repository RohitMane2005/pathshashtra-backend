package com.pathshashtra.backend.career;

import com.pathshashtra.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "career_assessments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CareerAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Psychometric answers stored as JSON string */
    @Column(columnDefinition = "TEXT")
    private String answersJson;

    /** Full AI analysis result stored as JSON string */
    @Column(columnDefinition = "TEXT")
    private String resultJson;

    /** Top career title from result — for quick listing */
    private String topCareer;

    /** Overall match score of top career */
    private Integer topMatchScore;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    private AssessmentStatus status = AssessmentStatus.PENDING;

    public enum AssessmentStatus { PENDING, COMPLETED }
}
