package com.pathshashtra.backend.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * FIX D1: Global per-user API rate limiter.
 * 120 requests/min per authenticated user on all endpoints.
 * Prevents data scraping and API abuse on non-AI endpoints
 * (e.g., /users/leaderboard, /coding/problems, /bookmarks).
 */
@Component
public class GlobalRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    public GlobalRateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip auth endpoints (they have their own rate limits) and health check
        return path.startsWith("/api/auth") || path.equals("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            if (!rateLimiter.allowApiRequest(email)) {
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write("{\"error\":\"Too many requests. Please slow down.\",\"status\":429}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
