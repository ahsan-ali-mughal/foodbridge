package com.foodbridge.logistics.service.impl;

import com.foodbridge.common.enums.PickupStatus;
import com.foodbridge.common.event.EventEnvelope;
import com.foodbridge.common.event.EventType;
import com.foodbridge.common.event.PickupCompletedEvent;
import com.foodbridge.common.event.PickupFailedEvent;
import com.foodbridge.common.exception.ConflictException;
import com.foodbridge.common.exception.ForbiddenException;
import com.foodbridge.common.exception.ResourceNotFoundException;
import com.foodbridge.logistics.dto.PickupTaskResponse;
import com.foodbridge.logistics.entity.PickupTask;
import com.foodbridge.logistics.repository.PickupTaskRepository;
import com.foodbridge.logistics.service.LogisticsService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@SuppressWarnings("rawtypes")
public class LogisticsServiceImpl implements LogisticsService {

    private static final String TOPIC_PICKUP_COMPLETED = "pickup.completed";
    private static final String TOPIC_PICKUP_FAILED = "pickup.failed";

    private final PickupTaskRepository pickupTaskRepository;
    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;

    public LogisticsServiceImpl(PickupTaskRepository pickupTaskRepository,
                                 KafkaTemplate<String, EventEnvelope> kafkaTemplate) {
        this.pickupTaskRepository = pickupTaskRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Transactional
    public void createTaskForClaim(Long claimId, String listingId) {
        if (pickupTaskRepository.findByClaimId(claimId).isPresent()) {
            log.info("Pickup task already exists for claimId={}, skipping (idempotent)", claimId);
            return;
        }
        PickupTask task = pickupTaskRepository.save(PickupTask.newUnassignedTask(claimId, listingId));
        log.info("Created pickup task id={} for claimId={} listingId={}", task.getId(), claimId, listingId);
    }

    @Override
    @Transactional
    public PickupTaskResponse acceptTask(Long taskId, Long volunteerId) {
        PickupTask task = getTaskOrThrow(taskId);
        if (task.getVolunteerId() != null) {
            throw new ConflictException("TASK_ALREADY_ASSIGNED", "This pickup task already has a volunteer assigned");
        }
        task.assignVolunteer(volunteerId);
        pickupTaskRepository.save(task);
        log.info("Volunteer volunteerId={} accepted taskId={}", volunteerId, taskId);
        return toResponse(task);
    }

    @Override
    @Transactional
    public PickupTaskResponse markPickedUp(Long taskId, Long volunteerId) {
        PickupTask task = getOwnedTaskOrThrow(taskId, volunteerId);
        task.markPickedUp();
        pickupTaskRepository.save(task);
        log.info("Task taskId={} marked PICKED_UP by volunteerId={}", taskId, volunteerId);
        return toResponse(task);
    }

    @Override
    @Transactional
    public PickupTaskResponse markDelivered(Long taskId, Long volunteerId) {
        PickupTask task = getOwnedTaskOrThrow(taskId, volunteerId);
        task.markDelivered();
        pickupTaskRepository.save(task);
        publishPickupCompleted(task);
        log.info("Task taskId={} marked DELIVERED by volunteerId={}", taskId, volunteerId);
        return toResponse(task);
    }

    @Override
    @Transactional
    public PickupTaskResponse markFailed(Long taskId, Long volunteerId, String reason) {
        PickupTask task = getOwnedTaskOrThrow(taskId, volunteerId);
        task.markFailed(reason);
        pickupTaskRepository.save(task);
        publishPickupFailed(task);
        log.warn("Task taskId={} marked FAILED by volunteerId={} reason={}", taskId, volunteerId, reason);
        return toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PickupTaskResponse> getUnassignedTasks(Pageable pageable) {
        return pickupTaskRepository.findByVolunteerIdIsNullAndStatus(PickupStatus.ASSIGNED, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PickupTaskResponse> getByVolunteer(Long volunteerId, Pageable pageable) {
        return pickupTaskRepository.findByVolunteerIdAndStatus(volunteerId, PickupStatus.ASSIGNED, pageable)
                .map(this::toResponse);
    }

    private PickupTask getTaskOrThrow(Long taskId) {
        return pickupTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("PickupTask", taskId));
    }

    private PickupTask getOwnedTaskOrThrow(Long taskId, Long volunteerId) {
        PickupTask task = getTaskOrThrow(taskId);
        if (task.getVolunteerId() == null || !task.getVolunteerId().equals(volunteerId)) {
            throw new ForbiddenException("You may only update pickup tasks assigned to you");
        }
        return task;
    }

    private void publishPickupCompleted(PickupTask task) {
        var payload = new PickupCompletedEvent(task.getClaimId(), task.getListingId(), task.getVolunteerId(), task.getDeliveredAt());
        var envelope = EventEnvelope.of(EventType.PICKUP_COMPLETED, MDC.get("correlationId"), payload);
        kafkaTemplate.send(TOPIC_PICKUP_COMPLETED, task.getListingId(), envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish pickup.completed for taskId={}", task.getId(), ex);
                    }
                });
    }

    private void publishPickupFailed(PickupTask task) {
        var payload = new PickupFailedEvent(task.getClaimId(), task.getFailureReason());
        var envelope = EventEnvelope.of(EventType.PICKUP_FAILED, MDC.get("correlationId"), payload);
        kafkaTemplate.send(TOPIC_PICKUP_FAILED, task.getListingId(), envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish pickup.failed for taskId={}", task.getId(), ex);
                    }
                });
    }

    private PickupTaskResponse toResponse(PickupTask task) {
        return new PickupTaskResponse(task.getId(), task.getClaimId(), task.getListingId(), task.getVolunteerId(),
                task.getStatus(), task.getAssignedAt(), task.getPickedUpAt(), task.getDeliveredAt());
    }
}
