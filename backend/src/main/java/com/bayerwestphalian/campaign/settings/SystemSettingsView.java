package com.bayerwestphalian.campaign.settings;

import java.time.Instant;
import java.util.UUID;

/**
 * API projection for the System Settings screen (item 534).
 */
public record SystemSettingsView(
        UUID id,
        int monthlyContactLimit,
        int sendRetryLimit,
        int uninterestedExclusionDays,
        UUID updatedByUserId,
        Instant updatedAt) {

    public static SystemSettingsView from(SystemSettings settings) {
        return new SystemSettingsView(
                settings.getId(),
                settings.getMonthlyContactLimit(),
                settings.getSendRetryLimit(),
                settings.getUninterestedExclusionDays(),
                settings.getUpdatedByUserId(),
                settings.getUpdatedAt());
    }
}
