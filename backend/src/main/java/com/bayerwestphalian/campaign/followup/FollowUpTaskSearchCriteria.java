package com.bayerwestphalian.campaign.followup;

import java.time.LocalDate;
import java.util.UUID;

public record FollowUpTaskSearchCriteria(
        UUID customerId,
        UUID assignedTo,
        FollowUpTaskPriority priority,
        FollowUpTaskStatus status,
        LocalDate dueDateFrom,
        LocalDate dueDateTo) {}
