package com.foodbridge.common.util;

/**
 * Constants shared by every service's correlation-id filter/interceptor so
 * the HTTP header name, MDC key, and Kafka header name never drift apart.
 */
public final class CorrelationIdHolder {

    public static final String HTTP_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String KAFKA_HEADER = "correlationId";

    private CorrelationIdHolder() {
    }
}
