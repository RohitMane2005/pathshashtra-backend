package com.pathshashtra.backend.user;

import com.pathshashtra.backend.ratelimit.RateLimiter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FIX: /api/quota was called by the frontend QuotaBar but no endpoint existed.
 * QuotaBar silently failed and never rendered. This adds the missing endpoint.
 */
@RestController
@RequestMapping("/api/quota")
public class QuotaController {

    private final RateLimiter rateLimiter;

    public QuotaController(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getQuota(Authentication auth) {
        String email = auth.getName();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("roadmap",   quota("ai_roadmap:",      email, 5));
        response.put("quiz",      quota("ai_quiz:",         email, 3));
        response.put("codingGen", quota("ai_coding_gen:",   email, 20));
        response.put("studyPlan", quota("ai_study:",        email, 3));
        response.put("careerQ",   quota("ai_career_q:",     email, 5));

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> quota(String prefix, String email, int limit) {
        int remaining = rateLimiter.remaining(prefix, email, limit);
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("limit", limit);
        q.put("remaining", remaining);
        q.put("used", limit - remaining);
        return q;
    }
}
