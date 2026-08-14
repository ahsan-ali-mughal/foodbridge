package com.foodbridge.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for api-gateway: the platform's single public entry point.
 * Built on Spring Cloud Gateway (WebFlux/reactive), it terminates JWT
 * verification once at the edge, rewrites the caller's user id/role into
 * headers for downstream services, applies per-user rate limiting via
 * Redis, and routes to each business service by path prefix.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
