package com.pathshashtra.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth") || path.equals("/api/health") || path.startsWith("/actuator")
                || path.startsWith("/api/quiz/share")
                || path.startsWith("/login/oauth2") || path.startsWith("/oauth2");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // FIX: No Authorization header — do NOT set authentication.
        // Spring Security's .anyRequest().authenticated() will then reject the request
        // with 401 via the HttpStatusEntryPoint configured in SecurityConfig.
        // Previously this was already handled correctly by Spring Security, but
        // making it explicit here prevents any future misconfiguration bypassing it.
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();

        if (token.isEmpty()) {
            sendUnauthorized(response, "Missing token");
            return;
        }

        try {
            if (jwtUtil.isTokenValid(token)) {
                String email = jwtUtil.extractEmail(token);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.debug("Rejected expired JWT for {}", request.getServletPath());
                sendUnauthorized(response, "Token expired");
                return;
            }
        } catch (Exception e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            sendUnauthorized(response, "Invalid token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * FIX BUG 11: Use Map + ObjectMapper for safe JSON serialization.
     * String concatenation could allow JSON injection if message ever
     * comes from user input or exception messages containing quotes.
     */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(
                java.util.Map.of("error", message, "status", 401)));
    }
}
