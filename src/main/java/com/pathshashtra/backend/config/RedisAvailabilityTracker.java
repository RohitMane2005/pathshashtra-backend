package com.pathshashtra.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple circuit breaker for Redis availability.
 *
 * Problem: When Redis is not installed (common in local dev), every HTTP request
 * triggers multiple Redis operations (TokenBlacklist, RateLimiter, JwtFilter),
 * each failing and logging a WARN — producing hundreds of identical log lines/sec.
 *
 * Solution: Check Redis connectivity once, cache the result, and re-check only
 * after a cooldown period (60s). All Redis-dependent components call isAvailable()
 * before attempting operations, skipping silently when Redis is down.
 */
@Component
public class RedisAvailabilityTracker {

    private static final Logger log = LoggerFactory.getLogger(RedisAvailabilityTracker.class);

    /** How long to wait before retrying after a failure (milliseconds). */
    private static final long RETRY_COOLDOWN_MS = 60_000L; // 60 seconds

    private final StringRedisTemplate redisTemplate;
    private final AtomicBoolean available = new AtomicBoolean(false);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    public RedisAvailabilityTracker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void checkOnStartup() {
        if (ping()) {
            available.set(true);
            log.info("Redis connection established — rate limiting, token blacklist, and OTP services active");
        } else {
            available.set(false);
            lastFailureTime.set(System.currentTimeMillis());
            log.warn("Redis is not available — rate limiting, token blacklist, and OTP features "
                    + "will be disabled. The app will function normally but without these security features. "
                    + "Will retry every {}s.", RETRY_COOLDOWN_MS / 1000);
        }
    }

    /**
     * Returns true if Redis is currently available.
     * After a failure, waits for the cooldown period before retrying.
     */
    public boolean isAvailable() {
        if (available.get()) {
            return true;
        }

        // Check if cooldown period has elapsed
        long elapsed = System.currentTimeMillis() - lastFailureTime.get();
        if (elapsed < RETRY_COOLDOWN_MS) {
            return false;
        }

        // Retry ping
        if (ping()) {
            available.set(true);
            log.info("Redis connection restored — security features re-enabled");
            return true;
        } else {
            lastFailureTime.set(System.currentTimeMillis());
            return false;
        }
    }

    /**
     * Mark Redis as unavailable (called by components on Redis exceptions).
     * Triggers the cooldown period.
     */
    public void markUnavailable() {
        if (available.compareAndSet(true, false)) {
            lastFailureTime.set(System.currentTimeMillis());
            log.warn("Redis connection lost — security features temporarily disabled. Will retry in {}s.",
                    RETRY_COOLDOWN_MS / 1000);
        }
    }

    private boolean ping() {
        try {
            String result = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equalsIgnoreCase(result);
        } catch (Exception e) {
            return false;
        }
    }
}
