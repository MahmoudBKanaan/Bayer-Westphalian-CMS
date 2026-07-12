package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SegmentExclusionReasonSummaryTests {

    @Test
    void createsSummaryWithTrimmedCodeAndMessage() {
        SegmentExclusionReasonSummary summary =
                SegmentExclusionReasonSummary.of(
                        "  DO_NOT_CONTACT  ", "  Customer has do-not-contact enabled  ", 3);

        assertThat(summary.code()).isEqualTo("DO_NOT_CONTACT");
        assertThat(summary.message()).isEqualTo("Customer has do-not-contact enabled");
        assertThat(summary.count()).isEqualTo(3);
    }

    @Test
    void defaultsBlankMessageToCode() {
        SegmentExclusionReasonSummary summary =
                SegmentExclusionReasonSummary.of("INVALID_CONSENT", "   ", 1);

        assertThat(summary.message()).isEqualTo("INVALID_CONSENT");
    }

    @Test
    void rejectsBlankCode() {
        assertThatThrownBy(() -> SegmentExclusionReasonSummary.of("  ", "message", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code");
    }

    @Test
    void rejectsNonPositiveCount() {
        assertThatThrownBy(() -> SegmentExclusionReasonSummary.of("DO_NOT_CONTACT", "message", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("count");
    }
}
