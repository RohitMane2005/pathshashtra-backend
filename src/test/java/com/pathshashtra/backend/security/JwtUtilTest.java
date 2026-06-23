package com.pathshashtra.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtUtil — covers token generation, parsing, validation,
 * and the startup secret-length check.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inject required properties via reflection (same as @Value injection)
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "test-secret-must-be-at-least-32-characters-long!!");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 86400000L);
        ReflectionTestUtils.setField(jwtUtil, "cookieName", "auth_token");
        ReflectionTestUtils.setField(jwtUtil, "cookieSecure", false);
        ReflectionTestUtils.setField(jwtUtil, "cookieSameSite", "Lax");
        ReflectionTestUtils.setField(jwtUtil, "cookieDomain", "");
        jwtUtil.init();
    }

    @Test
    @DisplayName("generateToken() produces a valid JWT that can be parsed")
    void generateToken_validJwt() {
        String token = jwtUtil.generateToken("user@test.com", "STUDENT");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("extractEmail() returns correct subject from token")
    void extractEmail_correctSubject() {
        String token = jwtUtil.generateToken("user@test.com", "STUDENT");

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("extractJti() returns non-null unique ID")
    void extractJti_nonNull() {
        String token = jwtUtil.generateToken("user@test.com");

        String jti = jwtUtil.extractJti(token);
        assertThat(jti).isNotBlank();
    }

    @Test
    @DisplayName("extractRole() returns role claim from token")
    void extractRole_returnsEmbeddedRole() {
        String token = jwtUtil.generateToken("user@test.com", "ADMIN");

        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("extractRole() defaults to STUDENT when no role provided")
    void extractRole_defaultsToStudent() {
        String token = jwtUtil.generateToken("user@test.com");

        assertThat(jwtUtil.extractRole(token)).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("validateToken() returns false for tampered token")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("user@test.com") + "tampered";

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken() returns false for empty string")
    void validateToken_emptyString_returnsFalse() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("extractRemainingSeconds() returns positive value for fresh token")
    void extractRemainingSeconds_freshToken_positive() {
        String token = jwtUtil.generateToken("user@test.com");

        long remaining = jwtUtil.extractRemainingSeconds(token);
        assertThat(remaining).isGreaterThan(0);
        assertThat(remaining).isLessThanOrEqualTo(86400);
    }

    @Test
    @DisplayName("init() throws if secret is too short")
    void init_shortSecret_throws() {
        JwtUtil shortSecretUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortSecretUtil, "secret", "short");
        ReflectionTestUtils.setField(shortSecretUtil, "expirationMs", 86400000L);
        ReflectionTestUtils.setField(shortSecretUtil, "cookieName", "auth_token");
        ReflectionTestUtils.setField(shortSecretUtil, "cookieSecure", false);
        ReflectionTestUtils.setField(shortSecretUtil, "cookieSameSite", "Lax");
        ReflectionTestUtils.setField(shortSecretUtil, "cookieDomain", "");

        assertThatThrownBy(shortSecretUtil::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }

    @Test
    @DisplayName("Two tokens for the same user have different JTIs")
    void generateToken_uniqueJtis() {
        String token1 = jwtUtil.generateToken("user@test.com");
        String token2 = jwtUtil.generateToken("user@test.com");

        assertThat(jwtUtil.extractJti(token1))
                .isNotEqualTo(jwtUtil.extractJti(token2));
    }

    @Test
    @DisplayName("buildCookie() creates HttpOnly cookie")
    void buildCookie_httpOnly() {
        String token = jwtUtil.generateToken("user@test.com");

        var cookie = jwtUtil.buildCookie(token);

        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getName()).isEqualTo("auth_token");
        assertThat(cookie.getValue()).isEqualTo(token);
    }

    @Test
    @DisplayName("buildClearCookie() creates cookie with maxAge=0")
    void buildClearCookie_maxAgeZero() {
        var cookie = jwtUtil.buildClearCookie();

        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getValue()).isEmpty();
    }
}
