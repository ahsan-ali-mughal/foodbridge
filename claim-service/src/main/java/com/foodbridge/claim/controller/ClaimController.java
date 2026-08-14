package com.foodbridge.claim.controller;

import com.foodbridge.claim.dto.ClaimResponse;
import com.foodbridge.claim.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claims")
@Tag(name = "Claims", description = "NGO claim workflow for food donation listings")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping("/{listingId}")
    @Operation(summary = "Claim a listing on behalf of the authenticated NGO (concurrency-safe)")
    public ResponseEntity<ClaimResponse> claim(@PathVariable String listingId, Authentication authentication) {
        Long ngoId = (Long) authentication.getPrincipal();
        ClaimResponse response = claimService.claimListing(listingId, ngoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{claimId}")
    @Operation(summary = "Fetch a single claim by id")
    public ResponseEntity<ClaimResponse> getById(@PathVariable Long claimId) {
        return ResponseEntity.ok(claimService.getById(claimId));
    }

    @GetMapping("/mine")
    @Operation(summary = "List the current NGO's active claims, paginated")
    public ResponseEntity<Page<ClaimResponse>> mine(Pageable pageable, Authentication authentication) {
        Long ngoId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(claimService.getByNgo(ngoId, pageable));
    }

    @DeleteMapping("/{claimId}")
    @Operation(summary = "Cancel a claim owned by the authenticated NGO")
    public ResponseEntity<Void> cancel(@PathVariable Long claimId, Authentication authentication) {
        Long ngoId = (Long) authentication.getPrincipal();
        claimService.cancelClaim(claimId, ngoId);
        return ResponseEntity.noContent().build();
    }
}
