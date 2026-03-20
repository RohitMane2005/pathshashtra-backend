package com.pathshashtra.backend.auth;

import com.pathshashtra.backend.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (user.getName() == null || user.getName().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
        if (user.getEmail() == null || user.getEmail().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        if (user.getPassword() == null || user.getPassword().length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));

        user.setEmail(user.getEmail().toLowerCase().trim());

        if (authService.emailExists(user.getEmail()))
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered"));

        User saved = authService.register(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "name", saved.getName(),
                "email", saved.getEmail(),
                "role", saved.getRole()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password required"));
        request.setEmail(request.getEmail().toLowerCase().trim());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
