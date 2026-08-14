package com.foodbridge.admin.dto;

public record DashboardStats(
        long totalListingsCreated,
        long totalDonationsClaimed,
        long totalDeliveriesCompleted,
        long totalUsersRegistered,
        long pendingNgoVerifications
) {
}
