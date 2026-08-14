package com.foodbridge.document.document;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Metadata record for a generated receipt PDF. The PDF bytes themselves
 * live in S3 ({@code s3Key}); this document is the queryable index over
 * them ("show me claim X's receipt") plus a processing-status audit trail.
 */
@Document(collection = "receipts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Receipt {

    @Id
    private String id;

    private Long claimId;

    private String listingId;

    private Long donorId;

    private String s3Bucket;

    private String s3Key;

    private ReceiptStatus status;

    private String failureReason;

    @CreatedDate
    private Instant createdAt;

    private Instant generatedAt;

    public enum ReceiptStatus {
        PENDING,
        GENERATED,
        FAILED
    }

    public static Receipt pending(Long claimId, String listingId, Long donorId) {
        Receipt receipt = new Receipt();
        receipt.claimId = claimId;
        receipt.listingId = listingId;
        receipt.donorId = donorId;
        receipt.status = ReceiptStatus.PENDING;
        return receipt;
    }

    public void markGenerated(String s3Bucket, String s3Key) {
        this.s3Bucket = s3Bucket;
        this.s3Key = s3Key;
        this.status = ReceiptStatus.GENERATED;
        this.generatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = ReceiptStatus.FAILED;
        this.failureReason = reason;
    }
}
