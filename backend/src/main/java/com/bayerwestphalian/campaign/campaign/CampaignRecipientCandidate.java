package com.bayerwestphalian.campaign.campaign;

import java.util.UUID;

/** Row-level campaign audience candidate produced before recipient rows are persisted. */
public record CampaignRecipientCandidate(UUID customerId, EligibilityDecision eligibilityDecision) {

    public boolean eligible() {
        return eligibilityDecision != null && eligibilityDecision.eligible();
    }

    public String exclusionReason() {
        return eligibilityDecision == null ? null : eligibilityDecision.exclusionReason();
    }

    public String eligibilityExplanation() {
        return eligibilityDecision == null ? null : eligibilityDecision.eligibilityExplanation();
    }
}
