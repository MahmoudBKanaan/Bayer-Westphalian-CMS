package com.bayerwestphalian.campaign.analytics;

import java.math.BigDecimal;
import java.util.List;

/**
 * Executive aggregate dashboard payload (KB item 434 / item 457 / COMP-010 / GET {@code
 * /api/analytics/executive}).
 *
 * <p>Platform-level aggregated KPIs for Executive Viewer and other analytics roles: campaign
 * inventory (total/active/completed), audience funnel, engagement totals, rates and ROI derived
 * from aggregates, and optional product performance summary rows. COMP-010 / acceptance item 457
 * prefer these aggregates over raw contact-event detail for management reports.
 */
public record ExecutiveDashboardView(
        long totalCampaigns,
        long activeCampaigns,
        long completedCampaigns,
        long totalAudience,
        long totalEligible,
        long totalExcluded,
        long totalSent,
        long totalOpened,
        long totalClicked,
        long totalReplied,
        long totalConverted,
        BigDecimal overallOpenRate,
        BigDecimal overallClickRate,
        BigDecimal overallConversionRate,
        BigDecimal totalEstimatedCost,
        BigDecimal totalEstimatedRevenue,
        BigDecimal overallEstimatedRoi,
        List<ProductPerformanceView> productPerformance) {

    public ExecutiveDashboardView {
        productPerformance =
                productPerformance == null ? List.of() : List.copyOf(productPerformance);
    }

    /** Empty executive dashboard with zeroed aggregates. */
    public static ExecutiveDashboardView empty() {
        return new ExecutiveDashboardView(
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                0L,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null,
                List.of());
    }
}
