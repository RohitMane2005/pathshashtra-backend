package com.pathshashtra.backend.quiz;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class QuizSubmitRequest {
    private Long sessionId;
    // Map of question number → selected answer e.g. {"1": "A", "2": "C", ...}
    private Map<String, String> answers;
}
