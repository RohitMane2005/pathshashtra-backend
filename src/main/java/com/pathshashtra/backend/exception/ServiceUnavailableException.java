package com.pathshashtra.backend.exception;

/**
 * Thrown when an external service (AI, email, etc.) is temporarily unavailable.
 * Mapped to HTTP 503 by GlobalExceptionHandler.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
