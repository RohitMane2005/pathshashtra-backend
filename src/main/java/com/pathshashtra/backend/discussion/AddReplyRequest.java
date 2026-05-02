package com.pathshashtra.backend.discussion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * FIX BUG 7: Typed request DTO with validation for adding discussion replies.
 */
@Getter
@Setter
public class AddReplyRequest {

    @NotBlank(message = "Reply content is required")
    @Size(min = 1, max = 5000, message = "Reply must be 1-5000 characters")
    private String content;
}
