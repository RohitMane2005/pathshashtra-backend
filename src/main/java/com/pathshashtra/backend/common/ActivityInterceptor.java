package com.pathshashtra.backend.common;

import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserActivity;
import com.pathshashtra.backend.user.UserActivityRepository;
import com.pathshashtra.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * BE-05 FIX: Activity tracking now uses Redis for the "already recorded today" check.
 *
 * Old behaviour: every authenticated request triggered SELECT + INSERT against the DB.
 * New behaviour: SET activity:{userId}:{date} NX EX 86400 — one write per user per day.
 *              DB insert happens at most once per user per day.
 */
@Component
public class ActivityInterceptor implements HandlerInterceptor {

    private final UserActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    public ActivityInterceptor(UserActivityRepository activityRepository,
                               UserRepository userRepository,
                               StringRedisTemplate redisTemplate) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return true;
            }

            String email = auth.getName();
            LocalDate today = LocalDate.now();
            String redisKey = "activity:" + email + ":" + today.format(DateTimeFormatter.ISO_LOCAL_DATE);

            // Redis NX (set only if not exists) — returns true if the key was newly set
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", 86400, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(isNew)) {
                // First request today — write to DB
                userRepository.findByEmail(email).ifPresent(user -> {
                    if (!activityRepository.existsByUserIdAndActivityDate(user.getId(), today)) {
                        UserActivity activity = new UserActivity();
                        activity.setUser(user);
                        activity.setActivityDate(today);
                        activityRepository.save(activity);
                    }
                });
            }
        } catch (Exception ignored) {
            // Never fail a request due to activity tracking
        }
        return true;
    }
}
