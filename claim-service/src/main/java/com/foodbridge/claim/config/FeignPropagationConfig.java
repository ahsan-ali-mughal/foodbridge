package com.foodbridge.claim.config;

import com.foodbridge.common.util.CorrelationIdHolder;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propagates both the correlation id and the original caller's bearer token
 * (token relay) onto every outbound Feign call. Token relay lets
 * listing-service's {@code /claimed} endpoint authorize the request as the
 * same NGO principal that initiated the claim, without claim-service having
 * to mint its own service-to-service credentials for this call.
 */
@Configuration
public class FeignPropagationConfig {

    @Bean
    public RequestInterceptor propagationInterceptor() {
        return template -> {
            String correlationId = MDC.get(CorrelationIdHolder.MDC_KEY);
            if (correlationId != null) {
                template.header(CorrelationIdHolder.HTTP_HEADER, correlationId);
            }

            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader != null) {
                    template.header(HttpHeaders.AUTHORIZATION, authHeader);
                }
            }
        };
    }
}
