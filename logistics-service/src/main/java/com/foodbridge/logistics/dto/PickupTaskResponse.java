package com.foodbridge.logistics.dto;

import com.foodbridge.common.enums.PickupStatus;

import java.time.Instant;

public record PickupTaskResponse(
        Long id,
        Long claimId,
        String listingId,
        Long volunteerId,
        PickupStatus status,
        Instant assignedAt,
        Instant pickedUpAt,
        Instant deliveredAt
) {
}
