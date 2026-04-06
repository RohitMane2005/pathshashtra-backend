package com.pathshashtra.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * FIX: Added validation annotations so malformed payloads are rejected at
 * the controller layer with a 400, not a 500 NullPointerException deep in
 * the service layer. Without these, POST {} body caused NPE in UserRepository.
 *
 * Password policy (uppercase + digit) is enforced server-side so direct
 * API calls can't bypass the frontend validation.
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email is too long")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be 8–128 characters")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
        message = "Password must contain at least one uppercase letter and one number"
    )
    private String password;
}
