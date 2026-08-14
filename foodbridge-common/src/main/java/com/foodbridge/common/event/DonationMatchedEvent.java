package com.foodbridge.common.event;

import java.util.List;

/**
 * Published by matching-service once eligible NGOs have been computed for a listing.
 *
 * @param listingId listing that was matched
 * @param ngoIds    ids of NGOs eligible to claim, ordered by proximity
 */
public record DonationMatchedEvent(String listingId, List<Long> ngoIds) {
}
