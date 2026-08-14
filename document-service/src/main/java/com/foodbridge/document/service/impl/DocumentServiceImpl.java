package com.foodbridge.document.service.impl;

import com.foodbridge.common.exception.ResourceNotFoundException;
import com.foodbridge.document.client.ListingServiceClient;
import com.foodbridge.document.document.Receipt;
import com.foodbridge.document.dto.ReceiptResponse;
import com.foodbridge.document.repository.ReceiptRepository;
import com.foodbridge.document.service.DocumentService;
import com.foodbridge.document.service.ReceiptPdfGenerator;
import com.foodbridge.document.service.S3ReceiptStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;

@Service
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final ReceiptRepository receiptRepository;
    private final ListingServiceClient listingServiceClient;
    private final ReceiptPdfGenerator receiptPdfGenerator;
    private final S3ReceiptStorageService s3ReceiptStorageService;
    private final SqsClient sqsClient;
    private final String receiptQueueUrl;

    public DocumentServiceImpl(ReceiptRepository receiptRepository,
                                ListingServiceClient listingServiceClient,
                                ReceiptPdfGenerator receiptPdfGenerator,
                                S3ReceiptStorageService s3ReceiptStorageService,
                                SqsClient sqsClient,
                                @Value("${aws.sqs.receipt-generation-queue-url}") String receiptQueueUrl) {
        this.receiptRepository = receiptRepository;
        this.listingServiceClient = listingServiceClient;
        this.receiptPdfGenerator = receiptPdfGenerator;
        this.s3ReceiptStorageService = s3ReceiptStorageService;
        this.sqsClient = sqsClient;
        this.receiptQueueUrl = receiptQueueUrl;
    }

    @Override
    public void createPendingReceipt(Long claimId, String listingId, Long donorId) {
        if (receiptRepository.findByClaimId(claimId).isPresent()) {
            log.info("Receipt already exists for claimId={}, skipping (idempotent)", claimId);
            return;
        }
        receiptRepository.save(Receipt.pending(claimId, listingId, donorId));
        log.info("Created pending receipt record for claimId={}", claimId);
    }

    @Override
    public void enqueueReceiptGeneration(Long claimId) {
        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(receiptQueueUrl)
                .messageBody(String.valueOf(claimId))
                .build();
        sqsClient.sendMessage(request);
        log.info("Enqueued receipt generation job for claimId={}", claimId);
    }

    @Override
    public void processReceiptGenerationJob(Long claimId) {
        Receipt receipt = receiptRepository.findByClaimId(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", claimId));

        if (receipt.getStatus() == Receipt.ReceiptStatus.GENERATED) {
            log.info("Receipt for claimId={} already generated, skipping (idempotent)", claimId);
            return;
        }

        try {
            ListingServiceClient.ListingSummary listing = listingServiceClient.getListing(receipt.getListingId());
            byte[] pdfBytes = receiptPdfGenerator.generate(
                    claimId, receipt.getListingId(), listing.donorId(), Instant.now().toString());
            String s3Key = s3ReceiptStorageService.uploadReceiptPdf(String.valueOf(claimId), pdfBytes);

            receipt.markGenerated("foodbridge-receipts", s3Key);
            receiptRepository.save(receipt);
            log.info("Successfully generated and stored receipt for claimId={}", claimId);
        } catch (Exception ex) {
            receipt.markFailed(ex.getMessage());
            receiptRepository.save(receipt);
            log.error("Failed to generate receipt for claimId={}", claimId, ex);
            // Re-throw so the SQS poller does NOT delete the message — it becomes visible again
            // and retries, eventually landing in receipt-generation-dlq via the queue's redrive policy.
            throw ex;
        }
    }

    @Override
    public ReceiptResponse getByClaimId(Long claimId) {
        Receipt receipt = receiptRepository.findByClaimId(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt", claimId));
        String downloadUrl = receipt.getStatus() == Receipt.ReceiptStatus.GENERATED
                ? s3ReceiptStorageService.presignedDownloadUrl(receipt.getS3Key())
                : null;
        return new ReceiptResponse(receipt.getId(), receipt.getClaimId(), receipt.getListingId(),
                receipt.getStatus(), downloadUrl, receipt.getGeneratedAt());
    }
}
