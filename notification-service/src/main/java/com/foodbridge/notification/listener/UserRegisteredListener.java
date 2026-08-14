package com.foodbridge.notification.listener;

import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.common.event.UserRegisteredEvent;
import com.foodbridge.notification.service.SqsJobEnqueuer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Welcome emails are not time-critical, so they go through the SQS job
 * queue rather than SNS's urgent fan-out.
 */
@Component
@Slf4j
public class UserRegisteredListener {

    private final SqsJobEnqueuer sqsJobEnqueuer;

    public UserRegisteredListener(SqsJobEnqueuer sqsJobEnqueuer) {
        this.sqsJobEnqueuer = sqsJobEnqueuer;
    }

    @KafkaListener(topics = "user.registered", groupId = "notification-service")
    public void onUserRegistered(ConsumerRecord<String, EventEnvelope<UserRegisteredEvent>> record,
                                  Acknowledgment acknowledgment) {
        EventEnvelope<UserRegisteredEvent> envelope = record.value();
        MDC.put("correlationId", envelope.correlationId());
        try {
            UserRegisteredEvent event = envelope.payload();
            sqsJobEnqueuer.enqueue("WELCOME_EMAIL", String.valueOf(event.userId()), event.email());
            log.info("Enqueued welcome email job for userId={}", event.userId());
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process user.registered notification, will retry on redelivery", ex);
            throw ex;
        } finally {
            MDC.clear();
        }
    }
}
