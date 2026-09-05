package com.pathshashtra.backend.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /** Most recent active subscription for a user. */
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.status = 'ACTIVE' ORDER BY s.createdAt DESC")
    Optional<Subscription> findActiveByUserId(Long userId);

    /** Look up by Razorpay subscription ID (for webhook events). */
    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);

    /** Look up by Razorpay order ID (for payment verification). */
    Optional<Subscription> findByRazorpayOrderId(String razorpayOrderId);
}
