package com.foodbridge.listing.dto;

import com.foodbridge.common.enums.ListingStatus;

import java.time.Instant;
import java.util.List;

public record ListingResponse(
        String id,
        Long donorId,
        String foodType,
        int quantityServings,
        List<String> imageUrls,
        double latitude,
        double longitude,
        String pickupAddress,
        ListingStatus status,
        Instant preparedAt,
        Instant expiryAt,
        Instant createdAt
) {
}
