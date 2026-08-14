package com.foodbridge.document.dto;

import com.foodbridge.document.document.Receipt;

import java.time.Instant;

public record ReceiptResponse(
        String id,
        Long claimId,
        String listingId,
        Receipt.ReceiptStatus status,
        String downloadUrl,
        Instant generatedAt
) {
}
