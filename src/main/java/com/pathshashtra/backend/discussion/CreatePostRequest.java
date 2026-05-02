package com.pathshashtra.backend.discussion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * FIX BUG 7: Typed request DTO with validation for creating discussion posts.
 * Prevents NPE from null title/content and enforces size limits.
 */
@Getter
@Setter
public class CreatePostRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be 3-200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 10, max = 10000, message = "Content must be 10-10000 characters")
    private String content;

    @Size(max = 200, message = "Tags too long")
    private String tags;
}
