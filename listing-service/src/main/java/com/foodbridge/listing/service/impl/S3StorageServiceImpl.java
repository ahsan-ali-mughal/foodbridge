package com.foodbridge.listing.service.impl;

import com.foodbridge.listing.dto.PresignedUploadResponse;
import com.foodbridge.listing.service.S3StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

/**
 * Issues pre-signed S3 PUT URLs so the browser uploads listing photos
 * directly to S3 — the listing-service API never proxies raw image bytes.
 */
@Service
@Slf4j
public class S3StorageServiceImpl implements S3StorageService {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(5);

    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String publicBaseUrl;

    public S3StorageServiceImpl(S3Presigner s3Presigner,
                                 @Value("${aws.s3.listing-images-bucket}") String bucketName,
                                 @Value("${aws.s3.public-base-url}") String publicBaseUrl) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public PresignedUploadResponse createPresignedUploadUrl(String listingOwnerId, String fileName, String contentType) {
        String objectKey = "%s/%s-%s".formatted(listingOwnerId, UUID.randomUUID(), sanitize(fileName));

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_URL_TTL)
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        String objectUrl = "%s/%s/%s".formatted(publicBaseUrl, bucketName, objectKey);

        log.info("Issued pre-signed upload URL for owner={} key={}", listingOwnerId, objectKey);
        return new PresignedUploadResponse(presigned.url().toString(), objectUrl, UPLOAD_URL_TTL.toSeconds());
    }

    private String sanitize(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
