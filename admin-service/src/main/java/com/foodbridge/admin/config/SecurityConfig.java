package com.foodbridge.admin.config;

import com.foodbridge.admin.security.SessionJwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SessionJwtAuthenticationFilter sessionJwtAuthenticationFilter;

    public SecurityConfig(SessionJwtAuthenticationFilter sessionJwtAuthenticationFilter) {
        this.sessionJwtAuthenticationFilter = sessionJwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Browser/session-based flow (unlike every other FoodBridge service): CSRF stays
                // enabled for state-changing form posts, and a login page is served instead of 401s.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/actuator/health/**", "/actuator/info").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form.disable())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(
                        (request, response, authException) -> response.sendRedirect("/login")))
                .addFilterBefore(sessionJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
