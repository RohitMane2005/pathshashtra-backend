package com.pathshashtra.backend.study;

import com.pathshashtra.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_plans")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class StudyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String planTitle;        // e.g. "JEE Mains 2026 Prep"
    private LocalDate examDate;      // Target exam date
    private int dailyHours;          // Hours student can study per day
    private LocalDate startDate;     // When plan begins

    @Column(columnDefinition = "TEXT")
    private String planJson;         // Full AI-generated plan JSON

    @Enumerated(EnumType.STRING)
    private PlanStatus status = PlanStatus.ACTIVE;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum PlanStatus {
        ACTIVE, COMPLETED, ARCHIVED
    }
}