package com.foodbridge.notification.listener;

import com.foodbridge.common.enums.NotificationChannel;
import com.foodbridge.common.event.DonationMatchedEvent;
import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.notification.document.NotificationLog;
import com.foodbridge.notification.repository.NotificationLogRepository;
import com.foodbridge.notification.service.SnsPublisherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * New listing matched to nearby NGOs — time-critical, so it goes through
 * SNS fan-out rather than the SQS job queue.
 */
@Component
@Slf4j
public class DonationMatchedListener {

    private final SnsPublisherService snsPublisherService;
    private final NotificationLogRepository notificationLogRepository;

    public DonationMatchedListener(SnsPublisherService snsPublisherService,
                                    NotificationLogRepository notificationLogRepository) {
        this.snsPublisherService = snsPublisherService;
        this.notificationLogRepository = notificationLogRepository;
    }

    @KafkaListener(topics = "donation.matched", groupId = "notification-service")
    public void onDonationMatched(ConsumerRecord<String, EventEnvelope<DonationMatchedEvent>> record,
                                   Acknowledgment acknowledgment) {
        EventEnvelope<DonationMatchedEvent> envelope = record.value();
        MDC.put("correlationId", envelope.correlationId());
        try {
            DonationMatchedEvent event = envelope.payload();
            String message = "A new food donation is available near you (listing %s). Open FoodBridge to claim it."
                    .formatted(event.listingId());

            String messageId = snsPublisherService.publishUrgentAlert("New donation nearby", message);
            notificationLogRepository.save(NotificationLog.of(
                    NotificationChannel.PUSH, "NGO", String.join(",", event.ngoIds().stream().map(String::valueOf).toList()),
                    "New donation nearby", "DONATION_MATCHED", NotificationLog.NotificationOutcome.SENT));

            log.info("Notified {} matched NGOs for listingId={} via SNS messageId={}",
                    event.ngoIds().size(), event.listingId(), messageId);
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process donation.matched notification, will retry on redelivery", ex);
            throw ex;
        } finally {
            MDC.clear();
        }
    }
}
