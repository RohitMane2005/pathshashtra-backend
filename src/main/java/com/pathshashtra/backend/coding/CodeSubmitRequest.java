package com.pathshashtra.backend.coding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CodeSubmitRequest {

    @NotNull(message = "Problem ID is required")
    private Long problemId;

    @NotBlank(message = "Code is required")
    @Size(max = 50_000, message = "Code must be under 50,000 characters")
    private String code;

    @NotBlank(message = "Language is required")
    @Size(max = 50)
    private String language;
}
