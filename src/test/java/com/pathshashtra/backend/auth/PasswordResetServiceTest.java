package com.pathshashtra.backend.auth;

import com.pathshashtra.backend.exception.BadRequestException;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PasswordResetService — covers request flow, token validation,
 * password reset, and expired/used token handling.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private PasswordResetRepository resetRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JavaMailSender mailSender;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private PasswordResetService passwordResetService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$hashedpassword");
    }

    @Test
    @DisplayName("requestReset() does nothing for non-existent email (prevents enumeration)")
    void requestReset_nonExistentEmail_noTokenCreated() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        // Should not throw — always returns success to prevent email enumeration
        assertThatCode(() -> passwordResetService.requestReset("unknown@test.com"))
                .doesNotThrowAnyException();

        verify(resetRepository, never()).save(any());
    }

    @Test
    @DisplayName("requestReset() creates token and invalidates old ones for valid email")
    void requestReset_validEmail_createsToken() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        passwordResetService.requestReset("test@example.com");

        // Old tokens should be deleted
        verify(resetRepository).deleteByUserId(1L);
        // New token should be saved
        verify(resetRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("isTokenValid() returns false for unknown token")
    void isTokenValid_unknownToken_returnsFalse() {
        when(resetRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThat(passwordResetService.isTokenValid("invalid-token")).isFalse();
    }

    @Test
    @DisplayName("resetPassword() throws BadRequestException for invalid token")
    void resetPassword_invalidToken_throws() {
        when(resetRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("bad-token", "NewPassword1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or expired");
    }

    @Test
    @DisplayName("resetPassword() throws for already-used token")
    void resetPassword_usedToken_throws() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(testUser);
        token.setUsed(true);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        when(resetRepository.findByToken(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("some-token", "NewPassword1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already used");
    }

    @Test
    @DisplayName("resetPassword() throws for weak password (no uppercase)")
    void resetPassword_weakPassword_throws() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(testUser);
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        when(resetRepository.findByToken(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("valid-token", "weakpassword1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("uppercase");
    }

    @Test
    @DisplayName("resetPassword() succeeds with valid token and strong password")
    void resetPassword_validTokenAndPassword_succeeds() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(testUser);
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        when(resetRepository.findByToken(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("ValidPass1")).thenReturn("$2a$10$newEncoded");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        assertThatCode(() -> passwordResetService.resetPassword("valid-token", "ValidPass1"))
                .doesNotThrowAnyException();

        // Password should be updated
        verify(userRepository).save(testUser);
        assertThat(testUser.getPassword()).isEqualTo("$2a$10$newEncoded");

        // Token should be marked as used
        verify(resetRepository).save(token);
        assertThat(token.isUsed()).isTrue();

        // Password-change timestamp should be stored in Redis
        verify(valueOps).set(eq("pwd_changed:test@example.com"), anyString(), eq(86400L), any());
    }
}
