package com.pathshashtra.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT utility — generates, validates, and parses JWTs.
 *
 * Changes vs. original:
 *  - Added JTI (jti) claim — unique per token, used for blacklisting on logout/password-change
 *  - Added HttpOnly cookie helpers — generates and reads the auth cookie
 *  - Startup validation ensures JWT_SECRET is at least 32 bytes (256 bits) to prevent HMAC weakness
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expirationMs;

    @Value("${app.cookie.name:auth_token}")
    private String cookieName;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:None}")
    private String cookieSameSite;

    @Value("${app.cookie.domain:}")
    private String cookieDomain;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                "JWT_SECRET must be at least 32 characters. Got: " +
                (secret == null ? "null" : secret.length() + " chars"));
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JwtUtil initialised — expiration={}ms, cookie={}, secure={}, sameSite={}",
                expirationMs, cookieName, cookieSecure, cookieSameSite);
    }

    // ── Token generation ──────────────────────────────────────────────────────

    /**
     * Generate a JWT with email as subject and role as a custom claim.
     * HIGH-06 FIX: Role is now embedded in the token so JwtAuthenticationFilter
     * can extract it without a DB lookup per request.
     */
    public String generateToken(String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())   // jti — enables blacklisting
                .subject(email)
                .claim("role", role != null ? role : "STUDENT")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /** Backward-compatible overload — defaults to STUDENT role. */
    public String generateToken(String email) {
        return generateToken(email, "STUDENT");
    }

    // ── Token parsing ─────────────────────────────────────────────────────────

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    public long extractRemainingSeconds(String token) {
        Date exp = parseClaims(token).getExpiration();
        long remaining = (exp.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /** HIGH-04 FIX: Extract issued-at time in millis for password-change validation. */
    public long extractIssuedAt(String token) {
        Date iat = parseClaims(token).getIssuedAt();
        return iat != null ? iat.getTime() : 0;
    }

    /** HIGH-06 FIX: Extract role claim for RBAC authority population. */
    public String extractRole(String token) {
        Object role = parseClaims(token).get("role");
        return role != null ? role.toString() : null;
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ── Cookie helpers ────────────────────────────────────────────────────────

    /**
     * Build a Set-Cookie response header for the JWT.
     * HttpOnly=true prevents JS access (mitigates CRIT-01).
     * SameSite=None + Secure allows cross-origin requests (frontend on different domain).
     */
    public ResponseCookie buildCookie(String token) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(cookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(expirationMs / 1000);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    /**
     * Build a cookie that clears the auth session (used on logout).
     */
    public ResponseCookie buildClearCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    /**
     * Extract the JWT from the auth cookie in an incoming request.
     * Returns null if the cookie is absent.
     */
    public String extractFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (cookieName.equals(c.getName())) {
                String v = c.getValue();
                return (v == null || v.isBlank()) ? null : v;
            }
        }
        return null;
    }

    public String getCookieName() { return cookieName; }
    public long getExpirationMs() { return expirationMs; }
}
