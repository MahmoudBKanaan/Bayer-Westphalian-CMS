package com.bayerwestphalian.campaign.followup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateFollowUpTaskRequest(
        @NotNull UUID customerId,
        UUID campaignId,
        UUID assignedTo,
        @NotBlank @Size(max = 255) String title,
        String description,
        LocalDate dueDate,
        FollowUpTaskPriority priority) {

    public CreateFollowUpTaskCommand toCommand() {
        return new CreateFollowUpTaskCommand(
                customerId,
                campaignId,
                assignedTo,
                title,
                description,
                dueDate,
                priority == null ? FollowUpTaskPriority.MEDIUM : priority);
    }
}
