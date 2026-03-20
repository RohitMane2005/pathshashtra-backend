package com.pathshashtra.backend.quiz;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // POST /api/quiz/start → Claude generates 10 questions
    @PostMapping("/start")
    public ResponseEntity<QuizStartResponse> startQuiz(Authentication auth) {
        QuizStartResponse response = quizService.startQuiz(auth.getName());
        return ResponseEntity.ok(response);
    }

    // POST /api/quiz/submit → Claude analyzes answers, returns full result
    @PostMapping("/submit")
    public ResponseEntity<QuizResult> submitQuiz(
            @RequestBody QuizSubmitRequest request,
            Authentication auth) {
        QuizResult result = quizService.submitQuiz(auth.getName(), request);
        return ResponseEntity.ok(result);
    }

    // GET /api/quiz/results → list all past quiz results
    @GetMapping("/results")
    public ResponseEntity<List<QuizResultSummary>> getMyResults(Authentication auth) {
        List<QuizResultSummary> results = quizService.getMyResults(auth.getName());
        return ResponseEntity.ok(results);
    }

    // GET /api/quiz/results/{sessionId} → get one full result
    @GetMapping("/results/{sessionId}")
    public ResponseEntity<QuizResult> getResult(
            @PathVariable Long sessionId,
            Authentication auth) {
        QuizResult result = quizService.getResult(auth.getName(), sessionId);
        return ResponseEntity.ok(result);
    }
}
