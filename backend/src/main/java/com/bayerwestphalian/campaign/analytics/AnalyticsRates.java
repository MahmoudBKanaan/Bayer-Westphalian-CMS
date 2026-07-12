package com.bayerwestphalian.campaign.analytics;

import java.math.BigDecimal;

/**
 * Shared rate helpers for analytics DTOs (open / click / conversion / ROI).
 *
 * <p>Engagement rate denominator is messages sent. When sent is zero, rates are zero at scale 4.
 *
 * <p>KB item 425 / item 451 / FR-104: {@link #openRate(long, long)} delegates to {@link
 * AnalyticsCalculations#calculateOpenRate(long, long)}.
 *
 * <p>KB item 426 / item 452 / FR-105: {@link #clickRate(long, long)} delegates to {@link
 * AnalyticsCalculations#calculateClickRate(long, long)}.
 *
 * <p>KB item 427 / item 453 / FR-106: {@link #conversionRate(long, long)} delegates to {@link
 * AnalyticsCalculations#calculateConversionRate(long, long)}.
 *
 * <p>KB item 430 / item 454 / FR-107: {@link #roi(BigDecimal, BigDecimal)} delegates to {@link
 * AnalyticsCalculations#calculateEstimatedRoi(BigDecimal, BigDecimal)}.
 */
public final class AnalyticsRates {

    private AnalyticsRates() {}

    /**
     * Open rate = opened / sent when sent &gt; 0; otherwise zero (KB item 425 / item 451 / FR-104).
     */
    public static BigDecimal openRate(long openedCount, long sentCount) {
        return AnalyticsCalculations.calculateOpenRate(openedCount, sentCount);
    }

    /**
     * Click rate = clicked / sent when sent &gt; 0; otherwise zero (KB item 426 / item 452 / FR-105).
     */
    public static BigDecimal clickRate(long clickedCount, long sentCount) {
        return AnalyticsCalculations.calculateClickRate(clickedCount, sentCount);
    }

    /**
     * Conversion rate = converted / sent when sent &gt; 0; otherwise zero (KB item 427 / item 453 /
     * FR-106).
     */
    public static BigDecimal conversionRate(long convertedCount, long sentCount) {
        return AnalyticsCalculations.calculateConversionRate(convertedCount, sentCount);
    }

    /**
     * Estimated ROI = (revenue − cost) / cost when cost &gt; 0; null when cost missing; zero when
     * cost is zero (KB item 430 / item 454 / FR-107).
     */
    public static BigDecimal roi(BigDecimal estimatedCost, BigDecimal estimatedRevenue) {
        return AnalyticsCalculations.calculateEstimatedRoi(estimatedCost, estimatedRevenue);
    }
}
