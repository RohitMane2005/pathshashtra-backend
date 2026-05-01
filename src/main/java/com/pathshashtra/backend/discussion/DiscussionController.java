package com.pathshashtra.backend.discussion;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/discussions")
public class DiscussionController {

    private final DiscussionService service;

    public DiscussionController(DiscussionService service) {
        this.service = service;
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
    public ResponseEntity<DiscussionPost> create(
            @RequestBody Map<String, String> body, Authentication auth) {
        return ResponseEntity.ok(service.createPost(
                auth.getName(), body.get("title"), body.get("content"), body.get("tags")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPost(id));
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<DiscussionReply> addReply(
            @PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        return ResponseEntity.ok(service.addReply(auth.getName(), id, body.get("content")));
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
