package com.pathshashtra.backend.subscription;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST endpoints for the subscription lifecycle:
 *   POST /api/subscription/create-order  — create Razorpay order
 *   POST /api/subscription/verify        — verify payment & activate Pro
 *   POST /api/subscription/cancel        — cancel subscription
 *   GET  /api/subscription/status        — get current plan status
 *   POST /api/subscription/webhook       — Razorpay server-to-server events
 */
@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /** Creates a Razorpay order and returns orderId + keyId for the frontend. */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(Authentication auth) {
        return ResponseEntity.ok(subscriptionService.createOrder(auth.getName()));
    }

    /**
     * Verifies the Razorpay payment signature and upgrades the user to Pro.
     * Body: { orderId, paymentId, signature }
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String orderId   = body.get("orderId");
        String paymentId = body.get("paymentId");
        String signature = body.get("signature");

        if (orderId == null || paymentId == null || signature == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "orderId, paymentId, and signature are required"));
        }

        return ResponseEntity.ok(
            subscriptionService.verifyAndActivate(auth.getName(), orderId, paymentId, signature)
        );
    }

    /** Cancels the active subscription and reverts user to Free plan. */
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, String>> cancel(Authentication auth) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(auth.getName()));
    }

    /** Returns current plan, expiry date, and days remaining. */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(Authentication auth) {
        return ResponseEntity.ok(subscriptionService.getStatus(auth.getName()));
    }

    /**
     * Razorpay webhook — called server-to-server, no user auth.
     * Signature is verified inside SubscriptionService.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        subscriptionService.handleWebhook(payload, webhookSecret, signature);
        return ResponseEntity.ok().build();
    }
}
