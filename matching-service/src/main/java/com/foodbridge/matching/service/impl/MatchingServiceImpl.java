package com.foodbridge.matching.service.impl;

import com.foodbridge.common.event.DonationMatchedEvent;
import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.common.event.EventType;
import com.foodbridge.matching.document.NgoProfile;
import com.foodbridge.matching.dto.UpdateNgoLocationRequest;
import com.foodbridge.matching.repository.NgoProfileRepository;
import com.foodbridge.matching.service.MatchingService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@SuppressWarnings("rawtypes")
public class MatchingServiceImpl implements MatchingService {

    private static final String TOPIC_DONATION_MATCHED = "donation.matched";
    private static final double DEFAULT_SEARCH_RADIUS_KM = 15.0;
    private static final int MAX_MATCHED_NGOS = 20;

    private final NgoProfileRepository ngoProfileRepository;
    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;

    public MatchingServiceImpl(NgoProfileRepository ngoProfileRepository,
                                KafkaTemplate<String, EventEnvelope> kafkaTemplate) {
        this.ngoProfileRepository = ngoProfileRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void ensureNgoProfileExists(Long ngoId) {
        if (!ngoProfileRepository.existsById(String.valueOf(ngoId))) {
            ngoProfileRepository.save(NgoProfile.stubFor(ngoId));
            log.info("Created NGO profile stub for ngoId={} (awaiting location setup)", ngoId);
        }
    }

    @Override
    public void updateNgoLocation(Long ngoId, UpdateNgoLocationRequest request) {
        NgoProfile profile = ngoProfileRepository.findById(String.valueOf(ngoId))
                .orElseGet(() -> NgoProfile.stubFor(ngoId));
        double radius = request.serviceRadiusKm() != null ? request.serviceRadiusKm() : DEFAULT_SEARCH_RADIUS_KM;
        profile.updateLocation(request.latitude(), request.longitude(), radius);
        ngoProfileRepository.save(profile);
        log.info("Updated location for ngoId={} radiusKm={}", ngoId, radius);
    }

    @Override
    public void matchListing(String listingId, double latitude, double longitude) {
        Point listingPoint = new Point(longitude, latitude);
        // Search using the platform-wide default radius; a future iteration could search per-NGO
        // radius by querying progressively wider and filtering client-side, but for the MVP a
        // single generous radius keeps the query simple and index-friendly.
        Distance searchDistance = new Distance(DEFAULT_SEARCH_RADIUS_KM, Metrics.KILOMETERS);

        var results = ngoProfileRepository.findByLocationNearAndLocationSetTrue(listingPoint, searchDistance);

        List<Long> ngoIds = results.getContent().stream()
                .map(r -> r.getContent().getNgoId())
                .limit(MAX_MATCHED_NGOS)
                .toList();

        if (ngoIds.isEmpty()) {
            log.warn("No NGOs matched for listingId={} at ({}, {}) within {}km",
                    listingId, latitude, longitude, DEFAULT_SEARCH_RADIUS_KM);
            return;
        }

        publishMatched(listingId, ngoIds);
        log.info("Matched listingId={} to {} NGOs", listingId, ngoIds.size());
    }

    private void publishMatched(String listingId, List<Long> ngoIds) {
        var payload = new DonationMatchedEvent(listingId, ngoIds);
        var envelope = EventEnvelope.of(EventType.DONATION_MATCHED, MDC.get("correlationId"), payload);
        kafkaTemplate.send(TOPIC_DONATION_MATCHED, listingId, envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish donation.matched for listingId={}", listingId, ex);
                    }
                });
    }
}
