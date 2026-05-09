package com.pathshashtra.backend.coding;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CodeFeedback {
    @JsonProperty("isCorrect")
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
