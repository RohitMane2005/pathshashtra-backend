package com.pathshashtra.backend.auth;

import com.pathshashtra.backend.ratelimit.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

    private final PasswordResetService resetService;
    private final RateLimiter rateLimiter;

    public PasswordResetController(PasswordResetService resetService, RateLimiter rateLimiter) {
        this.resetService = resetService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Step 1: Request a password reset email.
     * Rate limited: 3 per hour per IP.
     * Always returns 200 to prevent email enumeration.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        String ip = getClientIp(httpRequest);
        if (!rateLimiter.isAllowed("forgot_pwd:" + ip, 3, 3600)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many requests. Please try again later."));
        }

        resetService.requestReset(request.getEmail());
        // Always 200 — don't reveal whether email exists
        return ResponseEntity.ok(Map.of(
                "message", "If an account with that email exists, a reset link has been sent."));
    }

    /**
     * Step 2: Validate token (called when user lands on reset page).
     */
    @GetMapping("/reset-password/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestParam String token) {
        boolean valid = resetService.isTokenValid(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    /**
     * Step 3: Set the new password.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        resetService.resetPassword(request.getToken(), request.getPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated. You can now sign in."));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    // ── Request bodies ────────────────────────────────────────────────────

    @Getter @Setter
    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;
    }

    @Getter @Setter
    public static class ResetPasswordRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
    }
}
