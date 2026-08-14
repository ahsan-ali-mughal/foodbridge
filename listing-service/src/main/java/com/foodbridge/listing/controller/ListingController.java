package com.foodbridge.listing.controller;

import com.foodbridge.listing.dto.CreateListingRequest;
import com.foodbridge.listing.dto.ListingResponse;
import com.foodbridge.listing.dto.NearbyListingsQuery;
import com.foodbridge.listing.dto.PresignedUploadRequest;
import com.foodbridge.listing.dto.PresignedUploadResponse;
import com.foodbridge.listing.service.ListingService;
import com.foodbridge.listing.service.S3StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/listings")
@Tag(name = "Listings", description = "Create and discover food donation listings")
@Slf4j
public class ListingController {

    private final ListingService listingService;
    private final S3StorageService s3StorageService;

    public ListingController(ListingService listingService, S3StorageService s3StorageService) {
        this.listingService = listingService;
        this.s3StorageService = s3StorageService;
    }

    @PostMapping
    @Operation(summary = "Create a new food donation listing")
    public ResponseEntity<ListingResponse> create(@Valid @RequestBody CreateListingRequest request,
                                                    Authentication authentication) {
        Long donorId = (Long) authentication.getPrincipal();
        ListingResponse response = listingService.createListing(donorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/upload-url")
    @Operation(summary = "Obtain a pre-signed S3 URL to upload a listing photo directly from the browser")
    public ResponseEntity<PresignedUploadResponse> uploadUrl(@Valid @RequestBody PresignedUploadRequest request,
                                                                Authentication authentication) {
        String ownerId = String.valueOf(authentication.getPrincipal());
        return ResponseEntity.ok(s3StorageService.createPresignedUploadUrl(ownerId, request.fileName(), request.contentType()));
    }

    @GetMapping("/{listingId}")
    @Operation(summary = "Fetch a single listing by id")
    public ResponseEntity<ListingResponse> getById(@PathVariable String listingId) {
        return ResponseEntity.ok(listingService.getById(listingId));
    }

    @PostMapping("/{listingId}/claimed")
    @Operation(summary = "Internal: mark a listing as claimed (called by claim-service after a successful claim)")
    public ResponseEntity<Void> markClaimed(@PathVariable String listingId, Authentication authentication) {
        Long ngoId = (Long) authentication.getPrincipal();
        listingService.markClaimed(listingId, ngoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mine")
    @Operation(summary = "List the current donor's own listings, paginated")
    public ResponseEntity<Page<ListingResponse>> mine(Pageable pageable, Authentication authentication) {
        Long donorId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(listingService.getByDonor(donorId, pageable));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find active listings within a radius of a given point (NGO discovery)")
    public ResponseEntity<List<ListingResponse>> nearby(@RequestParam Double latitude,
                                                          @RequestParam Double longitude,
                                                          @RequestParam(required = false) Double radiusKm) {
        var query = new NearbyListingsQuery(latitude, longitude, radiusKm);
        return ResponseEntity.ok(listingService.findNearby(query));
    }
}
