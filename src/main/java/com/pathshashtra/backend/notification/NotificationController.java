package com.pathshashtra.backend.notification;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<Notification>> list(
            @RequestParam(defaultValue = "0") int page, Authentication auth) {
        return ResponseEntity.ok(service.getNotifications(auth.getName(), page));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Authentication auth) {
        return ResponseEntity.ok(Map.of("count", service.getUnreadCount(auth.getName())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markRead(@PathVariable Long id, Authentication auth) {
        service.markRead(auth.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllRead(Authentication auth) {
        service.markAllRead(auth.getName());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
