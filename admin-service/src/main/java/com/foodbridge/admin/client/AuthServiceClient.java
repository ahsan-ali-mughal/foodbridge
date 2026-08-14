package com.foodbridge.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @PostMapping("/api/auth/login")
    AuthResponse login(@RequestBody LoginRequest request);

    @GetMapping("/api/admin/users")
    List<UserResponse> listUsers(@RequestParam("role") String role, @RequestParam("status") String status);

    @PatchMapping("/api/admin/users/{userId}/verification")
    UserResponse verifyNgo(@PathVariable("userId") Long userId, @RequestBody VerifyNgoRequest request);

    record LoginRequest(String email, String password) {
    }

    record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {
    }

    record UserResponse(Long id, String email, String displayName, String role, String verificationStatus, Instant createdAt) {
    }

    record VerifyNgoRequest(String status) {
    }
}
