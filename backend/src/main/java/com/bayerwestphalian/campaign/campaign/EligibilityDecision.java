package com.bayerwestphalian.campaign.campaign;

public record EligibilityDecision(
        boolean eligible, String exclusionReason, String eligibilityExplanation) {

    public static EligibilityDecision included() {
        return new EligibilityDecision(true, null, "Customer is eligible for campaign contact");
    }

    public static EligibilityDecision excluded(EligibilityExclusionReason reason) {
        return excluded(reason.code(), reason.explanation());
    }

    public static EligibilityDecision excluded(String reason, String explanation) {
        return new EligibilityDecision(false, reason, explanation);
    }
}
