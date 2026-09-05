package com.pathshashtra.backend.ratelimit;

import org.springframework.stereotype.Component;

/**
 * Application-level rate limiter — delegates to RedisRateLimiter.
 * All window sizes and limits are defined here for easy tuning.
 *
 * Sliding window algorithm: see RedisRateLimiter for implementation.
 * All state lives in Redis — safe for multi-instance deployments and restarts.
 */
@Component
public class RateLimiter {

    private final RedisRateLimiter redis;

    public RateLimiter(RedisRateLimiter redis) {
        this.redis = redis;
    }

    /**
     * Generic pass-through for controllers that specify their own key/limit/window.
     * Prefer the named methods for consistency, but this is available for ad-hoc limits.
     */
    public boolean isAllowed(String key, int limit, long windowSeconds) {
        return redis.isAllowed(key, limit, windowSeconds);
    }

    // ── Auth endpoints ────────────────────────────────────────────────────────

    /** 10 login attempts per IP per 15 minutes */
    public boolean allowLogin(String ip) {
        return redis.isAllowed("login:ip:" + ip, 10, 900);
    }

    /** 5 registrations per IP per hour */
    public boolean allowRegister(String ip) {
        return redis.isAllowed("register:ip:" + ip, 5, 3600);
    }

    /** 3 password-reset requests per email per hour */
    public boolean allowPasswordReset(String email) {
        return redis.isAllowed("pwreset:" + email, 3, 3600);
    }

    // ── AI endpoints (Free / Pro tiers) ─────────────────────────────────────
    //
    // Free limits are conservative; Pro limits are effectively unlimited (10000).
    // Each method has a convenience overload that defaults to Free.

    private static final int PRO_LIMIT = 10000;

    /** Free: 20/day — Pro: unlimited */
    public boolean allowCodingGenerate(String email, boolean isPro) {
        return redis.isAllowed("ai_coding:" + email, isPro ? PRO_LIMIT : 20, 86400);
    }
    public boolean allowCodingGenerate(String email) { return allowCodingGenerate(email, false); }

    /** Free: 3/day — Pro: unlimited */
    public boolean allowQuizGenerate(String email, boolean isPro) {
        return redis.isAllowed("ai_quiz:" + email, isPro ? PRO_LIMIT : 3, 86400);
    }
    public boolean allowQuizGenerate(String email) { return allowQuizGenerate(email, false); }

    /** Free: 3/day — Pro: unlimited */
    public boolean allowRoadmapGenerate(String email, boolean isPro) {
        return redis.isAllowed("ai_roadmap:" + email, isPro ? PRO_LIMIT : 3, 86400);
    }
    public boolean allowRoadmapGenerate(String email) { return allowRoadmapGenerate(email, false); }

    /** Free: 3/day — Pro: unlimited */
    public boolean allowCareerAssessment(String email, boolean isPro) {
        return redis.isAllowed("ai_career:" + email, isPro ? PRO_LIMIT : 3, 86400);
    }
    public boolean allowCareerAssessment(String email) { return allowCareerAssessment(email, false); }

    /** Free: 30/day — Pro: unlimited */
    public boolean allowCodingHint(String email, boolean isPro) {
        return redis.isAllowed("ai_coding_hint:" + email, isPro ? PRO_LIMIT : 30, 86400);
    }
    public boolean allowCodingHint(String email) { return allowCodingHint(email, false); }

    /** Free: 30/day — Pro: unlimited */
    public boolean allowCodingSubmit(String email, boolean isPro) {
        return redis.isAllowed("ai_coding_submit:" + email, isPro ? PRO_LIMIT : 30, 86400);
    }
    public boolean allowCodingSubmit(String email) { return allowCodingSubmit(email, false); }

    /** Free: 3/day — Pro: unlimited */
    public boolean allowStudyPlanGenerate(String email, boolean isPro) {
        return redis.isAllowed("ai_study:" + email, isPro ? PRO_LIMIT : 3, 86400);
    }
    public boolean allowStudyPlanGenerate(String email) { return allowStudyPlanGenerate(email, false); }

    /** Free: 15/day — Pro: unlimited */
    public boolean allowChatMessage(String email, boolean isPro) {
        return redis.isAllowed("chat:" + email, isPro ? PRO_LIMIT : 15, 86400);
    }
    public boolean allowChatMessage(String email) { return allowChatMessage(email, false); }

    // ── Social / content endpoints ────────────────────────────────────────────

    /** 5 discussion posts per user per hour */
    public boolean allowDiscussionPost(String email) {
        return redis.isAllowed("discussion_post:" + email, 5, 3600);
    }

    /** 30 replies per user per hour */
    public boolean allowDiscussionReply(String email) {
        return redis.isAllowed("discussion_reply:" + email, 30, 3600);
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Generic rate-limit check with custom prefix, identifier, limit, and window.
     * FIX-8: windowSeconds is now a required parameter — the old hardcoded 24h window
     * was wrong for any caller needing per-minute or per-hour limits.
     */
    public boolean allowRequest(String prefix, String identifier, int limit, long windowSeconds) {
        return redis.isAllowed(prefix + identifier, limit, windowSeconds);
    }

    /**
     * Backward-compat overload defaulting to a 24-hour window.
     * Prefer the explicit 4-arg overload for new callers.
     */
    public boolean allowRequest(String prefix, String identifier, int limit) {
        return allowRequest(prefix, identifier, limit, 86400);
    }

    /** Returns remaining quota as int for header/response use */
    public int remaining(String prefix, String identifier, int limit) {
        return (int) redis.remaining(prefix, identifier, limit);
    }

    /** 120 requests/min per authenticated user — global API scraping protection */
    public boolean allowApiRequest(String email) {
        return redis.isAllowed("api:" + email, 120, 60);
    }
}
