package com.pathshashtra.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Async email service — @Async ensures SMTP calls don't block the HTTP request thread.
 * Requires @EnableAsync on the main application class.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendOtp(String toEmail, String otp) {
        // If Gmail not configured, just log the OTP (dev mode)
        if (mailUsername.isBlank() || mailUsername.equals("YOUR_GMAIL@gmail.com")) {
            log.info("[DEV] OTP for {} : {}", toEmail, otp);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("PathShashtra - Verify Your Email \uD83C\uDFAF");
            message.setText(
                "Welcome to PathShashtra!\n\n" +
                "Your OTP for email verification is:\n\n" +
                "  " + otp + "\n\n" +
                "This OTP is valid for 10 minutes.\n\n" +
                "Career · Study · Code — Powered by Intelligence\n" +
                "Team PathShashtra"
            );
            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Email send failed for {} : {}", toEmail, e.getMessage());
        }
    }
}
