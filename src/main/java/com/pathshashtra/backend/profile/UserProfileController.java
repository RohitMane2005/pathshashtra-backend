package com.pathshashtra.backend.profile;

import jakarta.validation.Valid;
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

    @PostMapping
    public ResponseEntity<UserProfile> saveProfile(
            @Valid @RequestBody UserProfileRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.saveProfile(authentication.getName(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfile> getProfile(Authentication authentication) {
        try {
            return ResponseEntity.ok(service.getProfile(authentication.getName()));
        } catch (RuntimeException e) {
            // Profile not yet created — return empty 204 instead of 404 error
            return ResponseEntity.noContent().build();
        }
    }
}
