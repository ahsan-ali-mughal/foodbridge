package com.foodbridge.logistics.listener;

import com.foodbridge.common.event.DonationClaimedEvent;
import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.logistics.service.LogisticsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Creates an unassigned pickup task whenever a listing is successfully
 * claimed. {@code createTaskForClaim} is idempotent (checks for an existing
 * task by claimId first), which matters because Kafka only guarantees
 * at-least-once delivery.
 */
@Component
@Slf4j
public class DonationClaimedListener {

    private final LogisticsService logisticsService;

    public DonationClaimedListener(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    @KafkaListener(topics = "donation.claimed", groupId = "logistics-service")
    public void onDonationClaimed(ConsumerRecord<String, EventEnvelope<DonationClaimedEvent>> record,
                                   Acknowledgment acknowledgment) {
        EventEnvelope<DonationClaimedEvent> envelope = record.value();
        MDC.put("correlationId", envelope.correlationId());
        try {
            DonationClaimedEvent event = envelope.payload();
            log.info("Received donation.claimed for claimId={} listingId={}", event.claimId(), event.listingId());
            logisticsService.createTaskForClaim(event.claimId(), event.listingId());
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process donation.claimed event, will retry on redelivery", ex);
            throw ex;
        } finally {
            MDC.clear();
        }
    }
}
