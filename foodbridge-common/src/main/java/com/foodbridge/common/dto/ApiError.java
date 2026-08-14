package com.foodbridge.common.dto;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response body returned by every FoodBridge service.
 * Keeping this shape identical across services simplifies client-side error handling.
 *
 * @param errorCode    stable machine-readable code, e.g. "RESOURCE_NOT_FOUND"
 * @param message      human-readable summary
 * @param path         request path that produced the error
 * @param timestamp    UTC timestamp of the error
 * @param correlationId request correlation id, useful for cross-service log lookups
 * @param details      optional list of field-level validation messages
 */
public record ApiError(
        String errorCode,
        String message,
        String path,
        Instant timestamp,
        String correlationId,
        List<String> details
) {
    public static ApiError of(String errorCode, String message, String path, String correlationId) {
        return new ApiError(errorCode, message, path, Instant.now(), correlationId, List.of());
    }

    public static ApiError withDetails(String errorCode, String message, String path,
                                        String correlationId, List<String> details) {
        return new ApiError(errorCode, message, path, Instant.now(), correlationId, details);
    }
}
