package com.pathshashtra.backend.user;

import com.pathshashtra.backend.ratelimit.RateLimiter;
import com.pathshashtra.backend.subscription.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FIX: /api/quota was called by the frontend QuotaBar but no endpoint existed.
 * QuotaBar silently failed and never rendered. This adds the missing endpoint.
 * Now also respects the user's Pro plan for accurate limits.
 */
@RestController
@RequestMapping("/api/quota")
public class QuotaController {

    private final RateLimiter rateLimiter;
    private final SubscriptionService subscriptionService;

    public QuotaController(RateLimiter rateLimiter, SubscriptionService subscriptionService) {
        this.rateLimiter = rateLimiter;
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getQuota(Authentication auth) {
        String email = auth.getName();
        boolean isPro = subscriptionService.isPro(email);
        int proLimit = 10000;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("plan",      isPro ? "PRO" : "FREE");
        response.put("roadmap",   quota("ai_roadmap:",   email, isPro ? proLimit : 3));
        response.put("quiz",      quota("ai_quiz:",      email, isPro ? proLimit : 3));
        response.put("codingGen", quota("ai_coding:",    email, isPro ? proLimit : 20));
        response.put("studyPlan", quota("ai_study:",     email, isPro ? proLimit : 3));
        response.put("careerQ",   quota("ai_career:",    email, isPro ? proLimit : 3));
        response.put("chat",      quota("chat:",         email, isPro ? proLimit : 15));

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> quota(String prefix, String email, int limit) {
        int remaining = rateLimiter.remaining(prefix, email, limit);
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("limit",     limit);
        q.put("remaining", remaining);
        q.put("used",      limit - remaining);
        return q;
    }
}
