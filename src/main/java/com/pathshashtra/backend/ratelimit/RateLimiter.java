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

    // ── AI endpoints ─────────────────────────────────────────────────────────

    /** 20 coding problems per user per day */
    public boolean allowCodingGenerate(String email) {
        return redis.isAllowed("ai_coding:" + email, 20, 86400);
    }

    /** 3 quiz sessions per user per day */
    public boolean allowQuizGenerate(String email) {
        return redis.isAllowed("ai_quiz:" + email, 3, 86400);
    }

    /** 5 roadmap generations per user per day */
    public boolean allowRoadmapGenerate(String email) {
        return redis.isAllowed("ai_roadmap:" + email, 5, 86400);
    }

    /** 3 career assessments per user per day */
    public boolean allowCareerAssessment(String email) {
        return redis.isAllowed("ai_career:" + email, 3, 86400);
    }

    /** 30 chat messages per user per hour */
    public boolean allowChatMessage(String email) {
        return redis.isAllowed("chat:" + email, 30, 3600);
    }

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

    /** Utility */
    public long remaining(String prefix, String identifier, int limit) {
        return redis.remaining(prefix, identifier, limit);
    }

    /** 120 requests/min per authenticated user — global API scraping protection */
    public boolean allowApiRequest(String email) {
        return redis.isAllowed("api:" + email, 120, 60);
    }
}
