package com.bayerwestphalian.campaign.analytics;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dashboard analytics payload (KB item 431 / FR-100–FR-107 / GET {@code /api/analytics/dashboard}).
 *
 * <p>Summarizes platform-wide campaign KPIs for Admin, BI Analyst, Campaign Manager, Marketing
 * Analyst, and Executive Viewer roles.
 *
 * <ul>
 *   <li>{@code campaignTotal} — FR-100
 *   <li>{@code activeCampaigns} — FR-101
 *   <li>{@code audienceSize} — FR-102
 *   <li>{@code messagesSent} — FR-103
 *   <li>{@code openRate} — FR-104
 *   <li>{@code clickRate} — FR-105
 *   <li>{@code conversionRate} — FR-106
 *   <li>{@code estimatedRoi} — FR-107
 * </ul>
 */
public record DashboardView(
        long campaignTotal,
        long activeCampaigns,
        long audienceSize,
        long messagesSent,
        long eligibleCount,
        long excludedCount,
        long openedCount,
        long clickedCount,
        long repliedCount,
        long convertedCount,
        BigDecimal openRate,
        BigDecimal clickRate,
        BigDecimal conversionRate,
        BigDecimal estimatedCost,
        BigDecimal estimatedRevenue,
        BigDecimal estimatedRoi,
        List<CampaignMetricsView> recentCampaignMetrics) {

    public DashboardView {
        recentCampaignMetrics =
                recentCampaignMetrics == null ? List.of() : List.copyOf(recentCampaignMetrics);
    }

    /** Empty dashboard with zeroed KPIs. */
    public static DashboardView empty() {
        return new DashboardView(
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
