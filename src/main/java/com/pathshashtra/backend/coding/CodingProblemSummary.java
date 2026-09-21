package com.pathshashtra.backend.coding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Type-safe JPA DTO for the problems list view.
 * Instantiated directly by JPQL constructor expression `SELECT new com.pathshashtra.backend.coding.CodingProblemSummary(...)`.
 * Eliminates interface-projection mapping and converter issues in Spring Data JPA.
 */
@Getter
@Setter
@NoArgsConstructor
public class CodingProblemSummary {
    private Long id;
    private String problemTitle;
    private String topic;
    private String difficulty;
    private String language;
    private CodingProblem.ProblemStatus status;
    private Integer hintsUsed;
    private LocalDateTime createdAt;

    public CodingProblemSummary(Long id, String problemTitle, String topic, String difficulty,
                                String language, CodingProblem.ProblemStatus status,
                                Integer hintsUsed, LocalDateTime createdAt) {
        this.id = id;
        this.problemTitle = problemTitle;
        this.topic = topic;
        this.difficulty = difficulty;
        this.language = language;
        this.status = status;
        this.hintsUsed = hintsUsed;
        this.createdAt = createdAt;
    }
}