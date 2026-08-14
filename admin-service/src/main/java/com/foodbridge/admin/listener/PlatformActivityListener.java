package com.foodbridge.admin.listener;

import com.foodbridge.admin.service.DashboardService;
import com.foodbridge.common.event.DonationClaimedEvent;
import com.foodbridge.common.event.DonationCreatedEvent;
import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.common.event.PickupCompletedEvent;
import com.foodbridge.common.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes the platform's key domain events purely to build the admin
 * dashboard's activity feed and summary counters (see {@code ActivityEvent}).
 * Deliberately one listener class per topic rather than one class handling
 * all four — each stays small, testable, and independently restartable if
 * one topic's consumer group needs to be reset.
 */
@Component
@Slf4j
public class PlatformActivityListener {

    private final DashboardService dashboardService;

    public PlatformActivityListener(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @KafkaListener(topics = "donation.created", groupId = "admin-service")
    public void onDonationCreated(ConsumerRecord<String, EventEnvelope<DonationCreatedEvent>> record,
                                   Acknowledgment acknowledgment) {
        withCorrelation(record.value().correlationId(), () -> {
            DonationCreatedEvent event = record.value().payload();
            dashboardService.recordActivity("DONATION_CREATED",
                    "New listing %s created by donor %d (%s, %d servings)"
                            .formatted(event.listingId(), event.donorId(), event.foodType(), event.quantityServings()));
        });
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "donation.claimed", groupId = "admin-service")
    public void onDonationClaimed(ConsumerRecord<String, EventEnvelope<DonationClaimedEvent>> record,
                                   Acknowledgment acknowledgment) {
        withCorrelation(record.value().correlationId(), () -> {
            DonationClaimedEvent event = record.value().payload();
            dashboardService.recordActivity("DONATION_CLAIMED",
                    "Listing %s claimed by NGO %d".formatted(event.listingId(), event.ngoId()));
        });
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "pickup.completed", groupId = "admin-service")
    public void onPickupCompleted(ConsumerRecord<String, EventEnvelope<PickupCompletedEvent>> record,
                                   Acknowledgment acknowledgment) {
        withCorrelation(record.value().correlationId(), () -> {
            PickupCompletedEvent event = record.value().payload();
            dashboardService.recordActivity("PICKUP_COMPLETED",
                    "Delivery completed for listing %s by volunteer %d".formatted(event.listingId(), event.volunteerId()));
        });
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "user.registered", groupId = "admin-service")
    public void onUserRegistered(ConsumerRecord<String, EventEnvelope<UserRegisteredEvent>> record,
                                  Acknowledgment acknowledgment) {
        withCorrelation(record.value().correlationId(), () -> {
            UserRegisteredEvent event = record.value().payload();
            dashboardService.recordActivity("USER_REGISTERED",
                    "New %s account registered: %s".formatted(event.role(), event.email()));
        });
        acknowledgment.acknowledge();
    }

    private void withCorrelation(String correlationId, Runnable action) {
        MDC.put("correlationId", correlationId);
        try {
            action.run();
        } catch (Exception ex) {
            log.error("Failed to record activity event", ex);
            throw ex;
        } finally {
            MDC.clear();
        }
    }
}
