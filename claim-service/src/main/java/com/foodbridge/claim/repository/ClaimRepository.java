package com.foodbridge.claim.repository;

import com.foodbridge.claim.entity.Claim;
import com.foodbridge.common.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByListingId(String listingId);

    boolean existsByListingId(String listingId);

    Page<Claim> findByNgoIdAndStatus(Long ngoId, ClaimStatus status, Pageable pageable);
}
