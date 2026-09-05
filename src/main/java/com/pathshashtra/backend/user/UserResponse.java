package com.pathshashtra.backend.user;

/**
 * HIGH-02 FIX: DTO that prevents User entity internal fields (authProvider, deletedAt)
 * from being leaked to the frontend. Only safe, non-sensitive fields are exposed.
 */
public record UserResponse(Long id, String name, String email, String role, String plan) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPlan() != null ? user.getPlan() : "FREE"
        );
    }
}
