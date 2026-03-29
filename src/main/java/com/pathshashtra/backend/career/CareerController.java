package com.pathshashtra.backend.career;

import com.pathshashtra.backend.ratelimit.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/career")
public class CareerController {

    private final CareerService careerService;
    private final RateLimiter rateLimiter;

    public CareerController(CareerService careerService, RateLimiter rateLimiter) {
        this.careerService = careerService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * GET /api/career/questions
     * Returns 12 AI-generated psychometric questions personalised to the user.
     * Rate limited: 5 question sets per day.
     */
    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(Authentication auth) {
        String email = auth.getName();
        if (!rateLimiter.isAllowed("ai_career_q:" + email, 5, 86_400L)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Daily limit reached. You can generate up to 5 assessments per day."));
        }
        return ResponseEntity.ok(careerService.getQuestions(email));
    }

    /**
     * POST /api/career/submit
     * Submit answers and receive full career analysis result.
     * Rate limited: 3 submissions per day.
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitAssessment(@RequestBody CareerSubmitRequest request, Authentication auth) {
        String email = auth.getName();
        if (!rateLimiter.isAllowed("ai_career_submit:" + email, 3, 86_400L)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Daily submission limit reached. Try again tomorrow."));
        }
        return ResponseEntity.ok(careerService.submitAssessment(email, request));
    }

    /**
     * GET /api/career/my
     * Lists all past career assessments for the authenticated user.
     */
    @GetMapping("/my")
    public ResponseEntity<List<CareerAssessmentSummary>> getMyAssessments(Authentication auth) {
        return ResponseEntity.ok(careerService.getMyAssessments(auth.getName()));
    }

    /**
     * GET /api/career/result/{id}
     * Returns full result for a specific assessment.
     */
    @GetMapping("/result/{id}")
    public ResponseEntity<CareerResult> getResult(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(careerService.getResult(auth.getName(), id));
    }
}
