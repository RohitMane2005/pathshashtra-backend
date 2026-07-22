package com.pathshashtra.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * HIGH-06 FIX: Validated DTO for account deletion.
 * Previously the endpoint accepted a raw Map<String, String> with no validation,
 * allowing arbitrarily large payloads and missing the @Valid annotation path.
 */
@Getter
@Setter
public class DeleteAccountRequest {

    @NotBlank(message = "Confirmation is required")
    @Pattern(regexp = "DELETE", message = "Send \"DELETE\" to confirm account deletion")
    private String confirm;

    @Size(max = 128, message = "Password too long")
    private String password; // Nullable for OAuth users who never set a password
}
