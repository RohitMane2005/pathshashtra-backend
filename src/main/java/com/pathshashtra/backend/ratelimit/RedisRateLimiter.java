package com.pathshashtra.backend.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

/**
 * Redis-backed sliding-window rate limiter.
 * Replaces the JVM ConcurrentHashMap version that reset on restart
 * and was not multi-instance safe.
 *
 * Uses a Lua script for atomicity:
 *  - Sorted set per key, score = epoch milliseconds
 *  - Remove members outside the window
 *  - Count members; if < limit, add new entry and return 1 (allowed)
 *  - Otherwise return 0 (denied)
 */
@Component
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;

    // Atomic sliding-window Lua script
    private static final RedisScript<Long> SLIDING_WINDOW = RedisScript.of(
        """
        local key        = KEYS[1]
        local now        = tonumber(ARGV[1])
        local windowMs   = tonumber(ARGV[2])
        local limit      = tonumber(ARGV[3])
        local windowStart = now - windowMs
        redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)
        local count = redis.call('ZCARD', key)
        if count < limit then
            redis.call('ZADD', key, now, now .. ':' .. ARGV[4])
            redis.call('PEXPIRE', key, windowMs + 1000)
            return 1
        end
        return 0
        """,
        Long.class
    );

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * @param key           unique rate-limit key (e.g. "login:user@example.com")
     * @param limit         max requests allowed in the window
     * @param windowSeconds sliding window size in seconds
     * @return true if the request is allowed
     */
    public boolean isAllowed(String key, int limit, long windowSeconds) {
        long nowMs = Instant.now().toEpochMilli();
        long windowMs = windowSeconds * 1000L;
        String uniqueMember = UUID.randomUUID().toString();

        Long result = redisTemplate.execute(
            SLIDING_WINDOW,
            Collections.singletonList("rl:" + key),
            String.valueOf(nowMs),
            String.valueOf(windowMs),
            String.valueOf(limit),
            uniqueMember
        );
        return Long.valueOf(1L).equals(result);
    }

    /**
     * Count remaining requests in the current window (for X-RateLimit-Remaining header).
     */
    public long remaining(String prefix, String identifier, int limit) {
        String key = "rl:" + prefix + identifier;
        long count = 0;
        try {
            Long card = redisTemplate.opsForZSet().zCard(key);
            count = card != null ? card : 0;
        } catch (Exception ignored) {}
        return Math.max(0, limit - count);
    }
}
