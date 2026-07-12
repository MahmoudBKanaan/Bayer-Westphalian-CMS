package com.bayerwestphalian.campaign.ai;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Single factor in an AI score explanation (KB {@code explainScore} / item 474 / AI-001).
 *
 * <p>Used by fuzzy customer search and risk scoring so operators can see why a score was produced
 * (human decision-support only — COMP-005).
 */
public record ScoreExplanationView(
        String factor, BigDecimal weight, BigDecimal contribution, String detail) {

    public ScoreExplanationView {
        Objects.requireNonNull(factor, "factor is required");
        factor = factor.trim();
        if (factor.isEmpty()) {
            throw new IllegalArgumentException("factor is required");
        }
        detail = detail == null ? null : detail.trim();
    }

    public static ScoreExplanationView of(
            String factor, BigDecimal weight, BigDecimal contribution, String detail) {
        return new ScoreExplanationView(factor, weight, contribution, detail);
    }
}
