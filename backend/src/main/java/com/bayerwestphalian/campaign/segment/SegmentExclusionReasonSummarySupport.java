package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Aggregates per-customer eligibility exclusions into a stable exclusion-reason summary for segment
 * preview (KB BR-006 / FR-055).
 */
final class SegmentExclusionReasonSummarySupport {

    static final String UNKNOWN_REASON_CODE = "UNKNOWN";
    static final String UNKNOWN_REASON_MESSAGE = "Customer excluded for an unspecified reason";

    private SegmentExclusionReasonSummarySupport() {}

    /**
     * Builds a summary list from ineligible decisions only. Results are ordered by descending
     * count, then ascending reason code for stable API output.
     */
    static List<SegmentExclusionReasonSummary> summarize(List<EligibilityDecision> decisions) {
        if (decisions == null || decisions.isEmpty()) {
            return List.of();
        }

        Map<String, MutableSummary> byCode = new LinkedHashMap<>();
        for (EligibilityDecision decision : decisions) {
            if (decision == null || decision.eligible()) {
                continue;
            }
            String code =
                    StringUtils.hasText(decision.exclusionReason())
                            ? decision.exclusionReason().trim()
                            : UNKNOWN_REASON_CODE;
            String message =
                    StringUtils.hasText(decision.eligibilityExplanation())
                            ? decision.eligibilityExplanation().trim()
                            : defaultMessageFor(code);
            MutableSummary summary =
                    byCode.computeIfAbsent(code, key -> new MutableSummary(message));
            // Keep the first non-blank message for the code.
            if (!StringUtils.hasText(summary.message) && StringUtils.hasText(message)) {
                summary.message = message;
            }
            summary.count++;
        }

        List<SegmentExclusionReasonSummary> result = new ArrayList<>(byCode.size());
        byCode.forEach(
                (code, summary) ->
                        result.add(
                                SegmentExclusionReasonSummary.of(
                                        code,
                                        StringUtils.hasText(summary.message)
                                                ? summary.message
                                                : defaultMessageFor(code),
                                        summary.count)));

        result.sort(
                Comparator.comparingInt(SegmentExclusionReasonSummary::count)
                        .reversed()
                        .thenComparing(SegmentExclusionReasonSummary::code));
        return List.copyOf(result);
    }

    private static String defaultMessageFor(String code) {
        if (UNKNOWN_REASON_CODE.equals(code)) {
            return UNKNOWN_REASON_MESSAGE;
        }
        return code;
    }

    private static final class MutableSummary {
        private String message;
        private int count;

        private MutableSummary(String message) {
            this.message = message;
        }
    }
}
