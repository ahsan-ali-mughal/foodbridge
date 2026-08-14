package com.foodbridge.common.event;

/**
 * Published by a scheduled job in listing-service when a listing is approaching its expiry window.
 *
 * @param listingId        listing about to expire
 * @param minutesRemaining minutes left before the pickup window closes
 */
public record DonationExpiringSoonEvent(String listingId, long minutesRemaining) {
}
