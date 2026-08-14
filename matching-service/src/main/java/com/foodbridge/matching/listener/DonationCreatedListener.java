package com.foodbridge.matching.listener;

import com.foodbridge.common.event.DonationCreatedEvent;
import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.matching.service.MatchingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DonationCreatedListener {

    private final MatchingService matchingService;

    public DonationCreatedListener(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @KafkaListener(topics = "donation.created", groupId = "matching-service")
    public void onDonationCreated(ConsumerRecord<String, EventEnvelope<DonationCreatedEvent>> record,
                                   Acknowledgment acknowledgment) {
        EventEnvelope<DonationCreatedEvent> envelope = record.value();
        MDC.put("correlationId", envelope.correlationId());
        try {
            DonationCreatedEvent event = envelope.payload();
            log.info("Received donation.created for listingId={}", event.listingId());
            matchingService.matchListing(event.listingId(), event.latitude().doubleValue(), event.longitude().doubleValue());
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process donation.created event, will retry on redelivery", ex);
            throw ex;
        } finally {
            MDC.clear();
        }
    }
}
