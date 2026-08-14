package com.foodbridge.listing.config;

import com.foodbridge.common.util.CorrelationIdHolder;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Propagates the current request's correlation id onto every outbound Feign
 * call so logs across services can be joined on the same id end-to-end.
 */
@Configuration
public class FeignCorrelationIdPropagationConfig {

    @Bean
    public RequestInterceptor correlationIdForwardingInterceptor() {
        return template -> {
            String correlationId = MDC.get(CorrelationIdHolder.MDC_KEY);
            if (correlationId != null) {
                template.header(CorrelationIdHolder.HTTP_HEADER, correlationId);
            }
        };
    }
}
