package com.pathshashtra.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key;
    private final long expiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {

        // FIX: Enforce minimum secret length at startup.
        // HMAC-SHA256 requires >= 256 bits (32 bytes). A short secret is weak
        // and jjwt will silently accept it, making tokens forgeable.
        // 64 hex chars = 32 bytes = 256 bits minimum.
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
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Date expiry = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return !expiry.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
