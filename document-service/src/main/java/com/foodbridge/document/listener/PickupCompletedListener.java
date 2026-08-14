package com.foodbridge.document.listener;

import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.common.event.PickupCompletedEvent;
import com.foodbridge.document.client.ListingServiceClient;
import com.foodbridge.document.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * On every completed pickup, creates a pending receipt record and enqueues
 * the actual PDF generation onto {@code receipt-generation-queue}. Splitting
 * "record the intent" (this listener, synchronous, fast) from "do the work"
 * (the SQS-driven poller, which can retry independently) keeps the Kafka
 * consumer thread from being blocked on PDF rendering or S3 I/O.
 */
@Component
@Slf4j
public class PickupCompletedListener {

    private final DocumentService documentService;
    private final ListingServiceClient listingServiceClient;

    public PickupCompletedListener(DocumentService documentService, ListingServiceClient listingServiceClient) {
        this.documentService = documentService;
        this.listingServiceClient = listingServiceClient;
    }

    @KafkaListener(topics = "pickup.completed", groupId = "document-service")
    public void onPickupCompleted(ConsumerRecord<String, EventEnvelope<PickupCompletedEvent>> record,
                                   Acknowledgment acknowledgment) {
        EventEnvelope<PickupCompletedEvent> envelope = record.value();
        MDC.put("correlationId", envelope.correlationId());
        try {
            PickupCompletedEvent event = envelope.payload();
            log.info("Received pickup.completed for claimId={} listingId={}", event.claimId(), event.listingId());

            ListingServiceClient.ListingSummary listing = listingServiceClient.getListing(event.listingId());
            documentService.createPendingReceipt(event.claimId(), event.listingId(), listing.donorId());
            documentService.enqueueReceiptGeneration(event.claimId());

            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process pickup.completed event, will retry on redelivery", ex);
            throw ex;
        } finally {
            MDC.clear();
        }
    }
}
