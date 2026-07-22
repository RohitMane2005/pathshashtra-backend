package com.pathshashtra.backend.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** HIGH-02 FIX: Return UserResponse DTO instead of raw entity to prevent leaking internal fields. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication auth) {
        return ResponseEntity.ok(UserResponse.from(userService.findByEmail(auth.getName())));
    }

    /** Returns current user's login streak (consecutive days active). */
    @GetMapping("/streak")
    public ResponseEntity<Map<String, Integer>> getStreak(Authentication auth) {
        int streak = userService.getStreak(auth.getName());
        return ResponseEntity.ok(Map.of("streak", streak));
    }

    /** Top 20 users by XP for the leaderboard. Read-only, no auth required is fine but keeping it authenticated. */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard() {
        return ResponseEntity.ok(userService.getLeaderboard());
    }

    /**
     * HIGH-06 FIX: Uses validated DTO instead of raw Map<String, String>.
     * @Valid triggers JSR-303 validation before method body runs:
     *   - confirm must be exactly "DELETE" (@Pattern)
     *   - password max 128 chars (@Size)
     * GlobalExceptionHandler returns structured field errors on validation failure.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            Authentication auth) {

        String password = request.getPassword();
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password is required to confirm account deletion."));
        }
        userService.deleteAccount(auth.getName(), password);
        return ResponseEntity.ok(Map.of("message", "Account deleted. We're sorry to see you go."));
    }
}
