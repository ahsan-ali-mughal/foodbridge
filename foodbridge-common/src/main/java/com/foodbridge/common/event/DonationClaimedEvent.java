package com.foodbridge.common.event;

import java.time.Instant;

/**
 * Published by claim-service once an NGO successfully claims a listing.
 *
 * @param claimId   relational id of the claim record
 * @param listingId listing that was claimed
 * @param ngoId     NGO that claimed it
 * @param claimedAt timestamp of the successful claim
 */
public record DonationClaimedEvent(Long claimId, String listingId, Long ngoId, Instant claimedAt) {
}
