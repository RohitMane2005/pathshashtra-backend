package com.pathshashtra.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String toEmail, String otp) {
        // If Gmail not configured, just log the OTP (dev mode)
        if (mailUsername.isBlank() || mailUsername.equals("YOUR_GMAIL@gmail.com")) {
            System.out.println("============================");
            System.out.println("DEV MODE — OTP for " + toEmail + " : " + otp);
            System.out.println("============================");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("PathShashtra - Verify Your Email 🎯");
            message.setText(
                "Welcome to PathShashtra!\n\n" +
                "Your OTP for email verification is:\n\n" +
                "  " + otp + "\n\n" +
                "This OTP is valid for 10 minutes.\n\n" +
                "Career · Study · Code — Powered by Intelligence\n" +
                "Team PathShashtra"
            );
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("Email send failed — OTP for " + toEmail + " : " + otp);
        }
    }
}
