package com.pathshashtra.backend.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class QuizResultSummary {
    private Long sessionId;
    private String topCareer;
    private String summary;
    private LocalDateTime completedAt;
}
