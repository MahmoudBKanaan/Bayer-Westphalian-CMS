package com.bayerwestphalian.campaign.ai;

import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request for rule-based segment suggestions (KB AI-002 / {@code POST
 * /api/ai/segment-suggestions} / item 471 / AiRecommendationService.suggestSegments).
 *
 * <p>Optional seed fields guide suggestions; all fields may be null for general recommendations.
 */
public record SegmentSuggestionRequest(
        UUID customerId,
        @Size(max = 100) String city,
        @Size(max = 100) String country,
        @Size(max = 100) String productTypeHint,
        Integer expirationWithinMonths) {}
