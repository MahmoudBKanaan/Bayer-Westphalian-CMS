package com.bayerwestphalian.campaign.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Admin update payload for System Settings (item 534).
 */
public record UpdateSystemSettingsRequest(
        @NotNull @Min(1) @Max(100) Integer monthlyContactLimit,
        @NotNull @Min(1) @Max(20) Integer sendRetryLimit,
        @NotNull @Min(1) @Max(3650) Integer uninterestedExclusionDays) {

    public UpdateSystemSettingsCommand toCommand() {
        return new UpdateSystemSettingsCommand(
                monthlyContactLimit, sendRetryLimit, uninterestedExclusionDays);
    }
}
