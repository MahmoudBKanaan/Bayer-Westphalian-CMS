package com.bayerwestphalian.campaign.ai;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request for duplicate-contact risk warning (KB AI-006 / item 471 / item 479 /
 * AiRecommendationService.detectDuplicateRisk).
 */
public record DuplicateContactRiskRequest(@NotNull UUID customerId, UUID campaignId) {}
