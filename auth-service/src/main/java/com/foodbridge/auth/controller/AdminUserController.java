package com.foodbridge.auth.controller;

import com.foodbridge.auth.dto.UserResponse;
import com.foodbridge.auth.dto.VerifyNgoRequest;
import com.foodbridge.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin - Users", description = "NGO verification and user administration")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "List users filtered by role and verification status (e.g. pending NGOs)")
    public ResponseEntity<java.util.List<UserResponse>> list(@RequestParam com.foodbridge.common.enums.Role role,
                                                     @RequestParam com.foodbridge.common.enums.VerificationStatus status) {
        return ResponseEntity.ok(authService.listByRoleAndStatus(role, status));
    }

    @PatchMapping("/{userId}/verification")
    @Operation(summary = "Approve or reject an NGO's verification status")
    public ResponseEntity<UserResponse> verifyNgo(@PathVariable Long userId,
                                                    @Valid @RequestBody VerifyNgoRequest request) {
        return ResponseEntity.ok(authService.verifyNgo(userId, request));
    }
}
