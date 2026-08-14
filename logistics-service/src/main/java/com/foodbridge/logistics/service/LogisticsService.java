package com.foodbridge.logistics.service;

import com.foodbridge.logistics.dto.PickupTaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LogisticsService {

    void createTaskForClaim(Long claimId, String listingId);

    PickupTaskResponse acceptTask(Long taskId, Long volunteerId);

    PickupTaskResponse markPickedUp(Long taskId, Long volunteerId);

    PickupTaskResponse markDelivered(Long taskId, Long volunteerId);

    PickupTaskResponse markFailed(Long taskId, Long volunteerId, String reason);

    Page<PickupTaskResponse> getUnassignedTasks(Pageable pageable);

    Page<PickupTaskResponse> getByVolunteer(Long volunteerId, Pageable pageable);
}
