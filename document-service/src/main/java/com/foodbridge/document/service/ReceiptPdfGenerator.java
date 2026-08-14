package com.foodbridge.document.service;

public interface ReceiptPdfGenerator {

    byte[] generate(Long claimId, String listingId, Long donorId, String deliveredAtIso);
}
