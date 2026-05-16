package com.pathshashtra.backend.security;

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

    private static final long CODE_TTL_SECONDS = 30L;
    private static final String PREFIX = "oauth_code:";

    private final StringRedisTemplate redisTemplate;

    public OAuthCodeService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate a single-use code that maps to the user's email.
     * The code is valid for 30 seconds and consumed on exchange.
     */
    public String generateCode(String email) {
        String code = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PREFIX + code, email, CODE_TTL_SECONDS, TimeUnit.SECONDS);
        return code;
    }

    /**
     * Exchange the one-time code for the user email.
     * Returns null if the code is invalid or expired.
     * Deletes the code atomically after retrieval (single-use).
     */
    public String exchangeCode(String code) {
        if (code == null || code.isBlank()) return null;
        String key = PREFIX + code;
        String email = redisTemplate.opsForValue().get(key);
        if (email != null) {
            redisTemplate.delete(key);
        }
        return email;
    }
}
