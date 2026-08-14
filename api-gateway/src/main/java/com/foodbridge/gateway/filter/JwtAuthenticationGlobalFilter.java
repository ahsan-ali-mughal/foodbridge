package com.foodbridge.gateway.filter;

import com.foodbridge.common.dto.ApiError;
import com.foodbridge.common.util.CorrelationIdHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * Terminates JWT verification once at the edge. On success, the caller's
 * user id and role are rewritten into {@code X-User-Id} / {@code X-User-Role}
 * request headers for downstream services to trust — those services no
 * longer need to re-verify the JWT signature themselves for requests that
 * arrive via the gateway (they still validate it directly for any path that
 * might be reached another way, e.g. direct service-to-service Feign calls
 * that relay the original bearer token).
 *
 * <p>A correlation id is generated (or passed through) here as well, so
 * every request gets one at the earliest possible point in the platform.</p>
 */
@Component
@Slf4j
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/register", "/api/auth/login", "/api/auth/refresh",
            "/actuator/health", "/actuator/info"
    );

    private final SecretKey signingKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationGlobalFilter(@Value("${security.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        String correlationId = request.getHeaders().getFirst(CorrelationIdHolder.HTTP_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String finalCorrelationId = correlationId;

        if (isPublicPath(path)) {
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(CorrelationIdHolder.HTTP_HEADER, finalCorrelationId)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or malformed Authorization header", finalCorrelationId);
        }

        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(authHeader.substring(7)).getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(CorrelationIdHolder.HTTP_HEADER, finalCorrelationId)
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Gateway rejected invalid JWT on path={}: {}", path, ex.getMessage());
            return unauthorized(exchange, "Invalid or expired access token", finalCorrelationId);
        }
    }

    @Override
    public int getOrder() {
        return -100; // run before routing and rate limiting
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith)
                || path.contains("/swagger-ui") || path.contains("/v3/api-docs");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message, String correlationId) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        ApiError body = ApiError.of("UNAUTHORIZED", message, exchange.getRequest().getURI().getPath(), correlationId);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception ex) {
            bytes = ("{\"errorCode\":\"UNAUTHORIZED\",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
