package com.pathshashtra.backend.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * CRIT-04 FIX: OTP state migrated from JVM ConcurrentHashMap to Redis.
 *
 * Old problem: JVM hashmap is reset on every app restart/deployment.
 *             A user requesting an OTP before deployment would have it
 *             silently invalidated. Multi-instance deployments had completely
 *             separate OTP stores — OTP generated on instance A would fail on instance B.
 *
 * New behaviour:
 *  - OTPs stored in Redis with 10-minute TTL (auto-expiry, no cleanup needed)
 *  - OTPs are single-use: consumed (deleted) on successful verification
 *  - Consistent across all app instances
 *  - Survives app restarts (Redis persists)
 *
 * Key pattern: otp:{email}   Value: OTP code   TTL: 10 minutes
 */
@Service
public class OtpService {

    private static final long OTP_TTL_MINUTES = 10L;
    private static final String PREFIX = "otp:";
    private final SecureRandom secureRandom = new SecureRandom();
    private final StringRedisTemplate redisTemplate;

    public OtpService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate and store a cryptographically secure 6-digit OTP for the email.
     * Any previously generated OTP for this email is overwritten.
     *
     * @return the 6-digit OTP to send to the user
     */
    public String generateOtp(String email) {
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String key = PREFIX + email.toLowerCase().trim();
        redisTemplate.opsForValue().set(key, otp, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        return otp;
    }

    /**
     * Verify the OTP for an email.
     * Single-use: the OTP is deleted on successful verification.
     *
     * @return true if the OTP matches and has not expired; false otherwise
     */
    public boolean verifyOtp(String email, String otp) {
        if (email == null || otp == null) return false;
        String key = PREFIX + email.toLowerCase().trim();
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) return false;
        if (!stored.equals(otp.trim())) return false;
        // Single-use: delete on success
        redisTemplate.delete(key);
        return true;
    }

    /**
     * Returns true if there is an active (non-expired) OTP for this email.
     */
    public boolean hasOtp(String email) {
        if (email == null) return false;
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + email.toLowerCase().trim()));
    }
}
