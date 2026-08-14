package com.foodbridge.admin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates the admin dashboard's browser requests from the JWT stored
 * server-side in the HTTP session (set by {@code LoginController} after a
 * successful login), rather than an {@code Authorization} header — this is
 * the one service in the platform driven by a browser session instead of a
 * bearer-token API client.
 */
@Component
@Slf4j
public class SessionJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;

    public SessionJwtAuthenticationFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String token = (String) session.getAttribute(SessionKeys.ACCESS_TOKEN);
            if (token != null) {
                try {
                    Claims claims = jwtValidator.parseClaims(token);
                    Long userId = Long.valueOf(claims.getSubject());
                    String role = claims.get("role", String.class);
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (JwtException | IllegalArgumentException ex) {
                    log.debug("Session JWT invalid/expired, clearing session: {}", ex.getMessage());
                    session.invalidate();
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
