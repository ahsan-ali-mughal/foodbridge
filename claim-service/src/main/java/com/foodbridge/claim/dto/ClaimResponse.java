package com.foodbridge.claim.dto;

import com.foodbridge.common.enums.ClaimStatus;

import java.time.Instant;

public record ClaimResponse(
        Long id,
        String listingId,
        Long ngoId,
        ClaimStatus status,
        Instant claimedAt,
        Instant completedAt
) {
}
