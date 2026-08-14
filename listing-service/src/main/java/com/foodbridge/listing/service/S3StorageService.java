package com.foodbridge.listing.service;

import com.foodbridge.listing.dto.PresignedUploadResponse;

public interface S3StorageService {

    PresignedUploadResponse createPresignedUploadUrl(String listingOwnerId, String fileName, String contentType);
}
