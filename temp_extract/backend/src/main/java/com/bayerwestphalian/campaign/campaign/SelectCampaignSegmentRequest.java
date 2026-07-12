package com.bayerwestphalian.campaign.campaign;

import java.util.UUID;

/**
 * HTTP body for {@code PUT /api/campaigns/{id}/segment} (KB FR-053 / item 222).
 *
 * <p>{@code segmentId} may be null to clear the campaign's target segment.
 */
public record SelectCampaignSegmentRequest(UUID segmentId) {

    SelectCampaignSegmentCommand toCommand() {
        return new SelectCampaignSegmentCommand(segmentId);
    }
}
