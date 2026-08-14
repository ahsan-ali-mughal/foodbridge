package com.foodbridge.auth.security;

import com.foodbridge.common.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Issues and validates JWT access/refresh tokens. Access tokens carry the
 * user id, role, and verification flag as claims so downstream services can
 * authorize requests without a synchronous call back to auth-service.
 */
@Service
@Slf4j
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_VERIFIED = "verified";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String email, Role role, boolean verified) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessTokenTtlMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_VERIFIED, verified)
                .claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.refreshTokenTtlDays(), ChronoUnit.DAYS);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TOKEN_TYPE, TYPE_REFRESH)
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtlMinutes() * 60;
    }

    /**
     * Parses and validates a token's signature and expiry.
     *
     * @throws JwtException if the token is malformed, expired, or has an invalid signature
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Rejected invalid JWT: {}", ex.getMessage());
            throw ex;
        }
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public Long extractUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }
}
