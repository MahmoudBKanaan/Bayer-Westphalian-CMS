package com.bayerwestphalian.campaign.followup;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FollowUpTaskView(
        UUID id,
        UUID customerId,
        String customerFullName,
        UUID campaignId,
        String campaignName,
        UUID assignedToUserId,
        String assignedToFullName,
        String title,
        String description,
        LocalDate dueDate,
        FollowUpTaskStatus status,
        FollowUpTaskPriority priority,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static FollowUpTaskView from(FollowUpTask task) {
        return new FollowUpTaskView(
                task.getId(),
                task.getCustomer().getId(),
                task.getCustomer().getFirstName() + " " + task.getCustomer().getLastName(),
                task.getCampaign() == null ? null : task.getCampaign().getId(),
                task.getCampaign() == null ? null : task.getCampaign().getName(),
                task.getAssignedTo() == null ? null : task.getAssignedTo().getId(),
                task.getAssignedTo() == null ? null : task.getAssignedTo().getFullName(),
                task.getTitle(),
                task.getDescription(),
                task.getDueDate(),
                task.getStatus(),
                task.getPriority(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
