package com.pathshashtra.backend.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(userService.findByEmail(authentication.getName()));
    }

    /**
     * Permanently delete the authenticated user's account and all associated data.
     * Requires confirmation body: { "confirm": "DELETE" }
     */
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        if (!"DELETE".equals(body.get("confirm"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Send { \"confirm\": \"DELETE\" } to confirm account deletion."));
        }

        userService.deleteAccount(authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Account deleted. We're sorry to see you go."));
    }
}
