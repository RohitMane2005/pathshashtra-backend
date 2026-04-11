package com.pathshashtra.backend.quiz;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class QuizSubmitRequest {
    @NotNull(message = "Session ID is required")
    private Long sessionId;

    // Map of question number → selected answer e.g. {"1": "A", "2": "C", ...}
    @NotEmpty(message = "Answers cannot be empty")
    private Map<String, String> answers;
}
