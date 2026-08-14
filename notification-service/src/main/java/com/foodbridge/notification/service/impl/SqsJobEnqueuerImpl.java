package com.foodbridge.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodbridge.notification.service.SqsJobEnqueuer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.HashMap;
import java.util.Map;

/**
 * Enqueues non-urgent notification jobs onto {@code notification-job-queue}.
 * The queue's redrive policy (configured in LocalStack/AWS, see
 * localstack/init-aws.sh) routes a message to
 * {@code notification-job-dlq} after 3 failed processing attempts by
 * {@code NotificationJobPoller}.
 */
@Service
@Slf4j
public class SqsJobEnqueuerImpl implements SqsJobEnqueuer {

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    public SqsJobEnqueuerImpl(SqsClient sqsClient,
                               @Value("${aws.sqs.notification-job-queue-url}") String queueUrl,
                               ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enqueue(String jobType, String recipientId, String payloadJson) {
        try {
            Map<String, String> envelope = new HashMap<>();
            envelope.put("jobType", jobType);
            envelope.put("recipientId", recipientId);
            envelope.put("payload", payloadJson);
            String body = objectMapper.writeValueAsString(envelope);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(body)
                    .build();
            SendMessageResponse response = sqsClient.sendMessage(request);
            log.info("Enqueued notification job type={} recipientId={} messageId={}",
                    jobType, recipientId, response.messageId());
        } catch (SqsException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.error("Failed to enqueue notification job type={} recipientId={}", jobType, recipientId, ex);
        }
    }
}
