package com.foodbridge.auth.controller;

import com.foodbridge.auth.dto.UserResponse;
import com.foodbridge.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal-facing lookup endpoint used by other services (via OpenFeign) to
 * resolve a user id to profile/verification details without duplicating the
 * users table. Intended to sit behind the API Gateway's internal network
 * segment rather than being publicly exposed.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User profile lookup (used by other services)")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Fetch a user's public profile by id")
    public ResponseEntity<UserResponse> getById(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.getById(userId));
    }
}
