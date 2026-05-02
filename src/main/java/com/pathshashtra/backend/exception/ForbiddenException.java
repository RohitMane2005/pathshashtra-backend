package com.pathshashtra.backend.exception;

/**
 * Thrown when access to a resource is denied.
 * Mapped to HTTP 403 by GlobalExceptionHandler.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
