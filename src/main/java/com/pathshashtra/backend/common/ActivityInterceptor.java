package com.pathshashtra.backend.common;

import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserActivity;
import com.pathshashtra.backend.user.UserActivityRepository;
import com.pathshashtra.backend.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;

/**
 * Records one UserActivity row per authenticated user per calendar day.
 * Runs after every successful authenticated request — upsert-style using existsByUserIdAndActivityDate.
 */
@Component
public class ActivityInterceptor implements HandlerInterceptor {

    private final UserActivityRepository activityRepository;
    private final UserRepository userRepository;

    public ActivityInterceptor(UserActivityRepository activityRepository, UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
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

            userRepository.findByEmail(email).ifPresent(user -> {
                if (!activityRepository.existsByUserIdAndActivityDate(user.getId(), today)) {
                    UserActivity activity = new UserActivity();
                    activity.setUser(user);
                    activity.setActivityDate(today);
                    activityRepository.save(activity);
                }
            });
        } catch (Exception ignored) {
            // Never fail a request due to activity tracking
        }
        return true;
    }
}
