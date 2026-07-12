package com.bayerwestphalian.campaign.ai;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Campaign copy suggestion payload (KB AI-005 / item 471 / item 481–482).
 *
 * <p>{@code requiresHumanApproval} is always {@code true}. Suggestions must not be applied to a
 * live campaign until a human approves (COMP-005).
 */
public record CampaignCopySuggestionView(
        UUID campaignId,
        String subject,
        String body,
        String callToAction,
        String explanation,
        BigDecimal confidenceScore,
        boolean requiresHumanApproval,
        boolean humanApproved,
        UUID approvedByUserId,
        UUID storedRecommendationId) {

    public CampaignCopySuggestionView {
        Objects.requireNonNull(subject, "subject is required");
        Objects.requireNonNull(body, "body is required");
        Objects.requireNonNull(explanation, "explanation is required");
        // COMP-005 / AI-005: copy suggestions always need human review.
        requiresHumanApproval = true;
    }

    public static CampaignCopySuggestionView pending(
            UUID campaignId,
            String subject,
            String body,
            String callToAction,
            String explanation,
            UUID storedRecommendationId) {
        return pending(
                campaignId, subject, body, callToAction, explanation, null, storedRecommendationId);
    }

    public static CampaignCopySuggestionView pending(
            UUID campaignId,
            String subject,
            String body,
            String callToAction,
            String explanation,
            BigDecimal confidenceScore,
            UUID storedRecommendationId) {
        return new CampaignCopySuggestionView(
                campaignId,
                subject,
                body,
                callToAction,
                explanation,
                confidenceScore,
                true,
                false,
                null,
                storedRecommendationId);
    }
}
