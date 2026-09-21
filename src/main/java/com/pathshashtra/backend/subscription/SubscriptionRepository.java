package com.pathshashtra.backend.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /** Most recent active subscription for a user (LIMIT 1 via Spring Data naming). */
    Optional<Subscription> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    /** Convenience wrapper that callers use — always queries for 'ACTIVE'. */
    default Optional<Subscription> findActiveByUserId(Long userId) {
        return findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, "ACTIVE");
    }

    /** Look up by Razorpay subscription ID (for webhook events). */
    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);

    /** Look up by Razorpay order ID (for payment verification). */
    Optional<Subscription> findByRazorpayOrderId(String razorpayOrderId);
}
