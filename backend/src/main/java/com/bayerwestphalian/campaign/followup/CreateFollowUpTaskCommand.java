package com.bayerwestphalian.campaign.followup;

import java.time.LocalDate;
import java.util.UUID;

public record CreateFollowUpTaskCommand(
        UUID customerId,
        UUID campaignId,
        UUID assignedTo,
        String title,
        String description,
        LocalDate dueDate,
        FollowUpTaskPriority priority) {}
