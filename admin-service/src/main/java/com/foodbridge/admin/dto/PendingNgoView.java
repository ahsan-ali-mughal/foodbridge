package com.foodbridge.admin.dto;

import java.time.Instant;

public record PendingNgoView(Long id, String email, String displayName, Instant createdAt) {
}
