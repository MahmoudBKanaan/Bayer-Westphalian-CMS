package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityExclusionReason;
import java.util.List;
import org.junit.jupiter.api.Test;

class SegmentExclusionReasonSummarySupportTests {

    @Test
    void summarizeReturnsEmptyListWhenNoExclusions() {
        assertThat(SegmentExclusionReasonSummarySupport.summarize(List.of())).isEmpty();
        assertThat(SegmentExclusionReasonSummarySupport.summarize(null)).isEmpty();
        assertThat(
                        SegmentExclusionReasonSummarySupport.summarize(
                                List.of(EligibilityDecision.included())))
                .isEmpty();
    }

    @Test
    void summarizeAggregatesByReasonCodeAndOrdersByCountDescending() {
        List<SegmentExclusionReasonSummary> summary =
                SegmentExclusionReasonSummarySupport.summarize(
                        List.of(
                                EligibilityDecision.excluded(
                                        EligibilityExclusionReason.DO_NOT_CONTACT),
                                EligibilityDecision.excluded(
                                        EligibilityExclusionReason.MARKETING_OPT_OUT),
                                EligibilityDecision.excluded(
                                        EligibilityExclusionReason.DO_NOT_CONTACT),
                                EligibilityDecision.excluded(
                                        EligibilityExclusionReason.INVALID_CONSENT),
                                EligibilityDecision.excluded(
                                        EligibilityExclusionReason.DO_NOT_CONTACT),
                                EligibilityDecision.included()));

        assertThat(summary).hasSize(3);
        assertThat(summary.get(0).code()).isEqualTo("DO_NOT_CONTACT");
        assertThat(summary.get(0).count()).isEqualTo(3);
        assertThat(summary.get(0).message())
                .isEqualTo(EligibilityExclusionReason.DO_NOT_CONTACT.explanation());
        assertThat(summary)
                .extracting(SegmentExclusionReasonSummary::code)
                .containsExactly("DO_NOT_CONTACT", "INVALID_CONSENT", "MARKETING_OPT_OUT");
        // INVALID_CONSENT and MARKETING_OPT_OUT both count 1 — sorted by code ascending
        assertThat(summary.get(1).count()).isEqualTo(1);
        assertThat(summary.get(2).count()).isEqualTo(1);
    }

    @Test
    void summarizeUsesUnknownCodeWhenReasonMissing() {
        List<SegmentExclusionReasonSummary> summary =
                SegmentExclusionReasonSummarySupport.summarize(
                        List.of(EligibilityDecision.excluded(null, null)));

        assertThat(summary).hasSize(1);
        assertThat(summary.getFirst().code())
                .isEqualTo(SegmentExclusionReasonSummarySupport.UNKNOWN_REASON_CODE);
        assertThat(summary.getFirst().message())
                .isEqualTo(SegmentExclusionReasonSummarySupport.UNKNOWN_REASON_MESSAGE);
        assertThat(summary.getFirst().count()).isEqualTo(1);
    }

    @Test
    void summarizeIgnoresNullDecisions() {
        List<SegmentExclusionReasonSummary> summary =
                SegmentExclusionReasonSummarySupport.summarize(
                        java.util.Arrays.asList(
                                null,
                                EligibilityDecision.excluded(
                                        EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT),
                                null));

        assertThat(summary).hasSize(1);
        assertThat(summary.getFirst().code()).isEqualTo("MONTHLY_CONTACT_LIMIT");
        assertThat(summary.getFirst().count()).isEqualTo(1);
    }
}
