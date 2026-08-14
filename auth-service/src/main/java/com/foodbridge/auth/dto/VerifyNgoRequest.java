package com.foodbridge.auth.dto;

import com.foodbridge.common.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;

public record VerifyNgoRequest(@NotNull VerificationStatus status) {
}
