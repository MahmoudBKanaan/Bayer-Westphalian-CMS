package com.bayerwestphalian.campaign.campaign;

/**
 * HTTP body for {@code PUT /api/campaigns/{id}/compliance-review-notes} (item 231).
 *
 * <p>{@code complianceReviewNotes} is optional; blank values clear stored notes.
 */
public record RecordComplianceReviewNotesRequest(String complianceReviewNotes) {}
