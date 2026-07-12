package com.bayerwestphalian.campaign.campaign;

import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.util.StringUtils;

/**
 * Query parameters for campaign list endpoint {@code GET /api/campaigns} (item 219).
 *
 * @param term optional name/objective search text (max 255)
 * @param ownerUserId optional owner filter
 * @param status optional lifecycle status filter
 * @param segmentId optional linked segment filter
 */
public record CampaignSearchRequest(
        @Size(max = 255) String term,
        UUID ownerUserId,
        CampaignStatus status,
        UUID segmentId) {

    CampaignSearchCriteria toCriteria() {
        return new CampaignSearchCriteria(normalize(term), ownerUserId, status, segmentId);
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
