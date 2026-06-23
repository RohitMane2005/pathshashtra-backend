package com.pathshashtra.backend.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AUDIT FIX: Token removed from response body.
 * JWT is now delivered ONLY via HttpOnly cookie (cannot be read by JS).
 * Sending the token in the body undermined the cookie-only security model —
 * any XSS on the page could read it from the response.
 */
@Getter
@AllArgsConstructor
public class AuthResponse {

    private String message;

}