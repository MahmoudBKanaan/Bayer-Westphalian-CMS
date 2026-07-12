package com.bayerwestphalian.campaign.analytics;

import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignMetrics;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * API view of a single campaign's performance counters and rates (KB {@code CampaignMetric} /
 * analytics E19).
 *
 * <p>Rates are derived via {@link CampaignMetrics#calculateOpenRate()} (KB item 425 / item 451 /
 * FR-104), {@link CampaignMetrics#calculateClickRate()} (KB item 426 / item 452 / FR-105), {@link
 * CampaignMetrics#calculateConversionRate()} (KB item 427 / item 453 / FR-106), and {@link
 * CampaignMetrics#calculateEstimatedRoi()} (KB item 430 / item 454 / FR-107).
 */
public record CampaignMetricsView(
        UUID metricsId,
        UUID campaignId,
        String campaignName,
        CampaignStatus campaignStatus,
        int audienceSize,
        int eligibleCount,
        int excludedCount,
        int sentCount,
        int openedCount,
        int clickedCount,
        int repliedCount,
        int convertedCount,
        BigDecimal openRate,
        BigDecimal clickRate,
        BigDecimal conversionRate,
        BigDecimal estimatedCost,
        BigDecimal estimatedRevenue,
        BigDecimal estimatedRoi,
        Instant updatedAt) {

    public static CampaignMetricsView from(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        Campaign campaign = metrics.getCampaign();
        // KB item 417: expose calculated audience size (eligible + excluded).
        int audienceSize = metrics.calculateAudienceSize();
        // KB item 418 / item 447: expose calculated/validated eligible count.
        int eligibleCount = metrics.calculateEligibleCount();
        // KB item 419 / item 448: expose calculated/validated excluded count.
        int excludedCount = metrics.calculateExcludedCount();
        // KB item 420: expose calculated/validated sent count.
        int sentCount = metrics.calculateSentCount();
        // KB item 421: expose calculated/validated opened count.
        int openedCount = metrics.calculateOpenedCount();
        // KB item 422: expose calculated/validated clicked count.
        int clickedCount = metrics.calculateClickedCount();
        // KB item 423: expose calculated/validated replied count.
        int repliedCount = metrics.calculateRepliedCount();
        // KB item 424: expose calculated/validated converted count.
        int convertedCount = metrics.calculateConvertedCount();
        // KB item 425 / item 451 / FR-104: open rate = opened / sent.
        BigDecimal openRate = metrics.calculateOpenRate();
        // KB item 426 / item 452 / FR-105: click rate = clicked / sent.
        BigDecimal clickRate = metrics.calculateClickRate();
        // KB item 427 / item 453 / FR-106: conversion rate = converted / sent.
        BigDecimal conversionRate = metrics.calculateConversionRate();
        // KB item 428: expose calculated/validated estimated cost.
        BigDecimal estimatedCost = metrics.calculateEstimatedCost();
        // KB item 429: expose calculated/validated estimated revenue.
        BigDecimal estimatedRevenue = metrics.calculateEstimatedRevenue();
        // KB item 430 / item 454 / FR-107: estimated ROI = (revenue − cost) / cost.
        BigDecimal estimatedRoi = metrics.calculateEstimatedRoi();
        return new CampaignMetricsView(
                metrics.getId(),
                metrics.getCampaignId(),
                campaign == null ? null : campaign.getName(),
                campaign == null ? null : campaign.getStatus(),
                audienceSize,
                eligibleCount,
                excludedCount,
                sentCount,
                openedCount,
                clickedCount,
                repliedCount,
                convertedCount,
                openRate,
                clickRate,
                conversionRate,
                estimatedCost,
                estimatedRevenue,
                estimatedRoi != null ? estimatedRoi : metrics.getEstimatedRoi(),
                metrics.getUpdatedAt());
    }
}
