package com.foodbridge.document.service;

public interface S3ReceiptStorageService {

    /** Uploads PDF bytes to S3 and returns the object key stored. */
    String uploadReceiptPdf(String claimId, byte[] pdfBytes);

    /** Generates a time-limited pre-signed GET URL for downloading a stored receipt. */
    String presignedDownloadUrl(String s3Key);
}
