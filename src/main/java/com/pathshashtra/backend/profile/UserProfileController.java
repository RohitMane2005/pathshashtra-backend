package com.pathshashtra.backend.profile;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    // Save or update profile for logged-in user
    @PostMapping
    public ResponseEntity<UserProfile> saveProfile(
            @RequestBody UserProfile profile,
            Authentication authentication) {
        String email = authentication.getName();
        UserProfile saved = service.saveProfile(email, profile);
        return ResponseEntity.ok(saved);
    }

    // Get profile for logged-in user
    @GetMapping("/me")
    public ResponseEntity<UserProfile> getProfile(Authentication authentication) {
        String email = authentication.getName();
        UserProfile profile = service.getProfile(email);
        return ResponseEntity.ok(profile);
    }
}
