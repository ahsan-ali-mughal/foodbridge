package com.foodbridge.matching.service;

import com.foodbridge.matching.dto.UpdateNgoLocationRequest;

public interface MatchingService {

    void ensureNgoProfileExists(Long ngoId);

    void updateNgoLocation(Long ngoId, UpdateNgoLocationRequest request);

    void matchListing(String listingId, double latitude, double longitude);
}
