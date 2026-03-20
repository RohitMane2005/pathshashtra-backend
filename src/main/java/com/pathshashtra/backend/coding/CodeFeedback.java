package com.pathshashtra.backend.coding;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CodeFeedback {
    private boolean isCorrect;
    private int score;
    private String overallFeedback;
    private List<String> strengths;
    private List<String> improvements;
    private List<String> bugs;
    private String optimizedApproach;
    private String timeComplexity;
    private String spaceComplexity;
    private String suggestedTimeComplexity;
    private String suggestedSpaceComplexity;
}
