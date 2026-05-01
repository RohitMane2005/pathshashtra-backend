package com.pathshashtra.backend.report;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_reports", indexes = {
    @Index(name = "idx_report_user_week", columnList = "user_id, week_start")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    private int problemsSolved;
    private int topicsCompleted;
    private int quizzesCompleted;
    private int streakDays;
    private long xpGained;

    /** JSON string with highlights */
    @Column(columnDefinition = "TEXT")
    private String highlights;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
