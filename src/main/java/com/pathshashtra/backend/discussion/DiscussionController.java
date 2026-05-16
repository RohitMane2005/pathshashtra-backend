package com.pathshashtra.backend.discussion;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.pathshashtra.backend.ratelimit.RateLimiter;

import java.util.Map;

/**
 * FIX BUG 7: Replaced raw Map request bodies with validated DTOs.
 * SEC-04 FIX: Added per-endpoint rate limits for posts (5/hr) and replies (30/hr).
 */
@RestController
@RequestMapping("/api/discussions")
public class DiscussionController {

    private final DiscussionService service;
    private final RateLimiter rateLimiter;

    public DiscussionController(DiscussionService service, RateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public ResponseEntity<Page<DiscussionPost>> list(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(service.listPosts(tag, search, sort, page));
    }

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody CreatePostRequest request, Authentication auth) {
        if (!rateLimiter.allowDiscussionPost(auth.getName())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "You can post at most 5 discussions per hour."));
        }
        return ResponseEntity.ok(service.createPost(
                auth.getName(), request.getTitle(), request.getContent(), request.getTags()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPost(id));
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<?> addReply(
            @PathVariable Long id, @Valid @RequestBody AddReplyRequest request, Authentication auth) {
        if (!rateLimiter.allowDiscussionReply(auth.getName())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "You can post at most 30 replies per hour."));
        }
        return ResponseEntity.ok(service.addReply(auth.getName(), id, request.getContent()));
    }

    @PostMapping("/{id}/upvote")
    public ResponseEntity<Map<String, Object>> upvotePost(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.toggleVote(auth.getName(), "POST", id));
    }

    @PostMapping("/reply/{id}/upvote")
    public ResponseEntity<Map<String, Object>> upvoteReply(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.toggleVote(auth.getName(), "REPLY", id));
    }
}
