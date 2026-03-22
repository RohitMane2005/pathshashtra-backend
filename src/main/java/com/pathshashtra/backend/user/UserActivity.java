package com.pathshashtra.backend.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * One row per user per calendar day they were active.
 * Written on any authenticated request via ActivityInterceptor.
 * Used to compute login streak on the dashboard.
 */
@Entity
@Table(name = "user_activity", indexes = {
    @Index(name = "idx_activity_user_date", columnList = "user_id, activity_date", unique = true)
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;
}
