package com.foodbridge.notification.listener;

import com.foodbridge.common.event.DonationExpiringSoonEvent;
import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.notification.service.SnsPublisherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DonationExpiringSoonListener {

    private final SnsPublisherService snsPublisherService;

    public DonationExpiringSoonListener(SnsPublisherService snsPublisherService) {
        this.snsPublisherService = snsPublisherService;
    }

    @KafkaListener(topics = "donation.expiring-soon", groupId = "notification-service")
    public void onExpiringSoon(ConsumerRecord<String, EventEnvelope<DonationExpiringSoonEvent>> record,
                                Acknowledgment acknowledgment) {
        EventEnvelope<DonationExpiringSoonEvent> envelope = record.value();
        MDC.put("correlationId", envelope.correlationId());
        try {
            DonationExpiringSoonEvent event = envelope.payload();
            String message = "Listing %s expires in %d minutes and hasn't been claimed yet."
                    .formatted(event.listingId(), event.minutesRemaining());
            String messageId = snsPublisherService.publishUrgentAlert("Donation expiring soon", message);
            log.info("Published expiry warning for listingId={} messageId={}", event.listingId(), messageId);
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process donation.expiring-soon notification, will retry on redelivery", ex);
            throw ex;
        } finally {
            MDC.clear();
        }
    }
}
