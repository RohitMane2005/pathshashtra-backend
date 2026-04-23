package com.pathshashtra.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT utility — updated for jjwt 0.12.x API.
 * Deprecated parserBuilder() → Jwts.parser(), setSubject → subject(), etc.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {

        // FIX: Enforce minimum secret length at startup.
        // HMAC-SHA256 requires >= 256 bits (32 bytes). A short secret is weak
        // and jjwt will silently accept it, making tokens forgeable.
        byte[] secretBytes = secret.getBytes();
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException(
                "JWT secret is too short (" + secretBytes.length + " bytes). " +
                "Minimum 32 bytes (64 hex chars) required. " +
                "Generate with: openssl rand -hex 64"
            );
        }

        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expiration = expiration;
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Date expiry = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return !expiry.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
