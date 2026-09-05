package com.pathshashtra.backend.subscription;

import com.pathshashtra.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a Razorpay subscription/payment record for a user.
 * One user can have multiple records (upgrades, renewals).
 * The most recent ACTIVE record determines the user's current plan.
 */
@Entity
@Table(name = "subscriptions", indexes = {
    @Index(name = "idx_sub_user_id",         columnList = "user_id"),
    @Index(name = "idx_sub_razorpay_sub_id", columnList = "razorpay_subscription_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Razorpay order ID (from create-order call). */
    private String razorpayOrderId;

    /** Razorpay payment ID (from client-side checkout). */
    private String razorpayPaymentId;

    /** Razorpay subscription ID (for recurring plans). */
    private String razorpaySubscriptionId;

    /** Plan level: PRO or FREE. */
    private String plan = "PRO";

    /** ACTIVE, CANCELLED, EXPIRED. */
    private String status = "ACTIVE";

    /** When this billing period ends. Null = never (one-time payment). */
    private LocalDateTime currentPeriodEnd;

    private LocalDateTime createdAt = LocalDateTime.now();
}
