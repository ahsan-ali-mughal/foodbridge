package com.foodbridge.admin.config;

import com.foodbridge.admin.security.SessionKeys;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Relays the admin's session-held JWT onto every outbound Feign call to
 * auth-service, so admin-only endpoints there (list pending NGOs, approve
 * verification) authorize the call as the logged-in admin.
 */
@Configuration
public class FeignTokenRelayConfig {

    @Bean
    public RequestInterceptor tokenRelayInterceptor() {
        return template -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest request = attrs.getRequest();
            HttpSession session = request.getSession(false);
            if (session != null) {
                String token = (String) session.getAttribute(SessionKeys.ACCESS_TOKEN);
                if (token != null) {
                    template.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                }
            }
        };
    }
}
