package com.foodbridge.listing.service.impl;

import com.foodbridge.common.enums.ListingStatus;
import com.foodbridge.common.enums.Role;
import com.foodbridge.common.event.DonationCreatedEvent;
import com.foodbridge.common.event.DonationExpiringSoonEvent;
import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.common.event.EventType;
import com.foodbridge.common.exception.ConflictException;
import com.foodbridge.common.exception.ForbiddenException;
import com.foodbridge.common.exception.ResourceNotFoundException;
import com.foodbridge.listing.client.UserServiceClient;
import com.foodbridge.listing.document.Listing;
import com.foodbridge.listing.dto.CreateListingRequest;
import com.foodbridge.listing.dto.ListingResponse;
import com.foodbridge.listing.dto.NearbyListingsQuery;
import com.foodbridge.listing.dto.UserProfileResponse;
import com.foodbridge.listing.repository.ListingRepository;
import com.foodbridge.listing.service.ListingService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@SuppressWarnings("rawtypes")
public class ListingServiceImpl implements ListingService {

    private static final String TOPIC_DONATION_CREATED = "donation.created";
    private static final String TOPIC_DONATION_EXPIRING_SOON = "donation.expiring-soon";
    private static final long EXPIRY_WARNING_WINDOW_MINUTES = 60;

    private final ListingRepository listingRepository;
    private final UserServiceClient userServiceClient;
    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;

    public ListingServiceImpl(ListingRepository listingRepository,
                               UserServiceClient userServiceClient,
                               KafkaTemplate<String, EventEnvelope> kafkaTemplate) {
        this.listingRepository = listingRepository;
        this.userServiceClient = userServiceClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public ListingResponse createListing(Long donorId, CreateListingRequest request) {
        log.info("Creating listing for donorId={} foodType={} servings={}",
                donorId, request.foodType(), request.quantityServings());

        UserProfileResponse donor = userServiceClient.getUser(donorId);
        if (!Role.DONOR.name().equals(donor.role()) && !Role.ADMIN.name().equals(donor.role())) {
            throw new ForbiddenException("Only donor accounts may create listings");
        }

        Listing listing = Listing.newListing(
                donorId, request.foodType(), request.quantityServings(), request.imageUrls(),
                request.latitude(), request.longitude(), request.pickupAddress(),
                request.preparedAt(), request.expiryAt());

        Listing saved = listingRepository.save(listing);
        publishDonationCreated(saved);

        log.info("Created listing id={} donorId={} expiryAt={}", saved.getId(), donorId, saved.getExpiryAt());
        return toResponse(saved);
    }

    @Override
    public ListingResponse getById(String listingId) {
        return listingRepository.findById(listingId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));
    }

    @Override
    public Page<ListingResponse> getByDonor(Long donorId, Pageable pageable) {
        return listingRepository.findByDonorId(donorId, pageable).map(this::toResponse);
    }

    @Override
    public List<ListingResponse> findNearby(NearbyListingsQuery query) {
        Point point = new Point(query.longitude(), query.latitude());
        Distance distance = new Distance(query.radiusKmOrDefault(), Metrics.KILOMETERS);
        var results = listingRepository.findByLocationNearAndStatus(point, distance, ListingStatus.ACTIVE);
        log.info("Nearby search at ({}, {}) radiusKm={} returned {} listings",
                query.latitude(), query.longitude(), query.radiusKmOrDefault(), results.getContent().size());
        return results.getContent().stream()
                .map(r -> toResponse(r.getContent()))
                .toList();
    }

    @Override
    public void markClaimed(String listingId, Long ngoId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId));

        if (!listing.isClaimable()) {
            throw new ConflictException("LISTING_NOT_CLAIMABLE",
                    "Listing %s is not in a claimable state (current status: %s)".formatted(listingId, listing.getStatus()));
        }

        listing.setStatus(ListingStatus.CLAIMED);
        listing.setClaimedByNgoId(String.valueOf(ngoId));
        listingRepository.save(listing);
        log.info("Marked listing id={} as CLAIMED by ngoId={}", listingId, ngoId);
    }

    @Override
    public void expireStaleListings() {
        Instant now = Instant.now();

        List<Listing> expired = listingRepository.findByStatusAndExpiryAtBefore(ListingStatus.ACTIVE, now);
        expired.forEach(listing -> listing.setStatus(ListingStatus.EXPIRED));
        if (!expired.isEmpty()) {
            listingRepository.saveAll(expired);
            log.info("Expired {} stale listings", expired.size());
        }

        Instant warningWindowEnd = now.plus(EXPIRY_WARNING_WINDOW_MINUTES, ChronoUnit.MINUTES);
        List<Listing> expiringSoon = listingRepository.findByStatusAndExpiryAtBetween(
                ListingStatus.ACTIVE, now, warningWindowEnd);
        expiringSoon.forEach(this::publishExpiringSoon);
    }

    private void publishDonationCreated(Listing listing) {
        String correlationId = MDC.get("correlationId");
        var payload = new DonationCreatedEvent(
                listing.getId(), listing.getDonorId(), listing.getFoodType(), listing.getQuantityServings(),
                BigDecimal.valueOf(listing.getLocation().getY()), BigDecimal.valueOf(listing.getLocation().getX()),
                listing.getExpiryAt());
        var envelope = EventEnvelope.of(EventType.DONATION_CREATED, correlationId, payload);
        kafkaTemplate.send(TOPIC_DONATION_CREATED, listing.getId(), envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish donation.created for listingId={}", listing.getId(), ex);
                    } else {
                        log.debug("Published donation.created for listingId={}", listing.getId());
                    }
                });
    }

    private void publishExpiringSoon(Listing listing) {
        long minutesRemaining = ChronoUnit.MINUTES.between(Instant.now(), listing.getExpiryAt());
        var payload = new DonationExpiringSoonEvent(listing.getId(), minutesRemaining);
        var envelope = EventEnvelope.of(EventType.DONATION_EXPIRING_SOON, MDC.get("correlationId"), payload);
        kafkaTemplate.send(TOPIC_DONATION_EXPIRING_SOON, listing.getId(), envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish donation.expiring-soon for listingId={}", listing.getId(), ex);
                    }
                });
    }

    private ListingResponse toResponse(Listing listing) {
        return new ListingResponse(
                listing.getId(), listing.getDonorId(), listing.getFoodType(), listing.getQuantityServings(),
                listing.getImageUrls(), listing.getLocation().getY(), listing.getLocation().getX(),
                listing.getPickupAddress(), listing.getStatus(), listing.getPreparedAt(), listing.getExpiryAt(),
                listing.getCreatedAt());
    }
}
