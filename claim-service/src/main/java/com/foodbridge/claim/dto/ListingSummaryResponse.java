package com.foodbridge.claim.dto;

/**
 * Minimal subset of listing-service's ListingResponse that claim-service
 * needs to validate a claim attempt (status + expiry check).
 */
public record ListingSummaryResponse(String id, Long donorId, String status, String expiryAt) {
}
