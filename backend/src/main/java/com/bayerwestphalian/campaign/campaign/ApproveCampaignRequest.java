package com.bayerwestphalian.campaign.campaign;

/**
 * HTTP body for {@code POST /api/campaigns/{id}/approve} (item 231 compliance review notes).
 *
 * <p>Body may be omitted or empty; {@code complianceReviewNotes} is optional.
 */
public record ApproveCampaignRequest(String complianceReviewNotes) {

    ApproveCampaignCommand toCommand() {
        return new ApproveCampaignCommand(complianceReviewNotes);
    }
}
