package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EligibilityResponseTests {

    @Test
    void mapsEligibleDecisionToEligibleStatusWithoutReasons() {
        EligibilityResponse response = EligibilityResponse.from(EligibilityDecision.included());

        assertThat(response.status()).isEqualTo(EligibilityResponse.STATUS_ELIGIBLE);
        assertThat(response.eligible()).isTrue();
        assertThat(response.reasons()).isEmpty();
        assertThat(response.primaryReasonCode()).isNull();
        assertThat(response.primaryReasonMessage()).isNull();
    }

    @Test
    void mapsExcludedDecisionToExcludedStatusWithStructuredReason() {
        EligibilityResponse response =
                EligibilityResponse.from(
                        EligibilityDecision.excluded(
                                EligibilityExclusionReason.MARKETING_OPT_OUT));

        assertThat(response.status()).isEqualTo(EligibilityResponse.STATUS_EXCLUDED);
        assertThat(response.eligible()).isFalse();
        assertThat(response.reasons())
                .containsExactly(
                        new EligibilityResponse.Reason(
                                EligibilityExclusionReason.CODE_MARKETING_OPT_OUT,
                                "Customer has withdrawn or rejected marketing consent"));
        assertThat(response.primaryReasonCode())
                .isEqualTo(EligibilityExclusionReason.CODE_MARKETING_OPT_OUT);
        assertThat(response.primaryReasonMessage())
                .isEqualTo("Customer has withdrawn or rejected marketing consent");
    }

    @Test
    void excludedDecisionReturnsReasonFieldsReadyForRecipientStorage() {
        EligibilityResponse response =
                EligibilityResponse.from(
                        EligibilityDecision.excluded(
                                EligibilityExclusionReason.DO_NOT_CONTACT));

        Map<String, String> storageColumns =
                Map.of(
                        "exclusion_reason",
                        response.primaryReasonCode(),
                        "eligibility_explanation",
                        response.primaryReasonMessage());

        assertThat(response.status()).isEqualTo(EligibilityResponse.STATUS_EXCLUDED);
        assertThat(storageColumns)
                .containsEntry("exclusion_reason", EligibilityExclusionReason.CODE_DO_NOT_CONTACT)
                .containsEntry(
                        "eligibility_explanation", "Customer has do-not-contact enabled");
        assertThat(storageColumns.values()).allSatisfy(value -> assertThat(value).isNotBlank());
    }

    @Test
    void responseDefensivelyCopiesReasons() {
        List<EligibilityResponse.Reason> reasons =
                new java.util.ArrayList<>(
                        List.of(new EligibilityResponse.Reason("INVALID_CONSENT", "Missing")));

        EligibilityResponse response =
                new EligibilityResponse(EligibilityResponse.STATUS_EXCLUDED, false, reasons);
        reasons.clear();

        assertThat(response.reasons())
                .containsExactly(new EligibilityResponse.Reason("INVALID_CONSENT", "Missing"));
    }
}
