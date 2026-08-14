package com.foodbridge.listing.service;

import com.foodbridge.listing.dto.CreateListingRequest;
import com.foodbridge.listing.dto.ListingResponse;
import com.foodbridge.listing.dto.NearbyListingsQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ListingService {

    ListingResponse createListing(Long donorId, CreateListingRequest request);

    ListingResponse getById(String listingId);

    Page<ListingResponse> getByDonor(Long donorId, Pageable pageable);

    List<ListingResponse> findNearby(NearbyListingsQuery query);

    void markClaimed(String listingId, Long ngoId);

    void expireStaleListings();
}
