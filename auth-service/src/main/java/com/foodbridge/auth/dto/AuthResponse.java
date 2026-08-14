package com.foodbridge.auth.dto;

/**
 * Token pair returned on successful login/refresh.
 *
 * @param accessToken  short-lived JWT (see {@code security.jwt.access-token-ttl-minutes})
 * @param refreshToken longer-lived opaque-to-clients JWT used solely to mint new access tokens
 * @param tokenType    always "Bearer"
 * @param expiresInSeconds access token lifetime, for client-side proactive refresh scheduling
 */
public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {

    public static AuthResponse bearer(String accessToken, String refreshToken, long expiresInSeconds) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
