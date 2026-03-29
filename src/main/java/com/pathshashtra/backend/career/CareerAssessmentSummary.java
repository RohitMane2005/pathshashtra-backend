package com.pathshashtra.backend.career;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class CareerAssessmentSummary {
    private Long id;
    private String topCareer;
    private Integer topMatchScore;
    private String personalitySummary;
    private LocalDateTime completedAt;
}
