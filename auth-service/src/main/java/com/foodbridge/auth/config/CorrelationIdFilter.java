package com.foodbridge.auth.config;

import com.foodbridge.common.util.CorrelationIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads (or generates) a correlation id for every inbound request, puts it in
 * the logging MDC so every log line for this request is traceable, and
 * echoes it back on the response header for client-side log correlation.
 * The same MDC key is picked up when publishing Kafka events (see AuthServiceImpl).
 */
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CorrelationIdHolder.HTTP_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(CorrelationIdHolder.MDC_KEY, correlationId);
        response.setHeader(CorrelationIdHolder.HTTP_HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationIdHolder.MDC_KEY);
        }
    }
}
