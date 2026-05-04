package com.pathshashtra.backend.quiz;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Legacy quiz endpoints — kept for backward compatibility.
 * New assessments go through /api/career.
 * These endpoints serve existing quiz results and public share links.
 */
@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/results")
    public ResponseEntity<List<QuizResultSummary>> getMyResults(Authentication auth) {
        return ResponseEntity.ok(quizService.getMyResults(auth.getName()));
    }

    @GetMapping("/results/{sessionId}")
    public ResponseEntity<QuizResult> getResult(@PathVariable Long sessionId, Authentication auth) {
        return ResponseEntity.ok(quizService.getResult(auth.getName(), sessionId));
    }

    /** Public endpoint — no auth needed, results accessible by share token. */
    @GetMapping("/share/{token}")
    public ResponseEntity<?> getSharedResult(@PathVariable String token) {
        return ResponseEntity.ok(quizService.getPublicResult(token));
    }
}
