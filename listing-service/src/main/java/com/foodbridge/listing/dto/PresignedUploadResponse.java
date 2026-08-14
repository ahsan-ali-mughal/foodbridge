package com.foodbridge.listing.dto;

public record PresignedUploadResponse(String uploadUrl, String objectUrl, long expiresInSeconds) {
}
