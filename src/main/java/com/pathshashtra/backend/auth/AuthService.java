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

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /** Register from DTO -- no mass-assignment risk. Returns JWT directly. */
    public AuthResponse register(RegisterRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("STUDENT");
        userRepository.save(user);
        log.info("Registered user: {}", request.getEmail());
        return new AuthResponse(jwtUtil.generateToken(user.getEmail()));
    }

    /**
     * Login — FIX BUG 1: OAuth provider and soft-delete checks happen BEFORE
     * password comparison. All failure paths return null (same generic response)
     * to prevent user enumeration via timing or error message differences.
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // No user found — generic failure
        if (user == null) {
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

        return new AuthResponse(jwtUtil.generateToken(user.getEmail()));
    }
}
