package com.foodbridge.document.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "listing-service", path = "/api/listings")
public interface ListingServiceClient {

    @GetMapping("/{listingId}")
    ListingSummary getListing(@PathVariable("listingId") String listingId);

    record ListingSummary(String id, Long donorId, String foodType) {
    }
}
