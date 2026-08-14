package com.foodbridge.admin.document;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A denormalized, human-readable log line built from platform Kafka events,
 * purely for the admin dashboard's "recent activity" feed and summary
 * counters. Not a system of record for anything — each owning service's own
 * store remains authoritative; this is a read-model optimized for display.
 */
@Document(collection = "activity_events")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityEvent {

    @Id
    private String id;

    private String eventType;

    private String summary;

    private Instant occurredAt;

    public static ActivityEvent of(String eventType, String summary) {
        ActivityEvent event = new ActivityEvent();
        event.eventType = eventType;
        event.summary = summary;
        event.occurredAt = Instant.now();
        return event;
    }
}
