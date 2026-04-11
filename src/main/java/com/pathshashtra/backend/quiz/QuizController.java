package com.pathshashtra.backend.quiz;

import com.pathshashtra.backend.ratelimit.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;
    private final RateLimiter rateLimiter;

    public QuizController(QuizService quizService, RateLimiter rateLimiter) {
        this.quizService = quizService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startQuiz(Authentication auth) {
        String email = auth.getName();
        if (!rateLimiter.allowQuizStart(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Daily limit reached. You can start up to 3 quizzes per day."));
        }
        return ResponseEntity.ok()
                .header("X-RateLimit-Remaining", String.valueOf(rateLimiter.remaining("ai_quiz:", email, 3)))
                .body(quizService.startQuiz(email));
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@Valid @RequestBody QuizSubmitRequest request, Authentication auth) {
        return ResponseEntity.ok(quizService.submitQuizWithToken(auth.getName(), request));
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
