package com.pathshashtra.backend.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class QuizStartResponse {
    private Long sessionId;
    private List<QuizQuestion> questions;

    @Getter
    @AllArgsConstructor
    public static class QuizQuestion {
        private int number;
        private String question;
        private List<String> options;
    }
}
