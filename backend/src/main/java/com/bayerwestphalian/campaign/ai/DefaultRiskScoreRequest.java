package com.bayerwestphalian.campaign.ai;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request for payment default-risk scoring (KB AI-004 / item 471 /
 * AiRecommendationService.calculateDefaultRisk).
 */
public record DefaultRiskScoreRequest(@NotNull UUID customerId) {}
