package com.pathshashtra.backend.subscription;

import com.pathshashtra.backend.exception.NotFoundException;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Core subscription service — creates Razorpay orders, verifies payments,
 * activates Pro plans, handles cancellations, and webhook events.
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    /** Pro plan price in paise (₹299 × 100 = 29900 paise). */
    private static final int PRO_AMOUNT_PAISE = 29900;
    private static final String CURRENCY = "INR";

    private final SubscriptionRepository subscriptionRepo;
    private final UserRepository userRepo;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    public SubscriptionService(SubscriptionRepository subscriptionRepo, UserRepository userRepo) {
        this.subscriptionRepo = subscriptionRepo;
        this.userRepo = userRepo;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Creates a Razorpay order for the Pro plan.
     * The frontend uses the returned orderId to open the Razorpay checkout.
     */
    @Transactional
    public Map<String, Object> createOrder(String email) {
        User user = findUser(email);

        if ("PRO".equals(user.getPlan())) {
            throw new IllegalStateException("You are already on the Pro plan.");
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject options = new JSONObject();
            options.put("amount",   PRO_AMOUNT_PAISE);
            options.put("currency", CURRENCY);
            options.put("receipt",  "rcpt_" + user.getId() + "_" + System.currentTimeMillis());
            options.put("payment_capture", 1);

            Order order = client.orders.create(options);
            String orderId = order.get("id");

            // Persist a pending subscription record
            Subscription sub = new Subscription();
            sub.setUser(user);
            sub.setRazorpayOrderId(orderId);
            sub.setStatus("PENDING");
            sub.setPlan("PRO");
            subscriptionRepo.save(sub);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("orderId",  orderId);
            response.put("amount",   PRO_AMOUNT_PAISE);
            response.put("currency", CURRENCY);
            response.put("keyId",    keyId);
            response.put("userName", user.getName());
            response.put("userEmail", user.getEmail());
            return response;

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for {}: {}", email, e.getMessage());
            throw new RuntimeException("Payment service unavailable. Please try again.");
        }
    }

    /**
     * Verifies the Razorpay HMAC signature and activates the Pro plan.
     * Signature = HMAC-SHA256(orderId + "|" + paymentId, keySecret)
     */
    @Transactional
    public Map<String, Object> verifyAndActivate(String email,
                                                  String orderId,
                                                  String paymentId,
                                                  String signature) {
        if (!verifySignature(orderId, paymentId, signature)) {
            throw new SecurityException("Payment signature verification failed.");
        }

        User user = findUser(email);

        // Activate the pending subscription record
        subscriptionRepo.findByRazorpayOrderId(orderId).ifPresent(sub -> {
            sub.setRazorpayPaymentId(paymentId);
            sub.setStatus("ACTIVE");
            sub.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
            subscriptionRepo.save(sub);
        });

        // Upgrade user plan
        user.setPlan("PRO");
        userRepo.save(user);

        log.info("User {} upgraded to PRO (orderId={})", email, orderId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("plan",    "PRO");
        response.put("message", "🎉 Welcome to PathShashtra Pro!");
        return response;
    }

    /** Cancels the user's active subscription and reverts to FREE. */
    @Transactional
    public Map<String, String> cancelSubscription(String email) {
        User user = findUser(email);

        subscriptionRepo.findActiveByUserId(user.getId()).ifPresent(sub -> {
            sub.setStatus("CANCELLED");
            subscriptionRepo.save(sub);
        });

        user.setPlan("FREE");
        userRepo.save(user);
        log.info("User {} cancelled Pro subscription", email);

        return Map.of("message", "Subscription cancelled. You now have a Free plan.");
    }

    /** Returns the user's current plan status. */
    public Map<String, Object> getStatus(String email) {
        User user = findUser(email);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("plan", user.getPlan());
        response.put("isPro", "PRO".equals(user.getPlan()));

        subscriptionRepo.findActiveByUserId(user.getId()).ifPresent(sub -> {
            if (sub.getCurrentPeriodEnd() != null) {
                response.put("expiresAt", sub.getCurrentPeriodEnd().toString());
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDateTime.now(), sub.getCurrentPeriodEnd());
                response.put("daysLeft", Math.max(0, daysLeft));
            }
        });
        return response;
    }

    /** Quick boolean check used by RateLimiter and QuotaController. */
    public boolean isPro(String email) {
        return userRepo.findByEmail(email)
                .map(u -> "PRO".equals(u.getPlan()))
                .orElse(false);
    }

    /**
     * Handles Razorpay server-side webhook events.
     * Validates the X-Razorpay-Signature header before processing.
     */
    @Transactional
    public void handleWebhook(String payload, String webhookSecret, String receivedSignature) {
        // Verify webhook signature: HMAC-SHA256(payload, webhookSecret)
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSig = HexFormat.of().formatHex(hash);
            if (!expectedSig.equals(receivedSignature)) {
                log.warn("Razorpay webhook signature mismatch — ignoring");
                return;
            }
        } catch (Exception e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return;
        }

        JSONObject event = new JSONObject(payload);
        String eventName = event.optString("event", "");
        log.info("Razorpay webhook event: {}", eventName);

        // Handle subscription cancellation from Razorpay dashboard
        if ("subscription.cancelled".equals(eventName) || "subscription.expired".equals(eventName)) {
            JSONObject subObj = event.optJSONObject("payload")
                    .optJSONObject("subscription").optJSONObject("entity");
            if (subObj != null) {
                String subId = subObj.optString("id");
                subscriptionRepo.findByRazorpaySubscriptionId(subId).ifPresent(sub -> {
                    sub.setStatus("CANCELLED");
                    subscriptionRepo.save(sub);
                    User u = sub.getUser();
                    u.setPlan("FREE");
                    userRepo.save(u);
                    log.info("User {} reverted to FREE via webhook ({})", u.getEmail(), eventName);
                });
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return expected.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private User findUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
