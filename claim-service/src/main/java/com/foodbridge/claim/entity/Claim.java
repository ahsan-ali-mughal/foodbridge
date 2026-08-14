package com.foodbridge.claim.entity;

import com.foodbridge.common.enums.ClaimStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A claim ties one NGO to one listing. The unique constraint on
 * {@code listing_id} is the database-level second line of defense against
 * a double-claim, complementing the Redisson distributed lock that guards
 * the claim operation in application code (belt & suspenders — the lock
 * handles the common case cheaply, the constraint catches any edge case
 * where the lock is bypassed, e.g. a node crash mid-lock).
 */
@Entity
@Table(name = "claims",
        uniqueConstraints = @UniqueConstraint(name = "uq_claims_listing_id", columnNames = "listing_id"),
        indexes = {
                @Index(name = "idx_claims_ngo_id", columnList = "ngo_id"),
                @Index(name = "idx_claims_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false, length = 36)
    private String listingId;

    @Column(name = "ngo_id", nullable = false)
    private Long ngoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimStatus status;

    @Column(nullable = false)
    private Instant claimedAt;

    private Instant completedAt;

    private Instant cancelledAt;

    private Instant updatedAt;

    public static Claim newClaim(String listingId, Long ngoId) {
        Claim claim = new Claim();
        claim.listingId = listingId;
        claim.ngoId = ngoId;
        claim.status = ClaimStatus.CLAIMED;
        claim.claimedAt = Instant.now();
        return claim;
    }

    public void markCompleted() {
        this.status = ClaimStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markCancelled() {
        this.status = ClaimStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
