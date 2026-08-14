package com.foodbridge.auth.exception;

import com.foodbridge.common.dto.ApiError;
import com.foodbridge.common.exception.ConflictException;
import com.foodbridge.common.exception.ForbiddenException;
import com.foodbridge.common.exception.ResourceNotFoundException;
import com.foodbridge.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Translates domain and framework exceptions into the shared {@link ApiError} shape.
 * Every branch logs at a level proportional to operability impact: 4xx client
 * errors are warnings (expected, high volume), unexpected exceptions are errors.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("Resource not found on {}: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage(), req);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest req) {
        log.warn("Conflict on {}: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getErrorCode(), ex.getMessage(), req);
    }

    @ExceptionHandler({UnauthorizedException.class, BadCredentialsException.class})
    public ResponseEntity<ApiError> handleUnauthorized(RuntimeException ex, HttpServletRequest req) {
        log.warn("Unauthorized request on {}: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), req);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        log.warn("Forbidden request on {}: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getErrorCode(), ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        log.warn("Validation failed on {}: {}", req.getRequestURI(), details);
        ApiError body = ApiError.withDetails("VALIDATION_FAILED", "Request validation failed",
                req.getRequestURI(), MDC.get("correlationId"), details);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, HttpServletRequest req) {
        ApiError body = ApiError.of(code, message, req.getRequestURI(), MDC.get("correlationId"));
        return ResponseEntity.status(status).body(body);
    }
}
