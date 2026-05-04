package com.pathshashtra.backend.coding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProblemGenerateRequest {

    @NotBlank(message = "Topic is required")
    @Size(max = 100)
    private String topic;

    @NotBlank(message = "Difficulty is required")
    @Size(max = 20)
    private String difficulty;

    @NotBlank(message = "Language is required")
    @Size(max = 50)
    private String language;
}

