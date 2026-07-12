package com.bayerwestphalian.campaign.ai;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Segment suggestion payload (KB AI-002 / item 471).
 *
 * <p>Human decision-support only — does not create a saved segment until a Campaign Manager acts.
 */
public record SegmentSuggestionView(
        String suggestedName,
        String description,
        List<String> suggestedCriteriaSummary,
        String explanation,
        BigDecimal confidenceScore,
        UUID storedRecommendationId) {

    public SegmentSuggestionView {
        Objects.requireNonNull(suggestedName, "suggestedName is required");
        Objects.requireNonNull(explanation, "explanation is required");
        suggestedCriteriaSummary =
                suggestedCriteriaSummary == null
                        ? List.of()
                        : List.copyOf(suggestedCriteriaSummary);
    }

    public record ListResponse(List<SegmentSuggestionView> suggestions) {
        public ListResponse {
            suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
        }
    }
}
