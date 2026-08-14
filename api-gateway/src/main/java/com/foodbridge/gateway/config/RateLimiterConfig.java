package com.foodbridge.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

/**
 * Rate-limits by the authenticated user id when present (set by
 * {@code JwtAuthenticationGlobalFilter} as {@code X-User-Id}), falling back
 * to remote IP for unauthenticated requests (e.g. login attempts) so a
 * single anonymous client can't exhaust the gateway's request budget.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null) {
                return Mono.just(userId);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }
}
