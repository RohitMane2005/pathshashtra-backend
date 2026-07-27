package com.pathshashtra.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis-backed JWT blacklist. Stores the JTI (JWT ID) of tokens that have been
 * explicitly revoked (logout, password change, account deletion) so they cannot
 * be reused even if still within their expiry window.
 *
 * Key pattern: blacklist:{jti}   TTL: remaining seconds of the token
 */
@Component
public class TokenBlacklist {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklist.class);

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklist(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Blacklist a token by its JTI for the remaining lifetime of the token.
     *
     * @param jti        the unique JWT ID claim
     * @param ttlSeconds remaining seconds until the token expires naturally
     */
    public void blacklist(String jti, long ttlSeconds) {
        if (jti == null || ttlSeconds <= 0) return;
        try {
            redisTemplate.opsForValue().set("blacklist:" + jti, "1", ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis error during token blacklist operation: {}", e.getMessage());
        }
    }

    /**
     * Returns true if this JTI has been explicitly revoked.
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + jti));
        } catch (Exception e) {
            log.warn("Redis error during token blacklist check: {}", e.getMessage());
            return false;
        }
    }
}
