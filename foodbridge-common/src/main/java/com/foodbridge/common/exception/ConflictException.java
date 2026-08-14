package com.foodbridge.common.exception;

/**
 * Thrown when an operation conflicts with the current state of a resource,
 * e.g. attempting to claim a listing that is already claimed.
 * Mapped to HTTP 409 by each service's {@code GlobalExceptionHandler}.
 */
public class ConflictException extends FoodBridgeException {

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
