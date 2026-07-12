package com.bayerwestphalian.campaign.analytics;

import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignMetrics;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Campaign analytics detail (KB item 432 / E19 / GET {@code
 * /api/analytics/campaigns/{campaignId}}).
 *
 * <p>Combines campaign identity with full {@link CampaignMetricsView} counters and rates for a
 * single campaign performance drill-down. {@code metrics} may be {@code null} when no {@code
 * campaign_metrics} row exists yet.
 */
public record CampaignAnalyticsView(
        UUID campaignId,
        String campaignName,
        String objective,
        CampaignStatus status,
        CampaignChannel channel,
        LocalDate startDate,
        LocalDate endDate,
        UUID ownerUserId,
        String ownerFullName,
        CampaignMetricsView metrics,
        Instant generatedAt) {

    public static CampaignAnalyticsView of(
            UUID campaignId,
            String campaignName,
            String objective,
            CampaignStatus status,
            CampaignChannel channel,
            LocalDate startDate,
            LocalDate endDate,
            UUID ownerUserId,
            String ownerFullName,
            CampaignMetrics metrics) {
        Objects.requireNonNull(campaignId, "campaignId is required");
        CampaignMetricsView metricsView =
                metrics == null ? null : CampaignMetricsView.from(metrics);
        return new CampaignAnalyticsView(
                campaignId,
                campaignName,
                objective,
                status,
                channel,
                startDate,
                endDate,
                ownerUserId,
                ownerFullName,
                metricsView,
                Instant.now());
    }

    public static CampaignAnalyticsView of(
            UUID campaignId,
            String campaignName,
            String objective,
            CampaignStatus status,
            CampaignChannel channel,
            LocalDate startDate,
            LocalDate endDate,
            UUID ownerUserId,
            String ownerFullName,
            CampaignMetricsView metricsView) {
        Objects.requireNonNull(campaignId, "campaignId is required");
        return new CampaignAnalyticsView(
                campaignId,
                campaignName,
                objective,
                status,
                channel,
                startDate,
                endDate,
                ownerUserId,
                ownerFullName,
                metricsView,
                Instant.now());
    }
}
