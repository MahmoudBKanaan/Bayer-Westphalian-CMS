package com.bayerwestphalian.campaign.campaign;

/** Recipient eligibility and delivery status for KB {@code campaign_recipients}. */
public enum CampaignRecipientStatus {
    ELIGIBLE,
    EXCLUDED,
    SENT,
    OPENED,
    CLICKED,
    REPLIED,
    CONVERTED,
    FAILED
}
