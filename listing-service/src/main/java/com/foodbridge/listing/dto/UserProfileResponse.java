package com.foodbridge.listing.dto;

/**
 * Mirrors auth-service's UserResponse shape for the subset of fields
 * listing-service needs when validating a donor via Feign.
 */
public record UserProfileResponse(Long id, String email, String role, String verificationStatus) {
}
