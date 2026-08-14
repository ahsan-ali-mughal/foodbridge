package com.foodbridge.document.service.impl;

import com.foodbridge.document.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * Polls {@code receipt-generation-queue} and generates/uploads the
 * corresponding receipt PDF for each message. Invoked by
 * {@code ReceiptGenerationJob}, a Quartz job on a dedicated MySQL JobStore
 * (see the {@code quartz} package), not a Spring {@code @Scheduled} method.
 *
 * <p>As with notification-service's poller: a message is deleted only after
 * successful processing. On failure it is left alone, becomes visible again,
 * and is redelivered up to {@code maxReceiveCount} (3) times before SQS
 * itself routes it to {@code receipt-generation-dlq} — see
 * localstack/init-aws.sh for the redrive policy.</p>
 */
@Component
@Slf4j
public class ReceiptGenerationJobPoller {

    private static final int MAX_MESSAGES_PER_POLL = 10;
    private static final int WAIT_TIME_SECONDS = 5;

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final DocumentService documentService;

    public ReceiptGenerationJobPoller(SqsClient sqsClient,
                                       @Value("${aws.sqs.receipt-generation-queue-url}") String queueUrl,
                                       DocumentService documentService) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.documentService = documentService;
    }

    public void poll() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(MAX_MESSAGES_PER_POLL)
                .waitTimeSeconds(WAIT_TIME_SECONDS)
                .build();

        ReceiveMessageResponse response;
        try {
            response = sqsClient.receiveMessage(request);
        } catch (Exception ex) {
            log.error("Failed to poll receipt-generation-queue", ex);
            return;
        }

        for (Message message : response.messages()) {
            processMessage(message);
        }
    }

    private void processMessage(Message message) {
        try {
            Long claimId = Long.valueOf(message.body().trim());
            documentService.processReceiptGenerationJob(claimId);
            deleteMessage(message);
        } catch (Exception ex) {
            log.error("Failed to process receipt generation job sqsMessageId={}, will retry via SQS redelivery",
                    message.messageId(), ex);
        }
    }

    private void deleteMessage(Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
    }
}
