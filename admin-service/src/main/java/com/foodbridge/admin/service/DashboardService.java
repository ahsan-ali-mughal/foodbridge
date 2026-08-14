package com.foodbridge.admin.service;

import com.foodbridge.admin.dto.ActivityFeedItem;
import com.foodbridge.admin.dto.DashboardStats;
import com.foodbridge.admin.dto.PendingNgoView;

import java.util.List;

public interface DashboardService {

    DashboardStats getStats();

    List<PendingNgoView> getPendingNgos();

    List<ActivityFeedItem> getRecentActivity(int limit);

    void approveNgo(Long userId);

    void rejectNgo(Long userId);

    void recordActivity(String eventType, String summary);
}
