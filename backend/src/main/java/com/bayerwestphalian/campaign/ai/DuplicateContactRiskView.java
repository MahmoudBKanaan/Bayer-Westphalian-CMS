package com.bayerwestphalian.campaign.ai;

import java.util.Objects;
import java.util.UUID;

/**
 * Duplicate-contact risk warning payload (KB AI-006 / BR-010 / BR-011 / item 471).
 *
 * <p>Warns when contact frequency or same-campaign duplicate rules are at risk. Does not block
 * sends by itself — operators and eligibility rules remain authoritative (COMP-005).
 */
public record DuplicateContactRiskView(
        UUID customerId,
        UUID campaignId,
        boolean riskDetected,
        String warning,
        String explanation,
        int contactsInCurrentMonth,
        Integer monthlyContactLimit,
        boolean sameCampaignAlreadyContacted,
        UUID storedRecommendationId) {

    public DuplicateContactRiskView {
        Objects.requireNonNull(customerId, "customerId is required");
        Objects.requireNonNull(explanation, "explanation is required");
        if (contactsInCurrentMonth < 0) {
            throw new IllegalArgumentException("contactsInCurrentMonth must not be negative");
        }
    }
}
