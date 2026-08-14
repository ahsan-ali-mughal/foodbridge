package com.foodbridge.common.exception;

/**
 * Thrown when a synchronous call to another microservice fails or times out.
 * Mapped to HTTP 502/503 by each service's {@code GlobalExceptionHandler}.
 */
public class DownstreamServiceException extends FoodBridgeException {

    public DownstreamServiceException(String serviceName, Throwable cause) {
        super("DOWNSTREAM_SERVICE_ERROR", "Call to downstream service failed: " + serviceName, cause);
    }
}
