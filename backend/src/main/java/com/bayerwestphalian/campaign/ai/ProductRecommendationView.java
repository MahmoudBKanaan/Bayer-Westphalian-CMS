package com.bayerwestphalian.campaign.ai;

import com.bayerwestphalian.campaign.product.ProductType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Product recommendation row (KB AI-003 / item 471).
 *
 * <p>Includes product identity, suggestion text, required explanation, optional confidence, and
 * optional id of a persisted {@link AiRecommendation} row.
 */
public record ProductRecommendationView(
        UUID productId,
        String productName,
        ProductType productType,
        String recommendation,
        String explanation,
        BigDecimal confidenceScore,
        UUID storedRecommendationId) {

    public ProductRecommendationView {
        Objects.requireNonNull(productId, "productId is required");
        Objects.requireNonNull(recommendation, "recommendation is required");
        Objects.requireNonNull(explanation, "explanation is required");
    }

    /**
     * Envelope for a batch of product recommendations for one customer.
     */
    public record ListResponse(UUID customerId, List<ProductRecommendationView> recommendations) {
        public ListResponse {
            Objects.requireNonNull(customerId, "customerId is required");
            recommendations =
                    recommendations == null ? List.of() : List.copyOf(recommendations);
        }
    }
}
