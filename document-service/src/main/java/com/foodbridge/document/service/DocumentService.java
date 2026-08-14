package com.foodbridge.document.service;

import com.foodbridge.document.dto.ReceiptResponse;

public interface DocumentService {

    void createPendingReceipt(Long claimId, String listingId, Long donorId);

    void enqueueReceiptGeneration(Long claimId);

    void processReceiptGenerationJob(Long claimId);

    ReceiptResponse getByClaimId(Long claimId);
}
