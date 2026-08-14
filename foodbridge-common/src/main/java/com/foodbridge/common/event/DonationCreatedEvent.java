package com.foodbridge.common.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published by listing-service whenever a new food donation listing is created.
 *
 * @param listingId        Mongo document id of the listing
 * @param donorId          id of the donor organization (owned by auth-service/user-service)
 * @param foodType         free-text category, e.g. "Cooked meals"
 * @param quantityServings estimated number of servings
 * @param latitude         pickup location latitude
 * @param longitude        pickup location longitude
 * @param expiryAt         last safe pickup time
 */
public record DonationCreatedEvent(
        String listingId,
        Long donorId,
        String foodType,
        int quantityServings,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant expiryAt
) {
}
