package com.foodbridge.admin.service.impl;

import com.foodbridge.admin.client.AuthServiceClient;
import com.foodbridge.admin.document.ActivityEvent;
import com.foodbridge.admin.dto.ActivityFeedItem;
import com.foodbridge.admin.dto.DashboardStats;
import com.foodbridge.admin.dto.PendingNgoView;
import com.foodbridge.admin.repository.ActivityEventRepository;
import com.foodbridge.admin.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final ActivityEventRepository activityEventRepository;
    private final AuthServiceClient authServiceClient;

    public DashboardServiceImpl(ActivityEventRepository activityEventRepository, AuthServiceClient authServiceClient) {
        this.activityEventRepository = activityEventRepository;
        this.authServiceClient = authServiceClient;
    }

    @Override
    public DashboardStats getStats() {
        long listingsCreated = activityEventRepository.countByEventType("DONATION_CREATED");
        long donationsClaimed = activityEventRepository.countByEventType("DONATION_CLAIMED");
        long deliveriesCompleted = activityEventRepository.countByEventType("PICKUP_COMPLETED");
        long usersRegistered = activityEventRepository.countByEventType("USER_REGISTERED");
        long pendingNgos = getPendingNgos().size();

        return new DashboardStats(listingsCreated, donationsClaimed, deliveriesCompleted, usersRegistered, pendingNgos);
    }

    @Override
    public List<PendingNgoView> getPendingNgos() {
        List<AuthServiceClient.UserResponse> users = authServiceClient.listUsers("NGO", "PENDING");
        return users.stream()
                .map(u -> new PendingNgoView(u.id(), u.email(), u.displayName(), u.createdAt()))
                .toList();
    }

    @Override
    public List<ActivityFeedItem> getRecentActivity(int limit) {
        return activityEventRepository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, limit)).stream()
                .map(e -> new ActivityFeedItem(e.getEventType(), e.getSummary(), e.getOccurredAt()))
                .toList();
    }

    @Override
    public void approveNgo(Long userId) {
        authServiceClient.verifyNgo(userId, new AuthServiceClient.VerifyNgoRequest("VERIFIED"));
        log.info("Admin approved NGO userId={}", userId);
    }

    @Override
    public void rejectNgo(Long userId) {
        authServiceClient.verifyNgo(userId, new AuthServiceClient.VerifyNgoRequest("REJECTED"));
        log.info("Admin rejected NGO userId={}", userId);
    }

    @Override
    public void recordActivity(String eventType, String summary) {
        activityEventRepository.save(ActivityEvent.of(eventType, summary));
    }
}
