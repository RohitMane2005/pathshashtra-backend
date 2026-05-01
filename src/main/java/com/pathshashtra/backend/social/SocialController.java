package com.pathshashtra.backend.social;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social")
public class SocialController {

    private final SocialService service;

    public SocialController(SocialService service) {
        this.service = service;
    }

    @PostMapping("/follow/{userId}")
    public ResponseEntity<Map<String, Object>> toggleFollow(
            @PathVariable Long userId, Authentication auth) {
        return ResponseEntity.ok(service.toggleFollow(auth.getName(), userId));
    }

    @GetMapping("/following")
    public ResponseEntity<List<Map<String, Object>>> getFollowing(Authentication auth) {
        return ResponseEntity.ok(service.getFollowing(auth.getName()));
    }

    @GetMapping("/followers")
    public ResponseEntity<List<Map<String, Object>>> getFollowers(Authentication auth) {
        return ResponseEntity.ok(service.getFollowers(auth.getName()));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> getProfile(
            @PathVariable Long userId, Authentication auth) {
        return ResponseEntity.ok(service.getPublicProfile(auth.getName(), userId));
    }

    @GetMapping("/compare/{userId}")
    public ResponseEntity<Map<String, Object>> compare(
            @PathVariable Long userId, Authentication auth) {
        return ResponseEntity.ok(service.compareWith(auth.getName(), userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(
            @RequestParam String q, Authentication auth) {
        return ResponseEntity.ok(service.searchUsers(auth.getName(), q));
    }
}
