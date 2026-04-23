package com.pathshashtra.backend.auth;

import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetRepository resetRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public PasswordResetService(PasswordResetRepository resetRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                JavaMailSender mailSender) {
        this.resetRepository = resetRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
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

        // Generate a 32-byte URL-safe token
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        resetToken.setUsed(false);
        resetRepository.save(resetToken);

        sendResetEmail(user.getEmail(), token);
        log.info("Password reset token created for user {}", user.getId());
    }

    /**
     * Validates a reset token without consuming it (for the UI to check before showing the form).
     */
    public boolean isTokenValid(String token) {
        return resetRepository.findByToken(token)
                .map(t -> !t.isUsed() && !t.isExpired())
                .orElse(false);
    }

    /**
     * Resets the password using the token. Consumes (marks used) the token atomically.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset link"));

        if (resetToken.isUsed()) throw new RuntimeException("Reset link already used");
        if (resetToken.isExpired()) throw new RuntimeException("Reset link has expired. Please request a new one.");

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetRepository.save(resetToken);

        log.info("[AUDIT] Password successfully reset for userId={}", user.getId());
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
