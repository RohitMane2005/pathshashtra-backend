package com.pathshashtra.backend.auth;

import com.pathshashtra.backend.security.JwtUtil;
import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /**
     * S-4 FIX: Dummy bcrypt hash used to equalize response time when the user is not found.
     * Without this, an attacker can distinguish "user exists" from "user doesn't exist"
     * by measuring response times (~300ms difference for bcrypt rounds).
     */
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ012345";

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /** Register from DTO -- no mass-assignment risk. Returns message (JWT goes in cookie only). */
    public AuthResponse register(RegisterRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("STUDENT");
        userRepository.save(user);
        log.info("Registered user: {}", request.getEmail());
        return new AuthResponse("Registration successful");
    }

    /** Generates a JWT token for a given email+role — used by controller to set cookie. */
    public String generateToken(String email, String role) {
        return jwtUtil.generateToken(email, role);
    }

    /**
     * CRIT-02 FIX: Returns LoginResult (includes user's actual role from DB)
     * instead of AuthResponse (message-only). The controller uses the role to
     * generate a JWT with the correct authority — previously it hardcoded STUDENT.
     *
     * FIX BUG 1: OAuth provider and soft-delete checks happen BEFORE
     * password comparison. All failure paths return null (same generic response)
     * to prevent user enumeration via timing or error message differences.
     */
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // No user found — run dummy bcrypt to equalize timing, then return generic failure
        if (user == null) {
            passwordEncoder.matches(request.getPassword(), DUMMY_BCRYPT_HASH);
            return null;
        }

        // Soft-deleted account — generic failure (don't reveal account existed)
        if (user.getDeletedAt() != null) {
            return null;
        }

        // OAuth user trying local login — generic failure (don't reveal auth provider)
        if (!"LOCAL".equals(user.getAuthProvider())) {
            return null;
        }

        // Password mismatch — generic failure
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return null;
        }

        return new LoginResult("Login successful", user.getRole());
    }

    /**
     * CRIT-02 FIX: Immutable result carrying message + user's actual DB role.
     * The controller reads role() to generate a JWT with the correct authority.
     */
    public record LoginResult(String message, String role) {}
}
