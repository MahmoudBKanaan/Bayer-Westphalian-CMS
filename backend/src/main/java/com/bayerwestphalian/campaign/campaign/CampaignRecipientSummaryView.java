package com.bayerwestphalian.campaign.campaign;

import java.util.UUID;

/** Summary of persisted recipient delivery outcomes for a campaign (KB item 284). */
public record CampaignRecipientSummaryView(
        UUID campaignId, long eligible, long excluded, long sent, long failed) {}
