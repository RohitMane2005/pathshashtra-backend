package com.pathshashtra.backend.notification;

import com.pathshashtra.backend.exception.ForbiddenException;
import com.pathshashtra.backend.exception.NotFoundException;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notifRepo;
    private final UserRepository userRepo;

    public NotificationService(NotificationRepository notifRepo, UserRepository userRepo) {
        this.notifRepo = notifRepo;
        this.userRepo = userRepo;
    }

    /** Used by other services to send notifications. */
    public void send(Long userId, String type, String title, String message, String actionUrl) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setActionUrl(actionUrl);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notifRepo.save(n);
    }

    public Page<Notification> getNotifications(String email, int page) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return notifRepo.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, 20));
    }

    public long getUnreadCount(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return notifRepo.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markRead(String email, Long notifId) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        notifRepo.findById(notifId).ifPresent(n -> {
            if (n.getUserId().equals(user.getId())) {
                n.setRead(true);
                notifRepo.save(n);
            }
        });
    }

    /**
     * FIX BUG 10: Single bulk update marks ALL unread notifications as read.
     * Previously only processed the first 100 via PageRequest, silently
     * leaving remaining notifications unread.
     */
    @Transactional
    public void markAllRead(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        notifRepo.markAllReadByUserId(user.getId());
    }
}
