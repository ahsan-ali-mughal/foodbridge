package com.foodbridge.common.event;

import java.time.Instant;

/**
 * Published by logistics-service when a volunteer marks a delivery as complete.
 * Triggers receipt generation in document-service.
 *
 * @param claimId     claim this pickup fulfills
 * @param listingId   originating listing
 * @param volunteerId volunteer who performed the delivery
 * @param deliveredAt completion timestamp
 */
public record PickupCompletedEvent(Long claimId, String listingId, Long volunteerId, Instant deliveredAt) {
}
