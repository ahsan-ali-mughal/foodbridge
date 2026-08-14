package com.foodbridge.matching.listener;

import com.foodbridge.common.enums.Role;
import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.common.event.UserRegisteredEvent;
import com.foodbridge.matching.service.MatchingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * On every new NGO registration, pre-creates an (unlocated) profile stub so
 * the NGO shows up in matching-service's own store as soon as they complete
 * the separate "set my pickup location" step via the API.
 */
@Component
@Slf4j
public class UserRegisteredListener {

    private final MatchingService matchingService;

    public UserRegisteredListener(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @KafkaListener(topics = "user.registered", groupId = "matching-service")
    public void onUserRegistered(ConsumerRecord<String, EventEnvelope<UserRegisteredEvent>> record,
                                  Acknowledgment acknowledgment) {
        EventEnvelope<UserRegisteredEvent> envelope = record.value();
        MDC.put("correlationId", envelope.correlationId());
        try {
            UserRegisteredEvent event = envelope.payload();
            if (event.role() == Role.NGO) {
                matchingService.ensureNgoProfileExists(event.userId());
            }
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error("Failed to process user.registered event", ex);
            throw ex;
        } finally {
            MDC.clear();
        }
    }
}
