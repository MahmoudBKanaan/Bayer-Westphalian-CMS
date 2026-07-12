package com.bayerwestphalian.campaign.campaign;

import jakarta.validation.constraints.NotBlank;

/**
 * HTTP request body for {@code POST /api/campaigns/{id}/reject} (FR-059).
 *
 * <p>{@code rejectionReason} is required (item 232 / KB {@code campaigns.rejection_reason}).
 * {@code complianceReviewNotes} is optional (item 231).
 */
public record RejectCampaignRequest(
        @NotBlank(message = "Rejection reason is required.") String rejectionReason,
        String complianceReviewNotes) {

    public RejectCampaignRequest(String rejectionReason) {
        this(rejectionReason, null);
    }

    RejectCampaignCommand toCommand() {
        return new RejectCampaignCommand(rejectionReason, complianceReviewNotes);
    }
}
