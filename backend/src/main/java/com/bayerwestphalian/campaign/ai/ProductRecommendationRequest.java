package com.bayerwestphalian.campaign.ai;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request for rule-based product recommendations (KB AI-003 / {@code POST
 * /api/ai/product-recommendations} / item 471 / AiRecommendationService.recommendProducts).
 */
public record ProductRecommendationRequest(@NotNull UUID customerId) {}
