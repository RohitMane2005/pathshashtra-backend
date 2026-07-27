package com.pathshashtra.backend.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Custom Error Controller to replace Spring Boot's Whitelabel Error Page.
 * Returns structured JSON responses for unhandled errors dispatched to /error.
 */
@RestController
public class CustomErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object messageAttr = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exceptionAttr = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (statusAttr != null) {
            try {
                statusCode = Integer.parseInt(statusAttr.toString());
            } catch (NumberFormatException ignored) {
            }
        }

        HttpStatus status = HttpStatus.resolve(statusCode);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String errorMessage = "An unexpected error occurred";
        if (messageAttr != null && !messageAttr.toString().trim().isEmpty()) {
            errorMessage = messageAttr.toString();
        } else if (exceptionAttr instanceof Throwable) {
            Throwable th = (Throwable) exceptionAttr;
            if (th.getMessage() != null && !th.getMessage().trim().isEmpty()) {
                errorMessage = th.getMessage();
            }
        }

        log.error("Dispatched to /error with status {} — message: {}", statusCode, errorMessage);

        return ResponseEntity.status(status).body(Map.of(
                "error", errorMessage,
                "status", status.value(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
