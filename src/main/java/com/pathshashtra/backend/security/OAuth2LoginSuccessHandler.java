package com.pathshashtra.backend.security;

import com.pathshashtra.backend.user.User;
import com.pathshashtra.backend.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OAuthCodeService oAuthCodeService;

    @Value("${frontend.url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(JwtUtil jwtUtil, UserRepository userRepository,
                                     PasswordEncoder passwordEncoder, OAuthCodeService oAuthCodeService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.oAuthCodeService = oAuthCodeService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();
        OAuth2User oAuth2User = oAuth2AuthenticationToken.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // ── Extract email ─────────────────────────────────────────────────
        // FIX 1: For GitHub, email may be null even with user:email scope if
        // the user's email is set to private. The attributes may contain it
        // at the top level or not at all. We handle both cases gracefully.
        String email = extractEmail(attributes, registrationId);
        String name = extractName(attributes);

        if (email == null || email.isBlank()) {
            // Cannot proceed without email — redirect with error
            log.warn("OAuth2 login failed: no email from provider {}", registrationId);
            String errorUrl = frontendUrl + "/login?error=" +
                    URLEncoder.encode("No email received from " + registrationId + ". Please make your email public in your provider settings.", StandardCharsets.UTF_8);
            response.sendRedirect(errorUrl);
            return;
        }

        // ── Find or create user ───────────────────────────────────────────
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            // FIX H1: Do NOT overwrite authProvider for existing LOCAL users.
            // Previously this changed LOCAL → GOOGLE/GITHUB, permanently locking
            // users out of password-based login with no consent or warning.
            // Existing users can log in via OAuth without changing their auth method.
            log.info("Existing user {} logged in via OAuth (provider: {}, authProvider stays: {})",
                    email, registrationId, user.getAuthProvider());
        } else {
            // Register new user
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setRole("STUDENT");
            user.setAuthProvider(registrationId.toUpperCase());
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            userRepository.save(user);
            log.info("Registered new OAuth2 user: {} via {}", email, registrationId);
        }

        // HIGH-05 FIX: Use the user's actual role from the DB, not the default STUDENT.
        // Previously used the 1-arg overload which always defaulted to STUDENT,
        // causing admins who login via OAuth to lose their admin privileges.
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        // ── Invalidate HTTP session after OAuth completes ─────────────────
        // Session was only needed for OAuth state storage. JWT is now the auth mechanism.
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        // ── SEC-01 FIX: Do NOT put JWT in the redirect URL ────────────────
        // Previously: redirected to /oauth2/redirect?token=JWT
        // Problem:    JWT appears in browser history, server logs, Referrer headers
        // Fix:        Issue a one-time, 30-second code stored in Redis.
        //             Frontend exchanges it via POST /api/auth/exchange-code.
        //             The JWT is set as an HttpOnly cookie in that response.
        String code = oAuthCodeService.generateCode(user.getEmail());
        String redirectUrl = frontendUrl + "/oauth2/redirect?code=" + code;
        response.sendRedirect(redirectUrl);
    }

    /**
     * Extract email from OAuth attributes.
     * FIX 1: For GitHub, the email field may be null if user's email is private.
     * We check standard locations and fall back gracefully.
     */
    private String extractEmail(Map<String, Object> attributes, String registrationId) {
        String email = (String) attributes.get("email");

        // GitHub may not include email in the top-level attributes even with user:email scope.
        // If it's null, we cannot fabricate a fake @github.com address because:
        //   1. It's not a real email (can't send password reset, notifications, etc.)
        //   2. If the user later makes their email public, re-login would create a duplicate account
        //   3. It violates the uniqueness assumption across providers
        if (email != null) return email;

        // For GitHub specifically: the login attribute is always present
        // but we MUST NOT fabricate an email from it — return null to trigger error flow
        if ("github".equals(registrationId)) {
            log.warn("GitHub user '{}' has no public email — cannot create account",
                    attributes.get("login"));
            return null;
        }

        return null;
    }

    /**
     * Extract display name from OAuth attributes.
     */
    private String extractName(Map<String, Object> attributes) {
        String name = (String) attributes.get("name");
        if (name == null || name.isBlank()) {
            name = (String) attributes.get("login"); // GitHub fallback
        }
        if (name == null || name.isBlank()) {
            name = "User";
        }
        return name;
    }
}
