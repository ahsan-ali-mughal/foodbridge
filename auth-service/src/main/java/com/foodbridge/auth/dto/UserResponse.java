package com.foodbridge.auth.dto;

import com.foodbridge.common.enums.Role;
import com.foodbridge.common.enums.VerificationStatus;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        Role role,
        VerificationStatus verificationStatus,
        Instant createdAt
) {
}
