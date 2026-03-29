package com.pathshashtra.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = ex.getMessage();

        if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("not found"))        status = HttpStatus.NOT_FOUND;
            if (lower.contains("already registered")) status = HttpStatus.CONFLICT;
            if (lower.contains("invalid password")) status = HttpStatus.UNAUTHORIZED;
            if (lower.contains("unauthorized"))     status = HttpStatus.UNAUTHORIZED;
            if (lower.contains("token"))            status = HttpStatus.UNAUTHORIZED;
            if (lower.contains("maximum") || lower.contains("exceeded")) status = HttpStatus.TOO_MANY_REQUESTS;
            if (lower.contains("ai") || lower.contains("groq")) status = HttpStatus.SERVICE_UNAVAILABLE;
        }

        return ResponseEntity.status(status).body(Map.of(
                "error", message != null ? message : "Something went wrong",
                "status", status.value(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }


    /** FIX: handles @Valid failures — returns readable field errors instead of 500 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String firstError = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(Map.of(
                "error", firstError,
                "status", 400,
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid parameter: " + ex.getName(),
                "status", 400,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal server error",
                "status", 500,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
