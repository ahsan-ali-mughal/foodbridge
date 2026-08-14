package com.foodbridge.notification.listener;

import com.foodbridge.common.event.DonationClaimedEvent;
import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.notification.service.SqsJobEnqueuer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DonationClaimedListener {

    private final SqsJobEnqueuer sqsJobEnqueuer;

    public DonationClaimedListener(SqsJobEnqueuer sqsJobEnqueuer) {
        this.sqsJobEnqueuer = sqsJobEnqueuer;
    }

    @KafkaListener(topics = "donation.claimed", groupId = "notification-service")
    public void onDonationClaimed(ConsumerRecord<String, EventEnvelope<DonationClaimedEvent>> record,
                                   Acknowledgment acknowledgment) {
        EventEnvelope<DonationClaimedEvent> envelope = record.value();
        MDC.put("correlationId", envelope.correlationId());
        try {
            DonationClaimedEvent event = envelope.payload();
            sqsJobEnqueuer.enqueue("CLAIM_CONFIRMATION", String.valueOf(event.ngoId()), event.listingId());
            log.info("Enqueued claim confirmation job for claimId={}", event.claimId());
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process donation.claimed notification, will retry on redelivery", ex);
            throw ex;
        } finally {
            MDC.clear();
        }
    }
}
