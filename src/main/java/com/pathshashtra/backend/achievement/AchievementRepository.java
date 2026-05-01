package com.pathshashtra.backend.achievement;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByUserId(Long userId);
    Optional<Achievement> findByUserIdAndBadgeKey(Long userId, String badgeKey);
    boolean existsByUserIdAndBadgeKey(Long userId, String badgeKey);
    void deleteByUserId(Long userId);
}
