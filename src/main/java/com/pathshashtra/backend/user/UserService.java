package com.pathshashtra.backend.user;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Thin façade that delegates to UserQueryService and AccountDeletionService.
 * Kept for backward compatibility — any existing code that injects UserService
 * continues to work without changes to callers outside the user package.
 *
 * REFACTORED: Was a god class with 22 constructor dependencies.
 * Now delegates to focused services with single responsibilities.
 */
@Service
public class UserService {

    private final UserQueryService queryService;
    private final AccountDeletionService deletionService;

    public UserService(UserQueryService queryService,
                       AccountDeletionService deletionService) {
        this.queryService = queryService;
        this.deletionService = deletionService;
    }

    /** @see UserQueryService#findByEmail(String) */
    public User findByEmail(String email) {
        return queryService.findByEmail(email);
    }

    /** @see UserQueryService#getStreak(String) */
    public int getStreak(String email) {
        return queryService.getStreak(email);
    }

    /** @see UserQueryService#getLeaderboard() */
    public List<Map<String, Object>> getLeaderboard() {
        return queryService.getLeaderboard();
    }

    /** @see AccountDeletionService#deleteAccount(String, String) */
    public void deleteAccount(String email, String password) {
        deletionService.deleteAccount(email, password);
    }
}
