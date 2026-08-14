package com.foodbridge.claim.service.impl;

import com.foodbridge.claim.client.ListingServiceClient;
import com.foodbridge.claim.dto.ClaimResponse;
import com.foodbridge.claim.dto.ListingSummaryResponse;
import com.foodbridge.claim.entity.Claim;
import com.foodbridge.claim.repository.ClaimRepository;
import com.foodbridge.claim.service.ClaimService;
import com.foodbridge.common.enums.ClaimStatus;
import com.foodbridge.common.event.DonationClaimedEvent;
import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.common.event.EventType;
import com.foodbridge.common.exception.ConflictException;
import com.foodbridge.common.exception.ForbiddenException;
import com.foodbridge.common.exception.ResourceNotFoundException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Core of the platform's consistency guarantee: exactly one NGO may claim a
 * given listing. Concurrency control has two independent layers:
 *
 * <ol>
 *   <li><b>Redisson distributed lock</b>, keyed per listing, held only for the
 *       duration of the check-then-act claim sequence. This is the primary,
 *       cheap defense — it turns a race into a queue of one at a time.</li>
 *   <li><b>MySQL unique constraint</b> on {@code claims.listing_id}. This is
 *       the safety net for the rare case the lock is bypassed (e.g. a node
 *       crash between acquiring the lock and its TTL-based auto-release, or
 *       a Redis failover window). A constraint violation here is treated as
 *       an ordinary "already claimed" conflict, not a bug.</li>
 * </ol>
 */
@Service
@Slf4j
@SuppressWarnings("rawtypes")
public class ClaimServiceImpl implements ClaimService {

    private static final String TOPIC_DONATION_CLAIMED = "donation.claimed";
    private static final String LOCK_KEY_PREFIX = "lock:listing:";
    private static final long LOCK_WAIT_SECONDS = 2;
    private static final long LOCK_LEASE_SECONDS = 5;

    private final ClaimRepository claimRepository;
    private final ListingServiceClient listingServiceClient;
    private final RedissonClient redissonClient;
    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;

    public ClaimServiceImpl(ClaimRepository claimRepository,
                             ListingServiceClient listingServiceClient,
                             RedissonClient redissonClient,
                             KafkaTemplate<String, EventEnvelope> kafkaTemplate) {
        this.claimRepository = claimRepository;
        this.listingServiceClient = listingServiceClient;
        this.redissonClient = redissonClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public ClaimResponse claimListing(String listingId, Long ngoId) {
        log.info("NGO ngoId={} attempting to claim listingId={}", ngoId, listingId);

        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + listingId);
        boolean acquired;
        try {
            acquired = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ConflictException("CLAIM_INTERRUPTED", "Claim attempt was interrupted, please retry");
        }

        if (!acquired) {
            log.warn("Failed to acquire claim lock for listingId={} (contended)", listingId);
            throw new ConflictException("LISTING_CONTENDED",
                    "Another NGO is claiming this listing right now, please try again");
        }

        try {
            return doClaim(listingId, ngoId);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    protected ClaimResponse doClaim(String listingId, Long ngoId) {
        validateListingIsClaimable(listingId);

        if (claimRepository.existsByListingId(listingId)) {
            log.warn("Claim rejected: listingId={} already has a claim", listingId);
            throw new ConflictException("ALREADY_CLAIMED", "This listing has already been claimed");
        }

        Claim claim;
        try {
            claim = claimRepository.saveAndFlush(Claim.newClaim(listingId, ngoId));
        } catch (DataIntegrityViolationException ex) {
            // Safety-net branch: the unique constraint caught a race the lock didn't.
            log.warn("Unique constraint caught a duplicate claim for listingId={}", listingId);
            throw new ConflictException("ALREADY_CLAIMED", "This listing has already been claimed");
        }

        notifyListingServiceClaimed(listingId);
        publishDonationClaimed(claim);

        log.info("Claim succeeded: claimId={} listingId={} ngoId={}", claim.getId(), listingId, ngoId);
        return toResponse(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimResponse getById(Long claimId) {
        return claimRepository.findById(claimId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", claimId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClaimResponse> getByNgo(Long ngoId, Pageable pageable) {
        return claimRepository.findByNgoIdAndStatus(ngoId, ClaimStatus.CLAIMED, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public void completeClaim(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", claimId));
        claim.markCompleted();
        claimRepository.save(claim);
        log.info("Claim marked COMPLETED: claimId={}", claimId);
    }

    @Override
    @Transactional
    public void cancelClaim(Long claimId, Long requestingNgoId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", claimId));

        if (!claim.getNgoId().equals(requestingNgoId)) {
            log.warn("NGO ngoId={} attempted to cancel a claim owned by ngoId={}", requestingNgoId, claim.getNgoId());
            throw new ForbiddenException("You may only cancel your own claims");
        }

        claim.markCancelled();
        claimRepository.save(claim);
        log.info("Claim cancelled: claimId={} by ngoId={}", claimId, requestingNgoId);
    }

    private void validateListingIsClaimable(String listingId) {
        ListingSummaryResponse listing;
        try {
            listing = listingServiceClient.getListing(listingId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Listing", listingId);
        }

        if (!"ACTIVE".equals(listing.status()) && !"MATCHED".equals(listing.status())) {
            throw new ConflictException("LISTING_NOT_CLAIMABLE",
                    "Listing is not currently claimable (status: %s)".formatted(listing.status()));
        }

        Instant expiryAt = Instant.parse(listing.expiryAt());
        if (Instant.now().isAfter(expiryAt)) {
            throw new ConflictException("LISTING_EXPIRED", "This listing's pickup window has already passed");
        }
    }

    private void notifyListingServiceClaimed(String listingId) {
        try {
            listingServiceClient.markClaimed(listingId);
        } catch (FeignException ex) {
            // The claim record is already committed at this point — listing-service's status
            // will briefly lag. This is intentionally tolerated and reconciled by the
            // donation.claimed consumer in listing-service, so we log rather than roll back.
            log.error("Failed to notify listing-service of claim for listingId={}", listingId, ex);
        }
    }

    private void publishDonationClaimed(Claim claim) {
        String correlationId = MDC.get("correlationId");
        var payload = new DonationClaimedEvent(claim.getId(), claim.getListingId(), claim.getNgoId(), claim.getClaimedAt());
        var envelope = EventEnvelope.of(EventType.DONATION_CLAIMED, correlationId, payload);
        kafkaTemplate.send(TOPIC_DONATION_CLAIMED, claim.getListingId(), envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish donation.claimed for claimId={}", claim.getId(), ex);
                    } else {
                        log.debug("Published donation.claimed for claimId={}", claim.getId());
                    }
                });
    }

    private ClaimResponse toResponse(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getListingId(), claim.getNgoId(),
                claim.getStatus(), claim.getClaimedAt(), claim.getCompletedAt());
    }
}
