package com.bayerwestphalian.campaign.segment;

import org.springframework.util.StringUtils;

/**
 * Aggregated exclusion reason for segment preview (KB BR-006, FR-055): how many criteria-matched
 * customers were excluded for a given eligibility reason code.
 */
public record SegmentExclusionReasonSummary(String code, String message, int count) {

    public SegmentExclusionReasonSummary {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("exclusion reason code is required");
        }
        code = code.trim();
        if (!StringUtils.hasText(message)) {
            message = code;
        } else {
            message = message.trim();
        }
        if (count < 1) {
            throw new IllegalArgumentException("exclusion reason count must be at least 1");
        }
    }

    public static SegmentExclusionReasonSummary of(String code, String message, int count) {
        return new SegmentExclusionReasonSummary(code, message, count);
    }
}
