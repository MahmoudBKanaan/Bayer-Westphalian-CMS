package com.bayerwestphalian.campaign.campaign;

/**
 * Service command for rejecting a submitted campaign.
 *
 * <p>{@code rejectionReason} is required (item 232 / KB {@code rejectionReason}). Optional {@code
 * complianceReviewNotes} are item 231.
 */
public record RejectCampaignCommand(String rejectionReason, String complianceReviewNotes) {

    public RejectCampaignCommand(String rejectionReason) {
        this(rejectionReason, null);
    }
}
