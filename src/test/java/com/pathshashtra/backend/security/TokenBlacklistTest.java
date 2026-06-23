package com.pathshashtra.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenBlacklist — verifies Redis-backed token revocation.
 */
@ExtendWith(MockitoExtension.class)
class TokenBlacklistTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private TokenBlacklist tokenBlacklist;

    @BeforeEach
    void setUp() {
        tokenBlacklist = new TokenBlacklist(redisTemplate);
    }

    @Test
    @DisplayName("blacklist() stores JTI in Redis with correct TTL")
    void blacklist_storesWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        tokenBlacklist.blacklist("test-jti-123", 3600);

        verify(valueOps).set("blacklist:test-jti-123", "1", 3600, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("blacklist() ignores null JTI")
    void blacklist_nullJti_ignored() {
        tokenBlacklist.blacklist(null, 3600);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("blacklist() ignores zero or negative TTL")
    void blacklist_zeroTtl_ignored() {
        tokenBlacklist.blacklist("test-jti", 0);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("isBlacklisted() returns true when JTI exists in Redis")
    void isBlacklisted_existingJti_returnsTrue() {
        when(redisTemplate.hasKey("blacklist:revoked-jti")).thenReturn(true);

        assertThat(tokenBlacklist.isBlacklisted("revoked-jti")).isTrue();
    }

    @Test
    @DisplayName("isBlacklisted() returns false when JTI not in Redis")
    void isBlacklisted_unknownJti_returnsFalse() {
        when(redisTemplate.hasKey("blacklist:unknown-jti")).thenReturn(false);

        assertThat(tokenBlacklist.isBlacklisted("unknown-jti")).isFalse();
    }

    @Test
    @DisplayName("isBlacklisted() returns false for null JTI")
    void isBlacklisted_nullJti_returnsFalse() {
        assertThat(tokenBlacklist.isBlacklisted(null)).isFalse();
    }
}
