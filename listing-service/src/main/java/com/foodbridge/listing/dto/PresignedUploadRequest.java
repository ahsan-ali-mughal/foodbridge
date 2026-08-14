package com.foodbridge.listing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PresignedUploadRequest(
        @NotBlank String fileName,
        @NotBlank @Pattern(regexp = "image/(jpeg|png|webp)", message = "only jpeg, png, or webp images are accepted")
        String contentType
) {
}
