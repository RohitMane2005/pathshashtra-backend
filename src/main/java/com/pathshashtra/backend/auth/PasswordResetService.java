package com.pathshashtra.backend.auth;

import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetRepository resetRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public PasswordResetService(PasswordResetRepository resetRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                JavaMailSender mailSender,
                                StringRedisTemplate redisTemplate) {
        this.resetRepository = resetRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Initiates password reset. Always returns success to prevent email enumeration.
     * If no account exists for the email, no token is created and no email is sent.
     */
    @Transactional
    public void requestReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email (not revealing to caller)");
            return;
        }

        User user = userOpt.get();
        // Invalidate any existing token for this user
        resetRepository.deleteByUserId(user.getId());

        // Generate a 32-byte URL-safe token (sent to user in email)
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // SEC-07 fix: store only the SHA-256 hash in the DB — raw token never persisted
        // If the DB is breached, attackers cannot use stored hashes to reset accounts.
        String tokenHash = sha256(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(tokenHash);  // store hash
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        resetToken.setUsed(false);
        resetRepository.save(resetToken);

        sendResetEmail(user.getEmail(), rawToken);  // send raw token in email
        log.info("Password reset token created for user {}", user.getId());
    }

    /**
     * Validates a reset token without consuming it (for the UI to check before showing the form).
     */
    public boolean isTokenValid(String rawToken) {
        return resetRepository.findByToken(sha256(rawToken))
                .map(t -> !t.isUsed() && !t.isExpired())
                .orElse(false);
    }

    /**
     * Resets the password using the token. Consumes (marks used) the token atomically.
     *
     * HIGH-04 FIX: After password change, bumps a Redis password-version counter.
     * JwtAuthenticationFilter checks this version against the token's issued-at time
     * to reject tokens issued before the password change.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        // SEC-07: look up by hash, not raw token
        PasswordResetToken resetToken = resetRepository.findByToken(sha256(rawToken))
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset link"));

        if (resetToken.isUsed()) throw new RuntimeException("Reset link already used");
        if (resetToken.isExpired()) throw new RuntimeException("Reset link has expired. Please request a new one.");

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetRepository.save(resetToken);

        // HIGH-04 FIX: Invalidate all existing sessions by recording password-change timestamp.
        // Any JWT issued before this timestamp will be rejected by JwtAuthenticationFilter.
        // Key uses email to match JwtAuthenticationFilter's lookup (JWT subject = email).
        String pwdChangeKey = "pwd_changed:" + user.getEmail();
        redisTemplate.opsForValue().set(pwdChangeKey,
                String.valueOf(System.currentTimeMillis()),
                86400, java.util.concurrent.TimeUnit.SECONDS); // 24h = max JWT lifetime

        log.info("[AUDIT] Password successfully reset for userId={}, all prior tokens invalidated", user.getId());
    }

    /** SHA-256 hex digest of input. Used for secure token storage. */
    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private void sendResetEmail(String toEmail, String token) {
        boolean devMode = mailUsername == null || mailUsername.isBlank()
                || mailUsername.equals("noreply@pathshashtra.com");

        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        if (devMode) {
            log.warn("[DEV] Password reset URL for {}: {}", toEmail, resetUrl);
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setSubject("PathShashtra — Reset Your Password");
            msg.setText(
                "Hello!\n\n" +
                "We received a request to reset your PathShashtra password.\n\n" +
                "Click the link below to set a new password (valid for 30 minutes):\n\n" +
                resetUrl + "\n\n" +
                "If you didn't request this, you can safely ignore this email.\n\n" +
                "Team PathShashtra"
            );
            mailSender.send(msg);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send reset email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send reset email. Please try again.");
        }
    }

    /** Clean up expired tokens every hour. */
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void purgeExpiredTokens() {
        resetRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
