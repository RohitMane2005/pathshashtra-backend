package com.pathshashtra.backend.bookmark;

import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final SavedItemRepository savedItemRepository;
    private final UserRepository userRepository;

    public BookmarkController(SavedItemRepository savedItemRepository, UserRepository userRepository) {
        this.savedItemRepository = savedItemRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<SavedItem>> getBookmarks(Authentication auth) {
        return ResponseEntity.ok(savedItemRepository.findByUserIdOrderBySavedAtDesc(getUser(auth).getId()));
    }

    @PostMapping("/toggle")
    @Transactional
    public ResponseEntity<Map<String, Object>> toggle(@RequestBody BookmarkRequest req, Authentication auth) {
        User user = getUser(auth);
        boolean exists = savedItemRepository.existsByUserIdAndTypeAndRefId(user.getId(), req.getType(), req.getRefId());
        if (exists) {
            savedItemRepository.deleteByUserIdAndTypeAndRefId(user.getId(), req.getType(), req.getRefId());
            return ResponseEntity.ok(Map.of("saved", false));
        }
        SavedItem item = new SavedItem();
        item.setUser(user);
        item.setType(req.getType());
        item.setRefId(req.getRefId());
        item.setLabel(req.getLabel() != null ? req.getLabel() : "");
        savedItemRepository.save(item);
        return ResponseEntity.ok(Map.of("saved", true));
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> check(
            @RequestParam String type, @RequestParam Long refId, Authentication auth) {
        boolean saved = savedItemRepository.existsByUserIdAndTypeAndRefId(getUser(auth).getId(), type, refId);
        return ResponseEntity.ok(Map.of("saved", saved));
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Getter @Setter
    public static class BookmarkRequest {
        private String type;
        private Long refId;
        private String label;
    }
}
