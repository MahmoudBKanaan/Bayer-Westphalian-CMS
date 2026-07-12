package com.bayerwestphalian.campaign.ai;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * API view of a stored {@link AiRecommendation} (KB item 471 / entity item 469 / E21).
 *
 * <p>Exposes recommendation type, target, input summary, suggestion text, required explanation
 * (COMP-005), optional confidence, human approver, and created timestamp.
 */
public record AiRecommendationView(
        UUID id,
        AiRecommendationType recommendationType,
        String targetEntityType,
        UUID targetEntityId,
        String inputSummary,
        String recommendation,
        String explanation,
        BigDecimal confidenceScore,
        UUID approvedByUserId,
        String approvedByFullName,
        String reviewNotes,
        boolean approved,
        Instant createdAt) {

    public static AiRecommendationView from(AiRecommendation entity) {
        Objects.requireNonNull(entity, "recommendation is required");
        return new AiRecommendationView(
                entity.getId(),
                entity.getRecommendationType(),
                entity.getTargetEntityType(),
                entity.getTargetEntityId(),
                entity.getInputSummary(),
                entity.getRecommendation(),
                entity.getExplanation(),
                entity.getConfidenceScore(),
                entity.getApprovedByUserId(),
                entity.getApprovedBy() == null ? null : entity.getApprovedBy().getFullName(),
                entity.getReviewNotes(),
                entity.isApproved(),
                entity.getCreatedAt());
    }
}
