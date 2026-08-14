package com.foodbridge.listing.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateListingRequest(
        @NotBlank @Size(max = 120) String foodType,
        @Min(1) int quantityServings,
        @NotEmpty @Size(max = 5, message = "at most 5 images per listing") List<String> imageUrls,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotBlank @Size(max = 255) String pickupAddress,
        @NotNull Instant preparedAt,
        @NotNull @Future(message = "expiryAt must be in the future") Instant expiryAt
) {
}
