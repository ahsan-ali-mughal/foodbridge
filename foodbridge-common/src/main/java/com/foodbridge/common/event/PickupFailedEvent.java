package com.foodbridge.common.event;

/**
 * Published by logistics-service when a scheduled pickup could not be completed.
 *
 * @param claimId claim whose pickup failed
 * @param reason  short human-readable failure reason
 */
public record PickupFailedEvent(Long claimId, String reason) {
}
