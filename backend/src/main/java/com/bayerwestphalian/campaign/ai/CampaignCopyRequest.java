package com.bayerwestphalian.campaign.ai;

import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request for campaign copy suggestion (KB AI-005 / {@code POST /api/ai/campaign-copy} / item 471 /
 * CampaignCopyService.generateCopySuggestion).
 *
 * <p>Generated copy always requires human approval before use (COMP-005 / item 482).
 */
public record CampaignCopyRequest(
        UUID campaignId,
        @NotBlank @Size(max = 500) String objective,
        @Size(max = 255) String productName,
        CampaignChannel channel,
        @Size(max = 500) String audienceHint) {}
