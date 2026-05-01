package com.pathshashtra.backend.security;

import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${frontend.url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(JwtUtil jwtUtil, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();
        OAuth2User oAuth2User = oAuth2AuthenticationToken.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        if (email == null) {
            String login = (String) attributes.get("login");
            email = login != null ? login + "@github.com" : oAuth2User.getName() + "@provider.com";
        }
        if (name == null) {
            name = (String) attributes.get("login");
            if (name == null) name = "User";
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            // Register new user
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setRole("STUDENT");
            user.setAuthProvider(registrationId.toUpperCase());
            // Set a dummy password for OAuth users since they don't use it, but DB might require something
            // Ensure no one can log in using this password
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getEmail());

        // Redirect to frontend with token
        String redirectUrl = frontendUrl + "/oauth2/redirect?token=" + token;
        response.sendRedirect(redirectUrl);
    }
}
