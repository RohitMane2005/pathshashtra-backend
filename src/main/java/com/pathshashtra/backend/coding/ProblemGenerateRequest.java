package com.pathshashtra.backend.coding;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProblemGenerateRequest {

    @Size(max = 100)
    private String topic;

    @Size(max = 20)
    private String difficulty;

    @Size(max = 50)
    private String language;
}
