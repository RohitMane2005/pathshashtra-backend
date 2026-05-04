package com.pathshashtra.backend.user;

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

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Authentication auth) {
        return ResponseEntity.ok(userService.findByEmail(auth.getName()));
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
     * FIX H3: Require password verification for account deletion.
     * A compromised JWT + trivially guessable {"confirm":"DELETE"} body
     * should not be enough to permanently delete an account.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @RequestBody Map<String, String> body,
            Authentication auth) {

        if (!"DELETE".equals(body.get("confirm"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Send { \"confirm\": \"DELETE\", \"password\": \"...\" } to confirm account deletion."));
        }
        String password = body.get("password");
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password is required to confirm account deletion."));
        }
        userService.deleteAccount(auth.getName(), password);
        return ResponseEntity.ok(Map.of("message", "Account deleted. We're sorry to see you go."));
    }
}
