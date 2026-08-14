package com.foodbridge.claim.service;

import com.foodbridge.claim.dto.ClaimResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClaimService {

    ClaimResponse claimListing(String listingId, Long ngoId);

    ClaimResponse getById(Long claimId);

    Page<ClaimResponse> getByNgo(Long ngoId, Pageable pageable);

    void completeClaim(Long claimId);

    void cancelClaim(Long claimId, Long requestingNgoId);
}
