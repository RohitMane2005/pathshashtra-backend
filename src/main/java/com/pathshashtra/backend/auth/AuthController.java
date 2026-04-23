package com.pathshashtra.backend.auth;

import com.pathshashtra.backend.ratelimit.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final RateLimiter rateLimiter;

    public AuthController(AuthService authService, RateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Register — rate limited 5/hour per IP to prevent account flood.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);

        if (!rateLimiter.allowRegister(ip)) {
            log.warn("Register rate limit hit from ip={}", ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many registration attempts. Please try again later."));
        }

        String email = request.getEmail().trim().toLowerCase();
        request.setEmail(email);

        if (authService.emailExists(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered. Please sign in."));
        }

        AuthResponse response = authService.register(request);
        log.info("New user registered from ip={}", ip);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login — rate limited per IP (10/min) AND per email (5/5min).
     * FIX A1: Account lockout after 10 failed attempts in 15 minutes.
     * Uses generic error messages to prevent user enumeration.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String email = request.getEmail().trim().toLowerCase();
        request.setEmail(email);
        String ip = getClientIp(httpRequest);

        // FIX A1: Check account lockout BEFORE rate limit
        if (rateLimiter.isAccountLocked(email)) {
            log.warn("Login blocked — account locked for email={} ip={}", email, ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Account temporarily locked due to too many failed attempts. Please try again in 15 minutes."));
        }

        if (!rateLimiter.allowLogin(ip, email)) {
            log.warn("Login rate limit hit for email={} ip={}", email, ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many login attempts. Please wait a few minutes."));
        }

        AuthResponse response = authService.login(request);

        if (response == null) {
            // FIX A1: Record failed login attempt for lockout tracking
            rateLimiter.recordFailedLogin(email);
            log.warn("Failed login for email={} ip={}", email, ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        // FIX A1: Clear failed login counter on success
        rateLimiter.clearFailedLogins(email);
        log.info("Successful login for email={} ip={}", email, ip);
        return ResponseEntity.ok(response);
    }

    /**
     * FIX: X-Forwarded-For is used only if spring.server.forward-headers-strategy=framework
     * is set (it is, in application.properties).
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
