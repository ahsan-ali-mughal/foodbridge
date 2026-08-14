package com.foodbridge.common.exception;

/**
 * Thrown when a requested entity (listing, claim, user, etc.) does not exist.
 * Mapped to HTTP 404 by each service's {@code GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends FoodBridgeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("RESOURCE_NOT_FOUND", "%s not found for id: %s".formatted(resource, id));
    }
}
