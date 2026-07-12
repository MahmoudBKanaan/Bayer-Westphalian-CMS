package com.bayerwestphalian.campaign.campaign;

import java.util.UUID;

/** Normalized search filters for listing campaigns. */
public record CampaignSearchCriteria(
        String term, UUID ownerUserId, CampaignStatus status, UUID segmentId) {}
