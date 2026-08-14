package com.foodbridge.logistics.dto;

import com.foodbridge.common.enums.PickupStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull PickupStatus status, String failureReason) {
}
