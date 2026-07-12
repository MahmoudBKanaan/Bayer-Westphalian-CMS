package com.bayerwestphalian.campaign.followup;

import jakarta.validation.constraints.NotNull;

public record UpdateFollowUpStatusRequest(@NotNull FollowUpTaskStatus status) {}
