package com.pathshashtra.backend.coding;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class HintRequest {

    @NotNull(message = "Problem ID is required")
    private Long problemId;

    @Size(max = 50_000, message = "Code must be under 50,000 characters")
    private String currentCode;
}
