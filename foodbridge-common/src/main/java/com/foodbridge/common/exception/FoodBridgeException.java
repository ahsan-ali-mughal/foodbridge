package com.foodbridge.common.exception;

/**
 * Base unchecked exception for all FoodBridge domain errors.
 * Carries a stable {@code errorCode} that clients can branch on,
 * independent of the (human-readable, possibly-changing) message.
 */
public class FoodBridgeException extends RuntimeException {

    private final String errorCode;

    protected FoodBridgeException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected FoodBridgeException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
