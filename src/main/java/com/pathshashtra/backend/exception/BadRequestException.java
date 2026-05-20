package com.pathshashtra.backend.exception;

/**
 * Thrown when a client sends a malformed or invalid request.
 * Mapped to HTTP 400 by GlobalExceptionHandler.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
