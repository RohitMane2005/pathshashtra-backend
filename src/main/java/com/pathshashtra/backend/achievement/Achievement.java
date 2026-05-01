package com.pathshashtra.backend.achievement;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "achievements", uniqueConstraints = {
    @UniqueConstraint(name = "uk_achievement_user_badge", columnNames = {"user_id", "badge_key"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "badge_key", nullable = false)
    private String badgeKey;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt = LocalDateTime.now();
}
