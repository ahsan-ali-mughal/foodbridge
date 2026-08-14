package com.foodbridge.common.exception;

/**
 * Thrown when an authenticated principal lacks permission for the requested action,
 * e.g. an unverified NGO attempting to claim a listing.
 * Mapped to HTTP 403 by each service's {@code GlobalExceptionHandler}.
 */
public class ForbiddenException extends FoodBridgeException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
