package com.pathshashtra.backend.auth;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    // Use SecureRandom for security (not Random)
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    public String generateOtp(String email) {
        cleanExpired(); // FIX: evict stale entries so the map does not grow unboundedly
        // Generate cryptographically secure 6-digit OTP
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        otpStore.put(email.toLowerCase().trim(),
                new OtpEntry(otp, LocalDateTime.now().plusMinutes(10)));
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        if (email == null || otp == null) return false;

        OtpEntry entry = otpStore.get(email.toLowerCase().trim());
        if (entry == null) return false;

        if (LocalDateTime.now().isAfter(entry.expiry())) {
            otpStore.remove(email.toLowerCase().trim());
            return false;
        }

        if (!entry.otp().equals(otp.trim())) return false;

        // OTP used — remove immediately to prevent reuse
        otpStore.remove(email.toLowerCase().trim());
        return true;
    }

    public boolean hasOtp(String email) {
        if (email == null) return false;
        OtpEntry entry = otpStore.get(email.toLowerCase().trim());
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiry())) {
            otpStore.remove(email.toLowerCase().trim());
            return false;
        }
        return true;
    }

    // Clean up expired OTPs periodically (called on each generate)
    private void cleanExpired() {
        otpStore.entrySet().removeIf(e ->
                LocalDateTime.now().isAfter(e.getValue().expiry()));
    }

    record OtpEntry(String otp, LocalDateTime expiry) {}
}
