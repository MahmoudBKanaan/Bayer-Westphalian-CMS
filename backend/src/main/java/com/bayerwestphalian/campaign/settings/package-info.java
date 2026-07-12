/**
 * System settings package (KB item 534 / Sprint 14 — System Settings screen).
 *
 * <p>Admin-managed business configuration for contact limits, send retry, and uninterested
 * exclusion period. Exposed at {@code /api/system-settings} (GET/PUT, Admin only). Item 535:
 * monthly contact limit via {@link
 * com.bayerwestphalian.campaign.settings.SystemSettingsService#monthlyContactLimit()}. Item 536:
 * send retry limit via {@link
 * com.bayerwestphalian.campaign.settings.SystemSettingsService#sendRetryLimit()} consumed by
 * {@code SendRetryService}. Item 537: uninterested exclusion period via {@link
 * com.bayerwestphalian.campaign.settings.SystemSettingsService#uninterestedExclusionDays()}
 * consumed by {@code EligibilityService}.
 *
 * <ul>
 *   <li>{@link com.bayerwestphalian.campaign.settings.SystemSettings} — singleton JPA entity
 *   <li>{@link com.bayerwestphalian.campaign.settings.SystemSettingsService} — load / update
 *   <li>{@link com.bayerwestphalian.campaign.settings.SystemSettingsController} — REST boundary
 * </ul>
 */
package com.bayerwestphalian.campaign.settings;
