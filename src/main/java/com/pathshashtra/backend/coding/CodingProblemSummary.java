package com.pathshashtra.backend.coding;

import java.time.LocalDateTime;

/**
 * FIX-9: Type-safe JPA projection interface for the problems list view.
 *
 * Replaces the raw Object[] array-index access in CodingService.getMyProblems().
 * Spring Data JPA maps each getter to the corresponding column alias in the JPQL
 * SELECT clause -- if the query shape changes, this interface fails at startup
 * rather than silently mapping wrong data at runtime.
 *
 * Query order: id, problemTitle, topic, difficulty, language, status, hintsUsed, createdAt
 */
public interface CodingProblemSummary {
    Long getId();
    String getProblemTitle();
    String getTopic();
    String getDifficulty();
    String getLanguage();
    CodingProblem.ProblemStatus getStatus();
    Integer getHintsUsed();
    LocalDateTime getCreatedAt();
}