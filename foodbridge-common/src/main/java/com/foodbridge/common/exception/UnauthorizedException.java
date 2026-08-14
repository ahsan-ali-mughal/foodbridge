package com.foodbridge.common.exception;

/**
 * Thrown when authentication fails or credentials are invalid.
 * Mapped to HTTP 401 by each service's {@code GlobalExceptionHandler}.
 */
public class UnauthorizedException extends FoodBridgeException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
