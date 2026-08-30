package com.pathshashtra.backend.security;

import com.pathshashtra.backend.config.RedisAvailabilityTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Handles the OAuth2 "authorization code exchange" pattern.
 *
 * Instead of passing the JWT in the redirect URL (SEC-01 / CRIT-01), the backend:
 *   1. Generates a one-time, short-lived code (30s TTL) stored in Redis
 *   2. Redirects the frontend with ?code=<code>
 *   3. Frontend calls POST /api/auth/exchange-code with the code
 *   4. Backend returns the JWT as an HttpOnly cookie
 *
 * This ensures the JWT never appears in the URL, browser history, or server logs.
 */
@Component
public class OAuthCodeService {

    private static final Logger log = LoggerFactory.getLogger(OAuthCodeService.class);
    private static final long CODE_TTL_SECONDS = 30L;
    private static final String PREFIX = "oauth_code:";

    private final StringRedisTemplate redisTemplate;
    private final RedisAvailabilityTracker redisTracker;

    public OAuthCodeService(StringRedisTemplate redisTemplate, RedisAvailabilityTracker redisTracker) {
        this.redisTemplate = redisTemplate;
        this.redisTracker = redisTracker;
    }

    /**
     * Generate a single-use code that maps to the user's email.
     * The code is valid for 30 seconds and consumed on exchange.
     */
    public String generateCode(String email) {
        String code = UUID.randomUUID().toString();
        if (!redisTracker.isAvailable()) {
            log.warn("Redis unavailable — OAuth code exchange will not work. "
                    + "OAuth login requires Redis for secure code exchange.");
            return code;
        }
        try {
            redisTemplate.opsForValue().set(PREFIX + code, email, CODE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            redisTracker.markUnavailable();
            log.warn("Redis error storing OAuth code: {}", e.getMessage());
        }
        return code;
    }

    /**
     * Exchange the one-time code for the user email.
     * Returns null if the code is invalid or expired.
     *
     * CRIT-02 FIX: Uses atomic getAndDelete() (Redis GETDEL command) instead of
     * separate GET + DELETE. The old approach had a TOCTOU race condition where
     * two concurrent requests could both read the email before either deleted the key.
     */
    public String exchangeCode(String code) {
        if (code == null || code.isBlank()) return null;
        if (!redisTracker.isAvailable()) {
            log.warn("Redis unavailable — cannot exchange OAuth code");
            return null;
        }
        try {
            String key = PREFIX + code;
            // Atomic: returns value and deletes key in one Redis command
            return redisTemplate.opsForValue().getAndDelete(key);
        } catch (Exception e) {
            redisTracker.markUnavailable();
            log.warn("Redis error exchanging OAuth code: {}", e.getMessage());
            return null;
        }
    }
}
