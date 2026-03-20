package com.pathshashtra.backend.coding;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProblemGenerateRequest {
    private String topic;
    private String difficulty;
    private String language;
}
