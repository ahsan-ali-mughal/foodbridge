package com.foodbridge.notification.document;

import com.foodbridge.common.enums.NotificationChannel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Audit record of every notification attempt, successful or not. Kept
 * separately from the SNS/SQS transport layer so "did NGO X get notified
 * about listing Y" can be answered from our own store rather than reaching
 * into AWS delivery logs.
 */
@Document(collection = "notification_logs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationLog {

    @Id
    private String id;

    private NotificationChannel channel;

    private String recipientType; // e.g. "NGO", "DONOR", "VOLUNTEER"

    private String recipientId;

    private String subject;

    private String eventType;

    private NotificationOutcome outcome;

    private String failureReason;

    @CreatedDate
    private Instant createdAt;

    public enum NotificationOutcome {
        SENT,
        FAILED,
        QUEUED
    }

    public static NotificationLog of(NotificationChannel channel, String recipientType, String recipientId,
                                      String subject, String eventType, NotificationOutcome outcome) {
        NotificationLog log = new NotificationLog();
        log.channel = channel;
        log.recipientType = recipientType;
        log.recipientId = recipientId;
        log.subject = subject;
        log.eventType = eventType;
        log.outcome = outcome;
        return log;
    }
}
