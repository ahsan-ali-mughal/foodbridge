package com.foodbridge.auth.security;

import com.foodbridge.common.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates the {@code Authorization: Bearer <token>} header on every request
 * and populates the Spring Security context. Auth-service issues tokens;
 * this same filter class is duplicated (intentionally, to avoid a hard
 * runtime dependency) in every other service that accepts JWTs directly —
 * see api-gateway's {@code JwtAuthenticationFilter} for the reactive variant.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parseClaims(token);
                if (!jwtService.isRefreshToken(claims)) {
                    Long userId = jwtService.extractUserId(claims);
                    Role role = Role.valueOf(claims.get("role", String.class));
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
                    var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Ignoring invalid bearer token on {}: {}", request.getRequestURI(), ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
