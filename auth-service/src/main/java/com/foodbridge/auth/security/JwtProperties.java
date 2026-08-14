package com.foodbridge.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code security.jwt.*} configuration. The signing secret must be
 * injected via environment variable / secrets manager in every non-local
 * environment — never committed to source control.
 *
 * @param secret                 Base64-encoded HMAC-SHA256 signing key, min 256 bits
 * @param accessTokenTtlMinutes  access token lifetime in minutes
 * @param refreshTokenTtlDays    refresh token lifetime in days
 * @param issuer                 JWT "iss" claim value
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, long accessTokenTtlMinutes, long refreshTokenTtlDays, String issuer) {
}
