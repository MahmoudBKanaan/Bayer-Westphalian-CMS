package com.bayerwestphalian.campaign.campaign;

/**
 * Campaign lifecycle status (KB {@code campaigns.status} / {@code campaign_status} enum).
 *
 * <p>Controlled flow: DRAFT → SUBMITTED → APPROVED|REJECTED; APPROVED → ACTIVE (launch);
 * ACTIVE → PAUSED|COMPLETED; COMPLETED|REJECTED → ARCHIVED.
 */
public enum CampaignStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED
}
