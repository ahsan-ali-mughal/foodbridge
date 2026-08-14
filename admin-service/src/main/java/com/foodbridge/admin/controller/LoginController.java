package com.foodbridge.admin.controller;

import com.foodbridge.admin.client.AuthServiceClient;
import com.foodbridge.admin.security.SessionKeys;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Slf4j
public class LoginController {

    private final AuthServiceClient authServiceClient;

    public LoginController(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                         HttpServletRequest request, Model model) {
        try {
            AuthServiceClient.AuthResponse authResponse =
                    authServiceClient.login(new AuthServiceClient.LoginRequest(email, password));

            // The JWT's role claim is only checked at the security-filter level (hasRole("ADMIN"));
            // a non-admin who successfully authenticates will simply be denied on every subsequent
            // page by SecurityConfig, never granted dashboard access here.
            var session = request.getSession(true);
            session.setAttribute(SessionKeys.ACCESS_TOKEN, authResponse.accessToken());
            log.info("Admin login successful for email={}", email);
            return "redirect:/dashboard";
        } catch (FeignException.Unauthorized ex) {
            log.warn("Admin login failed for email={}", email);
            model.addAttribute("error", "Invalid email or password");
            return "login";
        } catch (FeignException ex) {
            log.error("Login call to auth-service failed", ex);
            model.addAttribute("error", "Login is temporarily unavailable, please try again shortly");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}
