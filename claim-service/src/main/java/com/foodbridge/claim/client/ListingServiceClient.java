package com.foodbridge.claim.client;

import com.foodbridge.claim.dto.ListingSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "listing-service", path = "/api/listings")
public interface ListingServiceClient {

    @GetMapping("/{listingId}")
    ListingSummaryResponse getListing(@PathVariable("listingId") String listingId);

    @PostMapping("/{listingId}/claimed")
    void markClaimed(@PathVariable("listingId") String listingId);
}
