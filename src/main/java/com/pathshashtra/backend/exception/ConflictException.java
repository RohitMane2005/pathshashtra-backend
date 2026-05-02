package com.pathshashtra.backend.exception;

/**
 * Thrown when a resource already exists or conflicts with current state.
 * Mapped to HTTP 409 by GlobalExceptionHandler.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
