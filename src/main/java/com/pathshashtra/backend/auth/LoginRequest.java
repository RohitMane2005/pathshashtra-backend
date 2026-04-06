package com.pathshashtra.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * FIX: Added @NotBlank/@Email constraints. Without these, POST /api/auth/login
 * with an empty body {} threw NullPointerException inside UserRepository
 * returning HTTP 500 instead of the correct 400.
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(max = 128, message = "Password too long")
    private String password;
}
