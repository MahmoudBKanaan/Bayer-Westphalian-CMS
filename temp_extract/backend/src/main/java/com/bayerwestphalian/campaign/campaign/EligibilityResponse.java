package com.bayerwestphalian.campaign.campaign;

import java.util.List;

public record EligibilityResponse(String status, boolean eligible, List<Reason> reasons) {

    public static final String STATUS_ELIGIBLE = "ELIGIBLE";
    public static final String STATUS_EXCLUDED = "EXCLUDED";

    public EligibilityResponse {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public static EligibilityResponse from(EligibilityDecision decision) {
        if (decision.eligible()) {
            return new EligibilityResponse(STATUS_ELIGIBLE, true, List.of());
        }
        return new EligibilityResponse(
                STATUS_EXCLUDED,
                false,
                List.of(new Reason(decision.exclusionReason(), decision.eligibilityExplanation())));
    }

    public String primaryReasonCode() {
        return reasons.isEmpty() ? null : reasons.get(0).code();
    }

    public String primaryReasonMessage() {
        return reasons.isEmpty() ? null : reasons.get(0).message();
    }

    public record Reason(String code, String message) {}
}
