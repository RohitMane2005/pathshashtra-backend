package com.pathshashtra.backend.security;

import com.pathshashtra.backend.config.RedisAvailabilityTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Handles the OAuth2 "authorization code exchange" pattern.
 *
 * Instead of passing the JWT in the redirect URL (SEC-01 / CRIT-01), the backend:
 *   1. Generates a one-time, short-lived code (30s TTL) stored in Redis (or in-memory fallback)
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
    private static final long CODE_TTL_MS = CODE_TTL_SECONDS * 1000L;
    private static final String PREFIX = "oauth_code:";

    private final StringRedisTemplate redisTemplate;
    private final RedisAvailabilityTracker redisTracker;

    // In-memory fallback map for environments where Redis is down or unavailable (e.g. local dev)
    private final Map<String, CodeEntry> fallbackMap = new ConcurrentHashMap<>();

    private record CodeEntry(String email, long expiryTime) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

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
        if (redisTracker.isAvailable()) {
            try {
                redisTemplate.opsForValue().set(PREFIX + code, email, CODE_TTL_SECONDS, TimeUnit.SECONDS);
                return code;
            } catch (Exception e) {
                redisTracker.markUnavailable();
                log.warn("Redis error storing OAuth code, falling back to in-memory store: {}", e.getMessage());
            }
        } else {
            log.info("Redis unavailable — using in-memory store for OAuth code exchange.");
        }

        // In-memory fallback
        cleanExpiredEntries();
        fallbackMap.put(code, new CodeEntry(email, System.currentTimeMillis() + CODE_TTL_MS));
        return code;
    }

    /**
     * Exchange the one-time code for the user email.
     * Returns null if the code is invalid or expired.
     *
     * CRIT-02 FIX: Uses atomic getAndDelete() (Redis GETDEL command) instead of
     * separate GET + DELETE. If Redis is unavailable, checks atomic ConcurrentHashMap remove.
     */
    public String exchangeCode(String code) {
        if (code == null || code.isBlank()) return null;

        if (redisTracker.isAvailable()) {
            try {
                String key = PREFIX + code;
                // Atomic: returns value and deletes key in one Redis command
                String email = redisTemplate.opsForValue().getAndDelete(key);
                if (email != null) {
                    return email;
                }
            } catch (Exception e) {
                redisTracker.markUnavailable();
                log.warn("Redis error exchanging OAuth code: {}", e.getMessage());
            }
        }

        // Check in-memory fallback
        CodeEntry entry = fallbackMap.remove(code);
        if (entry != null && !entry.isExpired()) {
            return entry.email();
        }

        return null;
    }

    private void cleanExpiredEntries() {
        long now = System.currentTimeMillis();
        fallbackMap.entrySet().removeIf(e -> e.getValue().expiryTime() < now);
    }
}
