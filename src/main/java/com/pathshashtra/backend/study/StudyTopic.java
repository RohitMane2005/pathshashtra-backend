package com.pathshashtra.backend.study;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Entity
@Table(name = "study_topics", indexes = {
    @Index(name = "idx_study_topic_plan_id", columnList = "study_plan_id"),
    @Index(name = "idx_study_topic_plan_week_day", columnList = "study_plan_id, week_number, day_number")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class StudyTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "study_plan_id", nullable = false)
    @JsonIgnoreProperties({"planJson", "user"})
    private StudyPlan studyPlan;

    private String subject;          // e.g. "Mathematics"
    private String topicName;        // e.g. "Calculus - Derivatives"
    private int weekNumber;          // Which week this topic is in
    private int dayNumber;           // Which day of the week

    @Enumerated(EnumType.STRING)
    private TopicStatus status = TopicStatus.PENDING;

    private int confidenceScore = 5; // 1–10, updated by student
    private boolean isWeak = false;  // Auto-flagged if confidence < 4

    public enum TopicStatus {
        PENDING, IN_PROGRESS, COMPLETED, STRUGGLING
    }
}