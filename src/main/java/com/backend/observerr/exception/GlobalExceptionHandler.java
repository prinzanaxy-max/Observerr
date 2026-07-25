package com.backend.observerr.exception;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, Object>> handleJwtException(JwtException ex) {
        log.warn("JWT error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid or expired token");
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<Map<String, Object>> handleFieldValidationException(FieldValidationException ex) {
        HttpStatus status = "currentPassword".equals(ex.getField())
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;

        Map<String, Object> body = new HashMap<>();
        body.put("error", status == HttpStatus.UNAUTHORIZED ? "UNAUTHORIZED" : "VALIDATION_FAILED");
        body.put("field", ex.getField());
        body.put("message", ex.getMessage());
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex) {
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Profile picture must be 5 MB or smaller");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();

        if (message != null && message.equalsIgnoreCase("Email already registered")) {
            return buildErrorResponse(HttpStatus.CONFLICT, "CONFLICT", message);
        }

        if (message != null && message.equalsIgnoreCase("Institutional ID already registered")) {
            return buildErrorResponse(HttpStatus.CONFLICT, "CONFLICT", message);
        }

        if (message != null && message.equalsIgnoreCase("Invalid credentials")) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
        }

        log.error("Unhandled runtime exception: {}", message, ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("error", "VALIDATION_FAILED");
        body.put("message", "Request validation failed");
        body.put("errors", fieldErrors);
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred.");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
