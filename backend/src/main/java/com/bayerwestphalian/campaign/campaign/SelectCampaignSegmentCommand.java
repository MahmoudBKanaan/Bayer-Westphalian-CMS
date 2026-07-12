package com.bayerwestphalian.campaign.campaign;

import java.util.UUID;

/**
 * Service command for FR-053 campaign segment selection. {@code null} segmentId clears the linked
 * segment.
 */
public record SelectCampaignSegmentCommand(UUID segmentId) {}
