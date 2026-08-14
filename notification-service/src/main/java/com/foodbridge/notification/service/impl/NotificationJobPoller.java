package com.foodbridge.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodbridge.notification.document.NotificationLog;
import com.foodbridge.notification.repository.NotificationLogRepository;
import com.foodbridge.common.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.Map;

/**
 * Polls {@code notification-job-queue} for non-urgent notification jobs
 * (welcome emails, claim confirmations, weekly digests) and "sends" them
 * (simulated here — wire in a real email/SMS provider in production).
 *
 * <p>Invoked by {@code NotificationJobPollerJob}, a Quartz job backed by a
 * dedicated MySQL JobStore (see the {@code quartz} package), rather than a
 * Spring {@code @Scheduled} method — this guarantees only one running
 * instance of the service polls per trigger fire if ever scaled out.</p>
 *
 * <p>Failure handling relies entirely on the queue's own redrive policy:
 * this poller deletes a message only after successful processing. An
 * unprocessed message becomes visible again after the visibility timeout
 * and is redelivered; once a message's {@code ApproximateReceiveCount}
 * exceeds the queue's {@code maxReceiveCount} (3, see
 * localstack/init-aws.sh), SQS itself moves it to
 * {@code notification-job-dlq} — no application-level DLQ logic needed.</p>
 */
@Component
@Slf4j
public class NotificationJobPoller {

    private static final int MAX_MESSAGES_PER_POLL = 10;
    private static final int WAIT_TIME_SECONDS = 5; // long polling

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;
    private final NotificationLogRepository notificationLogRepository;

    public NotificationJobPoller(SqsClient sqsClient,
                                  @Value("${aws.sqs.notification-job-queue-url}") String queueUrl,
                                  ObjectMapper objectMapper,
                                  NotificationLogRepository notificationLogRepository) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.objectMapper = objectMapper;
        this.notificationLogRepository = notificationLogRepository;
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
            log.error("Failed to poll notification-job-queue", ex);
            return;
        }

        for (Message message : response.messages()) {
            processMessage(message);
        }
    }

    @SuppressWarnings("unchecked")
    private void processMessage(Message message) {
        try {
            Map<String, String> envelope = objectMapper.readValue(message.body(), Map.class);
            String jobType = envelope.get("jobType");
            String recipientId = envelope.get("recipientId");

            log.info("Processing notification job type={} recipientId={} sqsMessageId={}",
                    jobType, recipientId, message.messageId());

            // Simulated send — in production this calls a real email/SMS provider SDK.
            sendEmail(jobType, recipientId, envelope.get("payload"));

            notificationLogRepository.save(NotificationLog.of(
                    NotificationChannel.EMAIL, "USER", recipientId, jobType, jobType,
                    NotificationLog.NotificationOutcome.SENT));

            deleteMessage(message);
        } catch (Exception ex) {
            // Deliberately do NOT delete the message on failure — let it become visible again
            // and retry, eventually landing in the DLQ via the queue's redrive policy.
            log.error("Failed to process notification job sqsMessageId={}, will retry via SQS redelivery",
                    message.messageId(), ex);
        }
    }

    private void sendEmail(String jobType, String recipientId, String payload) {
        // Placeholder for a real provider integration (SES, SendGrid, Twilio, etc).
        log.debug("Simulated send: jobType={} recipientId={} payload={}", jobType, recipientId, payload);
    }

    private void deleteMessage(Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
    }
}
