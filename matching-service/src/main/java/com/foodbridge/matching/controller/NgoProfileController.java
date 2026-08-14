package com.foodbridge.matching.controller;

import com.foodbridge.matching.dto.UpdateNgoLocationRequest;
import com.foodbridge.matching.service.MatchingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matching/ngo-profile")
public class NgoProfileController {

    private final MatchingService matchingService;

    public NgoProfileController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PutMapping("/{ngoId}/location")
    public ResponseEntity<Void> updateLocation(@PathVariable Long ngoId,
                                                 @Valid @RequestBody UpdateNgoLocationRequest request) {
        matchingService.updateNgoLocation(ngoId, request);
        return ResponseEntity.noContent().build();
    }
}
