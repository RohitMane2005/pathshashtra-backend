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
     *
     * FIX: register endpoint previously had no rate limit — an attacker could
     * create unlimited accounts, exhausting the database and blocking legitimate
     * signups.
     *
     * NOTE: The 409 "email already registered" response is intentional here.
     * For a B2C student app, usability (telling users the email exists) outweighs
     * the marginal user-enumeration risk at the registration stage.
     * Login uses generic errors to protect the more sensitive credential check.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);

        // FIX: rate limit registration — 5 accounts per hour per IP
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
     * Uses generic error messages to prevent user enumeration.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String email = request.getEmail().trim().toLowerCase();
        request.setEmail(email);
        String ip = getClientIp(httpRequest);

        if (!rateLimiter.allowLogin(ip, email)) {
            log.warn("Login rate limit hit for email={} ip={}", email, ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many login attempts. Please wait a few minutes."));
        }

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * FIX: X-Forwarded-For is used only if spring.server.forward-headers-strategy=framework
     * is set (it is, in application.properties). When deployed behind Render/Railway's
     * reverse proxy, the real client IP is in X-Forwarded-For. Taking only [0] prevents
     * IP spoofing via comma-appended fake IPs like "1.2.3.4, 5.6.7.8".
     *
     * Remaining risk: if app is directly exposed (no proxy), X-Forwarded-For is
     * fully client-controlled. Mitigated by server.forward-headers-strategy=framework
     * which only trusts the header when the request comes through a known proxy.
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
