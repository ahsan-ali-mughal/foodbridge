package com.foodbridge.document.service.impl;

import com.foodbridge.document.service.S3ReceiptStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;

@Service
@Slf4j
public class S3ReceiptStorageServiceImpl implements S3ReceiptStorageService {

    private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3ReceiptStorageServiceImpl(S3Client s3Client, S3Presigner s3Presigner,
                                        @Value("${aws.s3.receipts-bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    @Override
    public String uploadReceiptPdf(String claimId, byte[] pdfBytes) {
        String key = "%s/receipt.pdf".formatted(claimId);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/pdf")
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(pdfBytes));
        log.info("Uploaded receipt PDF to s3://{}/{}", bucketName, key);
        return key;
    }

    @Override
    public String presignedDownloadUrl(String s3Key) {
        GetObjectRequest getRequest = GetObjectRequest.builder().bucket(bucketName).key(s3Key).build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(DOWNLOAD_URL_TTL)
                .getObjectRequest(getRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
