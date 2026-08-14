package com.foodbridge.admin.dto;

import java.time.Instant;

public record ActivityFeedItem(String eventType, String summary, Instant occurredAt) {
}
