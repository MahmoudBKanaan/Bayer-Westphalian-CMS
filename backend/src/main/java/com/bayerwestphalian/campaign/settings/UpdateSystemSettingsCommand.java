package com.bayerwestphalian.campaign.settings;

/**
 * Internal update command for {@link SystemSettingsService#updateSettings}.
 */
public record UpdateSystemSettingsCommand(
        int monthlyContactLimit, int sendRetryLimit, int uninterestedExclusionDays) {}
