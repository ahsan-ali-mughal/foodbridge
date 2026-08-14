package com.foodbridge.common.event;

/**
 * Discriminator for {@link EventEnvelope#eventType()}. Keep in sync with the
 * Kafka topic naming convention: topic name is the lower-kebab form of the constant.
 */
public enum EventType {
    DONATION_CREATED,
    DONATION_MATCHED,
    DONATION_CLAIMED,
    DONATION_EXPIRING_SOON,
    PICKUP_ASSIGNED,
    PICKUP_COMPLETED,
    PICKUP_FAILED,
    USER_REGISTERED
}
