package com.pathshashtra.backend.auth;

import com.pathshashtra.backend.security.JwtUtil;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService — covers registration, login, timing-attack
 * defense, and edge cases for OAuth/soft-deleted accounts.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("$2a$10$hashedpassword");
        testUser.setRole("STUDENT");
        testUser.setAuthProvider("LOCAL");
    }

    // ── Registration Tests ──────────────────────────────────────────────

    @Test
    @DisplayName("register() should save user and return success message")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setName("New User");
        req.setEmail("new@example.com");
        req.setPassword("Password1");

        when(passwordEncoder.encode("Password1")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(req);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Registration successful");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("Password1");
    }

    @Test
    @DisplayName("emailExists() returns true when email is in DB")
    void emailExists_true() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        assertThat(authService.emailExists("test@example.com")).isTrue();
    }

    @Test
    @DisplayName("emailExists() returns false when email is not in DB")
    void emailExists_false() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThat(authService.emailExists("missing@example.com")).isFalse();
    }

    // ── Login Tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("login() returns AuthResponse on valid credentials")
    void login_validCredentials_returnsResponse() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("Password1");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password1", "$2a$10$hashedpassword")).thenReturn(true);

        AuthResponse response = authService.login(req);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Login successful");
    }

    @Test
    @DisplayName("login() returns null and runs dummy bcrypt when user not found (timing attack defense)")
    void login_userNotFound_dummyBcrypt() {
        LoginRequest req = new LoginRequest();
        req.setEmail("missing@example.com");
        req.setPassword("Password1");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        AuthResponse response = authService.login(req);

        assertThat(response).isNull();
        // Verify dummy bcrypt comparison was made to equalize response time
        verify(passwordEncoder).matches(eq("Password1"), anyString());
    }

    @Test
    @DisplayName("login() returns null for wrong password")
    void login_wrongPassword_returnsNull() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("WrongPass1");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPass1", "$2a$10$hashedpassword")).thenReturn(false);

        AuthResponse response = authService.login(req);

        assertThat(response).isNull();
    }

    @Test
    @DisplayName("login() returns null for soft-deleted account (no leak)")
    void login_softDeletedAccount_returnsNull() {
        testUser.setDeletedAt(java.time.LocalDateTime.now());
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("Password1");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        AuthResponse response = authService.login(req);

        assertThat(response).isNull();
        // Password should NOT be checked for deleted accounts
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("login() returns null for OAuth user attempting local login")
    void login_oauthUser_returnsNull() {
        testUser.setAuthProvider("GOOGLE");
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("Password1");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        AuthResponse response = authService.login(req);

        assertThat(response).isNull();
    }
}
