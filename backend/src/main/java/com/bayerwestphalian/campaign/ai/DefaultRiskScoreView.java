package com.bayerwestphalian.campaign.ai;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Default-risk score payload (KB AI-004 / item 471 / item 478).
 *
 * <p>Score is derived from missed payments, overdue days, reminder count, and payment history.
 * {@code riskLevel} is a human-readable band (e.g. LOW / MEDIUM / HIGH) — not an automatic
 * marketing decision (COMP-005).
 */
public record DefaultRiskScoreView(
        UUID customerId,
        BigDecimal riskScore,
        String riskLevel,
        String explanation,
        List<ScoreExplanationView> factors,
        UUID storedRecommendationId) {

    public DefaultRiskScoreView {
        Objects.requireNonNull(customerId, "customerId is required");
        Objects.requireNonNull(riskScore, "riskScore is required");
        Objects.requireNonNull(riskLevel, "riskLevel is required");
        Objects.requireNonNull(explanation, "explanation is required");
        factors = factors == null ? List.of() : List.copyOf(factors);
    }
}
