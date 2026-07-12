package com.bayerwestphalian.campaign.followup;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignFollowUpTaskRequest(@NotNull UUID assignedTo) {}
