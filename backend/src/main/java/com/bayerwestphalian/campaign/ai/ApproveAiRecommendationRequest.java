package com.bayerwestphalian.campaign.ai;

import jakarta.validation.constraints.Size;

/**
 * Optional body when a human approves a stored AI recommendation (KB COMP-005 / AI-005 / item 471 /
 * item 482).
 *
 * <p>Approver identity comes from the authenticated principal, not this body. Optional edited
 * subject/body/CTA override the stored suggestion text before applying to a DRAFT campaign.
 * Notes are optional audit context only.
 */
public record ApproveAiRecommendationRequest(
        @Size(max = 1000) String reviewNotes,
        @Size(max = 255) String editedSubject,
        String editedMessageBody,
        @Size(max = 255) String editedCallToAction) {

    /** Back-compat constructor used by existing tests and clients that only send review notes. */
    public ApproveAiRecommendationRequest(String reviewNotes) {
        this(reviewNotes, null, null, null);
    }
}
