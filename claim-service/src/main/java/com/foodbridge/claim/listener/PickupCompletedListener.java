package com.foodbridge.claim.listener;

import com.foodbridge.claim.service.ClaimService;
import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.common.event.PickupCompletedEvent;
import com.foodbridge.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code pickup.completed} events from logistics-service and marks
 * the corresponding claim COMPLETED. Idempotent by design: re-processing the
 * same event (at-least-once delivery) simply re-applies the same terminal
 * state, which is a safe no-op if already completed.
 */
@Component
@Slf4j
public class PickupCompletedListener {

    private final ClaimService claimService;

    public PickupCompletedListener(ClaimService claimService) {
        this.claimService = claimService;
    }

    @KafkaListener(topics = "pickup.completed", groupId = "claim-service")
    public void onPickupCompleted(ConsumerRecord<String, EventEnvelope<PickupCompletedEvent>> record,
                                   Acknowledgment acknowledgment) {
        EventEnvelope<PickupCompletedEvent> envelope = record.value();
        MDC.put("correlationId", envelope.correlationId());
        try {
            PickupCompletedEvent event = envelope.payload();
            log.info("Received pickup.completed for claimId={}", event.claimId());
            try {
                claimService.completeClaim(event.claimId());
            } catch (ResourceNotFoundException ex) {
                // Out-of-order or duplicate delivery referencing a claim we don't recognize
                // (or that was already cleaned up); log and acknowledge rather than retry forever.
                log.warn("Ignoring pickup.completed for unknown claimId={}", event.claimId());
            }
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            // Do not acknowledge on unexpected failure — the broker will redeliver, and a
            // persistently failing message will eventually be handled by the consumer's
            // configured retry/backoff and error handler (see application.yml).
            log.error("Failed to process pickup.completed event, will retry on redelivery", ex);
            throw ex;
        } finally {
            MDC.clear();
        }
    }
}
