package com.foodbridge.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Generic envelope wrapping every domain event published to Kafka.
 * Consumers should always deserialize the envelope first, then the
 * {@code payload} into the concrete event type indicated by {@code eventType}.
 *
 * @param eventId      unique id of this event instance, used for idempotency checks by consumers
 * @param eventType    discriminator matching one of {@link EventType}
 * @param occurredAt   UTC timestamp when the event was produced
 * @param version      schema version of the payload, bump on breaking changes
 * @param correlationId propagated end-to-end for distributed tracing / log correlation
 * @param payload      the concrete event record (e.g. {@link DonationCreatedEvent})
 * @param <T>          concrete payload type
 */
public record EventEnvelope<T>(
        UUID eventId,
        EventType eventType,
        Instant occurredAt,
        int version,
        String correlationId,
        T payload
) {
    public static <T> EventEnvelope<T> of(EventType eventType, String correlationId, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), eventType, Instant.now(), 1, correlationId, payload);
    }
}
