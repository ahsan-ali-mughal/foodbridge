package com.foodbridge.logistics.repository;

import com.foodbridge.common.enums.PickupStatus;
import com.foodbridge.logistics.entity.PickupTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PickupTaskRepository extends JpaRepository<PickupTask, Long> {

    Optional<PickupTask> findByClaimId(Long claimId);

    List<PickupTask> findByStatus(PickupStatus status);

    Page<PickupTask> findByVolunteerIdIsNullAndStatus(PickupStatus status, Pageable pageable);

    Page<PickupTask> findByVolunteerIdAndStatus(Long volunteerId, PickupStatus status, Pageable pageable);
}
