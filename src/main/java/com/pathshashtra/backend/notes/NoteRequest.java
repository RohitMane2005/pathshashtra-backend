package com.pathshashtra.backend.notes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * FIX C3: Request DTO for creating/updating notes.
 * Prevents mass assignment via the Note entity (which exposes userId, id, etc.).
 */
@Getter
@Setter
public class NoteRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be 1-200 characters")
    private String title;

    @Size(max = 50000, message = "Content must be under 50,000 characters")
    private String content;

    /** PROBLEM, TOPIC, GENERAL */
    @Size(max = 50, message = "Category too long")
    private String category;

    @Size(max = 500, message = "Tags too long")
    private String tags;
}
