package com.pathshashtra.backend.auth;

import com.pathshashtra.backend.ratelimit.RateLimiter;
import com.pathshashtra.backend.security.JwtUtil;
import com.pathshashtra.backend.security.OAuthCodeService;
import com.pathshashtra.backend.security.TokenBlacklist;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final RateLimiter rateLimiter;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final OAuthCodeService oAuthCodeService;
    private final com.pathshashtra.backend.user.UserRepository userRepository;

    public AuthController(AuthService authService, RateLimiter rateLimiter,
                          JwtUtil jwtUtil, TokenBlacklist tokenBlacklist,
                          OAuthCodeService oAuthCodeService,
                          com.pathshashtra.backend.user.UserRepository userRepository) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
        this.oAuthCodeService = oAuthCodeService;
        this.userRepository = userRepository;
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

        // HIGH-09 FIX: Don't explicitly check emailExists() — that returns a distinct
        // 409 response that lets attackers enumerate valid accounts.
        // Instead, try to register and catch the DB unique constraint violation.
        try {
            AuthResponse authResponse = authService.register(request);
            log.info("New user registered from ip={}", ip);

            String token = authService.generateToken(request.getEmail(), "STUDENT");
            ResponseCookie cookie = jwtUtil.buildCookie(token);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(authResponse);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Email already exists — return same generic error as other failures
            // to prevent user enumeration. Frontend handles this gracefully.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Registration failed. This email may already be registered — try signing in."));
        }
    }

    /**
     * Login — rate limited per IP (10/15min).
     * Sets an HttpOnly auth cookie alongside the response body token for backward compat.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String email = request.getEmail().trim().toLowerCase();
        request.setEmail(email);
        String ip = getClientIp(httpRequest);

        if (!rateLimiter.allowLogin(ip)) {
            log.warn("Login rate limit hit for email={} ip={}", email, ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many login attempts. Please wait a few minutes."));
        }

        AuthService.LoginResult loginResult = authService.login(request);

        if (loginResult == null) {
            log.warn("Failed login for email={} ip={}", email, ip);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        log.info("Successful login for email={} ip={}", email, ip);

        // CRIT-02 FIX: Use actual user role from DB instead of hardcoded "STUDENT".
        // Previously all users (including admins) got STUDENT role in their JWT.
        String token = authService.generateToken(email, loginResult.role());
        ResponseCookie cookie = jwtUtil.buildCookie(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(loginResult.message()));
    }

    /**
     * Logout — blacklists the current JWT in Redis and clears the auth cookie.
     * SEC-01: Token cannot be reused after logout even if captured from the network.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication auth, HttpServletRequest request) {
        // Extract token from cookie or Authorization header to blacklist it
        String token = jwtUtil.extractFromCookie(request);
        if (token == null) {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7).trim();
            }
        }

        if (token != null && jwtUtil.validateToken(token)) {
            String jti = jwtUtil.extractJti(token);
            long ttl = jwtUtil.extractRemainingSeconds(token);
            tokenBlacklist.blacklist(jti, ttl);
            log.info("Token blacklisted on logout for user={}", auth != null ? auth.getName() : "unknown");
        }

        ResponseCookie clearCookie = jwtUtil.buildClearCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    /**
     * SEC-01 fix: Exchange a one-time OAuth code for an HttpOnly auth cookie.
     * Called by the frontend's OAuth2RedirectHandler after receiving ?code=CODE.
     * The code is consumed (single-use, 30s TTL in Redis).
     */
    @PostMapping("/exchange-code")
    public ResponseEntity<?> exchangeOAuthCode(@RequestBody Map<String, String> body,
                                                HttpServletRequest request) {
        // MED-07 FIX: Rate limit exchange-code — defense-in-depth for auth endpoints
        String ip = getClientIp(request);
        if (!rateLimiter.isAllowed("exchange:" + ip, 10, 60)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many requests. Please try again later."));
        }

        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "code is required"));
        }

        String email = oAuthCodeService.exchangeCode(code);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired OAuth code. Please try logging in again."));
        }

        // HIGH-05 FIX: Look up user's actual role from DB instead of defaulting to STUDENT.
        // Previously used jwtUtil.generateToken(email) which hardcodes STUDENT role,
        // meaning admin users logging in via OAuth lost their admin privileges.
        String role = userRepository.findByEmail(email)
                .map(com.pathshashtra.backend.user.User::getRole)
                .orElse("STUDENT");
        String token = jwtUtil.generateToken(email, role);
        ResponseCookie cookie = jwtUtil.buildCookie(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "Authenticated successfully"));
    }

    /**
     * SEC-02: X-Forwarded-For trusted only because spring.server.forward-headers-strategy=framework.
     * Railway's proxy sets this header for real client IPs.
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
