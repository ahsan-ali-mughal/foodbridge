package com.foodbridge.logistics.controller;

import com.foodbridge.logistics.dto.PickupTaskResponse;
import com.foodbridge.logistics.dto.UpdateStatusRequest;
import com.foodbridge.logistics.service.LogisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logistics/tasks")
@Tag(name = "Logistics", description = "Volunteer pickup and delivery task tracking")
public class PickupTaskController {

    private final LogisticsService logisticsService;

    public PickupTaskController(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    @GetMapping("/unassigned")
    @Operation(summary = "List pickup tasks awaiting a volunteer")
    public ResponseEntity<Page<PickupTaskResponse>> unassigned(Pageable pageable) {
        return ResponseEntity.ok(logisticsService.getUnassignedTasks(pageable));
    }

    @GetMapping("/mine")
    @Operation(summary = "List the current volunteer's assigned tasks")
    public ResponseEntity<Page<PickupTaskResponse>> mine(Pageable pageable, Authentication authentication) {
        Long volunteerId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(logisticsService.getByVolunteer(volunteerId, pageable));
    }

    @PostMapping("/{taskId}/accept")
    @Operation(summary = "Accept an unassigned pickup task")
    public ResponseEntity<PickupTaskResponse> accept(@PathVariable Long taskId, Authentication authentication) {
        Long volunteerId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(logisticsService.acceptTask(taskId, volunteerId));
    }

    @PatchMapping("/{taskId}/status")
    @Operation(summary = "Update the status of a task assigned to the current volunteer")
    public ResponseEntity<PickupTaskResponse> updateStatus(@PathVariable Long taskId,
                                                             @Valid @RequestBody UpdateStatusRequest request,
                                                             Authentication authentication) {
        Long volunteerId = (Long) authentication.getPrincipal();
        PickupTaskResponse response = switch (request.status()) {
            case PICKED_UP -> logisticsService.markPickedUp(taskId, volunteerId);
            case DELIVERED -> logisticsService.markDelivered(taskId, volunteerId);
            case FAILED -> logisticsService.markFailed(taskId, volunteerId, request.failureReason());
            case ASSIGNED -> throw new IllegalArgumentException("Cannot manually transition a task back to ASSIGNED");
        };
        return ResponseEntity.ok(response);
    }
}
