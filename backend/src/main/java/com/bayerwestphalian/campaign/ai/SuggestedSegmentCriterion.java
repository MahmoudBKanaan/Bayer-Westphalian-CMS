package com.bayerwestphalian.campaign.ai;

import java.util.Objects;

/**
 * Structured segment criterion for AI-002 suggestions (compatible with the segment builder).
 *
 * <p>Human decision-support only — applying criteria still requires an explicit user save.
 */
public record SuggestedSegmentCriterion(
        String fieldName,
        String operator,
        String value,
        String logicalGroup,
        String joinOperator) {

    public SuggestedSegmentCriterion {
        Objects.requireNonNull(fieldName, "fieldName is required");
        Objects.requireNonNull(operator, "operator is required");
        Objects.requireNonNull(value, "value is required");
        if (joinOperator == null || joinOperator.isBlank()) {
            joinOperator = "AND";
        }
    }

    public static SuggestedSegmentCriterion equals(String fieldName, String value) {
        return new SuggestedSegmentCriterion(fieldName, "EQUALS", value, null, "AND");
    }

    public static SuggestedSegmentCriterion contains(String fieldName, String value) {
        return new SuggestedSegmentCriterion(fieldName, "CONTAINS", value, null, "AND");
    }

    public String toSummary() {
        return fieldName + " " + operator + " " + value;
    }
}
