package com.bayerwestphalian.campaign.analytics;

import com.bayerwestphalian.campaign.product.ProductType;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Product performance analytics row (KB item 433 / E19 / GET {@code
 * /api/analytics/products/performance}).
 *
 * <p>Aggregates campaign metrics linked to a product for product performance reports: campaign
 * count, audience/eligible/sent/engagement totals, rates (opened|clicked|converted ÷ sent), and
 * estimated cost/revenue/ROI.
 */
public record ProductPerformanceView(
        UUID productId,
        String productName,
        ProductType productType,
        long campaignCount,
        long audienceSize,
        long eligibleCount,
        long sentCount,
        long openedCount,
        long clickedCount,
        long convertedCount,
        BigDecimal openRate,
        BigDecimal clickRate,
        BigDecimal conversionRate,
        BigDecimal estimatedCost,
        BigDecimal estimatedRevenue,
        BigDecimal estimatedRoi) {

    public ProductPerformanceView {
        Objects.requireNonNull(productId, "productId is required");
    }

    /**
     * Builds a product performance row with open/click/conversion rates from totals (KB item 433).
     *
     * <p>Rates use {@code sentCount} as the denominator when positive; otherwise zero. Prefer
     * passing a pre-computed {@code estimatedRoi} from aggregate cost/revenue.
     */
    public static ProductPerformanceView of(
            UUID productId,
            String productName,
            ProductType productType,
            long campaignCount,
            long audienceSize,
            long eligibleCount,
            long sentCount,
            long openedCount,
            long clickedCount,
            long convertedCount,
            BigDecimal estimatedCost,
            BigDecimal estimatedRevenue,
            BigDecimal estimatedRoi) {
        return new ProductPerformanceView(
                productId,
                productName,
                productType,
                campaignCount,
                audienceSize,
                eligibleCount,
                sentCount,
                openedCount,
                clickedCount,
                convertedCount,
                AnalyticsCalculations.calculateOpenRate(openedCount, sentCount),
                AnalyticsCalculations.calculateClickRate(clickedCount, sentCount),
                AnalyticsCalculations.calculateConversionRate(convertedCount, sentCount),
                estimatedCost,
                estimatedRevenue,
                estimatedRoi);
    }
}
