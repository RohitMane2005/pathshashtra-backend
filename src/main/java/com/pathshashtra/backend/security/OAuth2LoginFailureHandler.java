package com.pathshashtra.backend.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles OAuth2 login failures (provider denial, misconfigured credentials,
 * user consent cancellation, etc.).
 *
 * Without this, Spring Security shows a raw whitelabel error page
 * instead of redirecting back to the frontend with a user-friendly message.
 */
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.warn("OAuth2 login failed: {}", exception.getMessage());

        String errorMessage = "Login failed. Please try again.";

        // Provide slightly more specific messages for common failure modes
        String exMsg = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        if (exMsg.contains("cancel") || exMsg.contains("denied") || exMsg.contains("access_denied")) {
            errorMessage = "Login was cancelled. Please try again.";
        } else if (exMsg.contains("email")) {
            errorMessage = "Could not retrieve your email. Please check your provider settings.";
        }

        String redirectUrl = frontendUrl + "/login?error=" +
                URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }
}
