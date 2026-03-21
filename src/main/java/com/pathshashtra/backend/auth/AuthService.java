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

    /** Register from DTO — no mass-assignment risk. Returns JWT directly. */
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

    /** Login — generic error message prevents user enumeration. */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return new AuthResponse(jwtUtil.generateToken(user.getEmail()));
    }
}
