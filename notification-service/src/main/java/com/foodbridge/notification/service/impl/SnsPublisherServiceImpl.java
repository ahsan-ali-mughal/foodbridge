package com.foodbridge.notification.service.impl;

import com.foodbridge.notification.service.SnsPublisherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

/**
 * Publishes to the {@code urgent-alerts-topic} SNS topic. SNS fans out to
 * every subscribed endpoint (an SQS queue feeding SMS, an SQS queue feeding
 * email, a mobile push endpoint) independently — a failure in one channel's
 * subscription does not block delivery to the others.
 */
@Service
@Slf4j
public class SnsPublisherServiceImpl implements SnsPublisherService {

    private final SnsClient snsClient;
    private final String topicArn;

    public SnsPublisherServiceImpl(SnsClient snsClient, @Value("${aws.sns.urgent-alerts-topic-arn}") String topicArn) {
        this.snsClient = snsClient;
        this.topicArn = topicArn;
    }

    @Override
    public String publishUrgentAlert(String subject, String message) {
        PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .subject(subject)
                .message(message)
                .build();
        try {
            PublishResponse response = snsClient.publish(request);
            log.info("Published SNS urgent alert messageId={} subject={}", response.messageId(), subject);
            return response.messageId();
        } catch (SnsException ex) {
            // Publishing failure here must not throw back into the calling Kafka listener and
            // block partition progress; the caller decides whether to log-and-continue or retry.
            log.error("Failed to publish SNS urgent alert subject={}", subject, ex);
            throw ex;
        }
    }
}
