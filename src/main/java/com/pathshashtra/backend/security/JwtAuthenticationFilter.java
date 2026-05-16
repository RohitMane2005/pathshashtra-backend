package com.pathshashtra.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * JWT authentication filter — extracts the JWT from:
 *   1. HttpOnly auth cookie  (preferred — prevents XSS token theft)
 *   2. Authorization: Bearer <token> header (backward compat)
 *
 * Additionally checks the TokenBlacklist to reject revoked tokens
 * (fixes CRIT-02 — stolen tokens are invalidated on logout/password-change).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   TokenBlacklist tokenBlacklist,
                                   ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                if (!jwtUtil.validateToken(token)) {
                    sendUnauthorized(response, "Invalid or expired token");
                    return;
                }

                // CRIT-02 fix: reject blacklisted (revoked) tokens
                String jti = jwtUtil.extractJti(token);
                if (tokenBlacklist.isBlacklisted(jti)) {
                    sendUnauthorized(response, "Token has been revoked. Please log in again.");
                    return;
                }

                String email = jwtUtil.extractEmail(token);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email, null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                log.warn("JWT processing error: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                sendUnauthorized(response, "Authentication failed");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Cookie first (prevents XSS token theft), then Authorization header (backward compat). */
    private String resolveToken(HttpServletRequest request) {
        // 1. Check HttpOnly cookie
        String fromCookie = jwtUtil.extractFromCookie(request);
        if (fromCookie != null) return fromCookie;

        // 2. Fall back to Authorization header
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    /** Writes a JSON 401 response using ObjectMapper (safe — no string interpolation). */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of("error", message));
    }
}
