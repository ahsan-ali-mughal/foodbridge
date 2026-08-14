package com.foodbridge.logistics.entity;

import com.foodbridge.common.enums.PickupStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Tracks a single pickup/delivery task created in response to a successful
 * claim. A task starts unassigned and is picked up by (or assigned to) a
 * volunteer, who then progresses it through {@link PickupStatus}.
 */
@Entity
@Table(name = "pickup_tasks", indexes = {
        @Index(name = "idx_pickup_tasks_claim_id", columnList = "claim_id", unique = true),
        @Index(name = "idx_pickup_tasks_volunteer_id", columnList = "volunteer_id"),
        @Index(name = "idx_pickup_tasks_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PickupTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "listing_id", nullable = false, length = 36)
    private String listingId;

    @Column(name = "volunteer_id")
    private Long volunteerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PickupStatus status;

    private Instant assignedAt;
    private Instant pickedUpAt;
    private Instant deliveredAt;
    private String failureReason;
    private Instant updatedAt;

    public static PickupTask newUnassignedTask(Long claimId, String listingId) {
        PickupTask task = new PickupTask();
        task.claimId = claimId;
        task.listingId = listingId;
        task.status = PickupStatus.ASSIGNED;
        return task;
    }

    public void assignVolunteer(Long volunteerId) {
        this.volunteerId = volunteerId;
        this.assignedAt = Instant.now();
        this.status = PickupStatus.ASSIGNED;
    }

    public void markPickedUp() {
        this.status = PickupStatus.PICKED_UP;
        this.pickedUpAt = Instant.now();
    }

    public void markDelivered() {
        this.status = PickupStatus.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = PickupStatus.FAILED;
        this.failureReason = reason;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
