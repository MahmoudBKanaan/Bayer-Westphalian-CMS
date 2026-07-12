package com.bayerwestphalian.campaign.analytics;

import com.bayerwestphalian.campaign.campaign.CampaignMetrics;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;

/**
 * Shared KPI calculation helpers for analytics (KB E19).
 *
 * <p>Item 418 / acceptance item 447: eligible count is the number of campaign recipients with
 * status {@code ELIGIBLE}, stored on {@code campaign_metrics.eligible_count} and summed for
 * dashboard totals.
 *
 * <p>Item 419 / acceptance item 448: excluded count is the number of campaign recipients with
 * status {@code EXCLUDED}, stored on {@code campaign_metrics.excluded_count} and summed for
 * dashboard totals.
 *
 * <p>Item 420 / item 449 / FR-103: sent count is the number of messages sent (SENT contact events at
 * launch), stored on {@code campaign_metrics.sent_count} (updated by launch) and summed as
 * dashboard {@code messagesSent}.
 *
 * <p>Item 421 / item 450: opened count is the number of OPENED contact events (BR-034), stored on
 * {@code campaign_metrics.opened_count} and summed for dashboard / executive totals.
 *
 * <p>Item 422 / item 450: clicked count is the number of CLICKED contact events (BR-034), stored on
 * {@code campaign_metrics.clicked_count} and summed for dashboard / executive totals.
 *
 * <p>Item 423 / item 450: replied count is the number of REPLIED contact events (BR-034), stored on
 * {@code campaign_metrics.replied_count} and summed for dashboard / executive totals.
 *
 * <p>Item 424 / item 450: converted count is the number of conversion outcomes (BR-034), stored on
 * {@code campaign_metrics.converted_count} and summed for dashboard / executive totals.
 *
 * <p>Item 425 / item 451 / FR-104: open rate = opened / sent when sent &gt; 0; otherwise zero
 * (scale 4). Dashboard/executive use aggregate opened ÷ aggregate sent.
 *
 * <p>Item 426 / item 452 / FR-105: click rate = clicked / sent when sent &gt; 0; otherwise zero
 * (scale 4). Dashboard/executive use aggregate clicked ÷ aggregate sent.
 *
 * <p>Item 427 / item 453 / FR-106: conversion rate = converted / sent when sent &gt; 0; otherwise
 * zero (scale 4). Dashboard/executive use aggregate converted ÷ aggregate sent.
 *
 * <p>Item 428: estimated cost is an optional non-negative monetary value on {@code
 * campaign_metrics.estimated_cost}, normalized at scale 2 and summed for dashboard totals.
 *
 * <p>Item 429: estimated revenue is an optional non-negative monetary value on {@code
 * campaign_metrics.estimated_revenue}, normalized at scale 2 and summed for dashboard totals.
 *
 * <p>Item 430 / item 454 / FR-107: estimated ROI = (revenue − cost) / cost when cost &gt; 0; null
 * when cost missing; zero when cost is zero (scale 2). Dashboard/executive use aggregate cost and
 * revenue (not an average of per-campaign ROIs).
 *
 * <p>Item 417 / acceptance item 446: audience size is eligible + excluded (per campaign) and summed
 * across campaigns for dashboard totals (FR-102). Derived values are preferred over a possibly
 * stale stored {@code audience_size} column.
 */
public final class AnalyticsCalculations {

    private AnalyticsCalculations() {}

    /**
     * Per-campaign eligible count (KB item 418 / item 447).
     *
     * <p>Validates a non-negative count of {@code ELIGIBLE} recipients.
     */
    public static int calculateEligibleCount(int eligibleCount) {
        return CampaignMetrics.calculateEligibleCount(eligibleCount);
    }

    /**
     * Eligible count from a metrics row (KB item 418 / item 447 / {@code eligible_count}).
     */
    public static int calculateEligibleCount(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateEligibleCount();
    }

    /**
     * Converts a repository recipient count into an eligible metrics count (KB item 418 / item 447).
     */
    public static int calculateEligibleCount(long eligibleRecipientCount) {
        return CampaignMetrics.calculateEligibleCount(eligibleRecipientCount);
    }

    /**
     * Dashboard / executive total eligible count: sum of per-campaign eligible counts (KB item 418
     * / item 447).
     */
    public static long totalEligibleCount(Collection<CampaignMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (CampaignMetrics metrics : metricsList) {
            if (metrics != null) {
                total += calculateEligibleCount(metrics);
            }
        }
        return total;
    }

    /**
     * Per-campaign excluded count (KB item 419 / item 448).
     *
     * <p>Validates a non-negative count of {@code EXCLUDED} recipients.
     */
    public static int calculateExcludedCount(int excludedCount) {
        return CampaignMetrics.calculateExcludedCount(excludedCount);
    }

    /**
     * Excluded count from a metrics row (KB item 419 / item 448 / {@code excluded_count}).
     */
    public static int calculateExcludedCount(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateExcludedCount();
    }

    /**
     * Converts a repository recipient count into an excluded metrics count (KB item 419 / item 448).
     */
    public static int calculateExcludedCount(long excludedRecipientCount) {
        return CampaignMetrics.calculateExcludedCount(excludedRecipientCount);
    }

    /**
     * Dashboard / executive total excluded count: sum of per-campaign excluded counts (KB item 419
     * / item 448).
     */
    public static long totalExcludedCount(Collection<CampaignMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (CampaignMetrics metrics : metricsList) {
            if (metrics != null) {
                total += calculateExcludedCount(metrics);
            }
        }
        return total;
    }

    /**
     * Per-campaign sent count (KB item 420 / FR-103).
     *
     * <p>Validates a non-negative count of sent messages / SENT contact events.
     */
    public static int calculateSentCount(int sentCount) {
        return CampaignMetrics.calculateSentCount(sentCount);
    }

    /**
     * Sent count from a metrics row (KB item 420 / {@code sent_count}).
     */
    public static int calculateSentCount(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateSentCount();
    }

    /**
     * Converts a long send tally into a metrics sent count (KB item 420).
     */
    public static int calculateSentCount(long sentEventCount) {
        return CampaignMetrics.calculateSentCount(sentEventCount);
    }

    /**
     * Dashboard / executive total messages sent: sum of per-campaign sent counts (KB item 420 /
     * FR-103).
     */
    public static long totalSentCount(Collection<CampaignMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (CampaignMetrics metrics : metricsList) {
            if (metrics != null) {
                total += calculateSentCount(metrics);
            }
        }
        return total;
    }

    /**
     * Per-campaign opened count (KB item 421).
     *
     * <p>Validates a non-negative count of OPENED contact events.
     */
    public static int calculateOpenedCount(int openedCount) {
        return CampaignMetrics.calculateOpenedCount(openedCount);
    }

    /**
     * Opened count from a metrics row (KB item 421 / {@code opened_count}).
     */
    public static int calculateOpenedCount(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateOpenedCount();
    }

    /**
     * Converts a long open tally into a metrics opened count (KB item 421).
     */
    public static int calculateOpenedCount(long openedEventCount) {
        return CampaignMetrics.calculateOpenedCount(openedEventCount);
    }

    /**
     * Dashboard / executive total opened count: sum of per-campaign opened counts (KB item 421).
     */
    public static long totalOpenedCount(Collection<CampaignMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (CampaignMetrics metrics : metricsList) {
            if (metrics != null) {
                total += calculateOpenedCount(metrics);
            }
        }
        return total;
    }

    /**
     * Per-campaign clicked count (KB item 422).
     *
     * <p>Validates a non-negative count of CLICKED contact events.
     */
    public static int calculateClickedCount(int clickedCount) {
        return CampaignMetrics.calculateClickedCount(clickedCount);
    }

    /**
     * Clicked count from a metrics row (KB item 422 / {@code clicked_count}).
     */
    public static int calculateClickedCount(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateClickedCount();
    }

    /**
     * Converts a long click tally into a metrics clicked count (KB item 422).
     */
    public static int calculateClickedCount(long clickedEventCount) {
        return CampaignMetrics.calculateClickedCount(clickedEventCount);
    }

    /**
     * Dashboard / executive total clicked count: sum of per-campaign clicked counts (KB item 422).
     */
    public static long totalClickedCount(Collection<CampaignMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (CampaignMetrics metrics : metricsList) {
            if (metrics != null) {
                total += calculateClickedCount(metrics);
            }
        }
        return total;
    }

    /**
     * Per-campaign replied count (KB item 423).
     *
     * <p>Validates a non-negative count of REPLIED contact events.
     */
    public static int calculateRepliedCount(int repliedCount) {
        return CampaignMetrics.calculateRepliedCount(repliedCount);
    }

    /**
     * Replied count from a metrics row (KB item 423 / {@code replied_count}).
     */
    public static int calculateRepliedCount(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateRepliedCount();
    }

    /**
     * Converts a long reply tally into a metrics replied count (KB item 423).
     */
    public static int calculateRepliedCount(long repliedEventCount) {
        return CampaignMetrics.calculateRepliedCount(repliedEventCount);
    }

    /**
     * Dashboard / executive total replied count: sum of per-campaign replied counts (KB item 423).
     */
    public static long totalRepliedCount(Collection<CampaignMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (CampaignMetrics metrics : metricsList) {
            if (metrics != null) {
                total += calculateRepliedCount(metrics);
            }
        }
        return total;
    }

    /**
     * Per-campaign converted count (KB item 424).
     *
     * <p>Validates a non-negative count of conversion outcomes.
     */
    public static int calculateConvertedCount(int convertedCount) {
        return CampaignMetrics.calculateConvertedCount(convertedCount);
    }

    /**
     * Converted count from a metrics row (KB item 424 / {@code converted_count}).
     */
    public static int calculateConvertedCount(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateConvertedCount();
    }

    /**
     * Converts a long conversion tally into a metrics converted count (KB item 424).
     */
    public static int calculateConvertedCount(long convertedEventCount) {
        return CampaignMetrics.calculateConvertedCount(convertedEventCount);
    }

    /**
     * Dashboard / executive total converted count: sum of per-campaign converted counts (KB item
     * 424).
     */
    public static long totalConvertedCount(Collection<CampaignMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (CampaignMetrics metrics : metricsList) {
            if (metrics != null) {
                total += calculateConvertedCount(metrics);
            }
        }
        return total;
    }

    /**
     * Open rate = opened / sent when sent &gt; 0; otherwise zero at scale 4 (KB item 425 / item 451
     * / FR-104).
     */
    public static BigDecimal calculateOpenRate(int openedCount, int sentCount) {
        return CampaignMetrics.calculateOpenRate(openedCount, sentCount);
    }

    /**
     * Open rate from aggregate long counts (KB item 425 / item 451 / FR-104). Used for dashboard
     * and executive totals.
     */
    public static BigDecimal calculateOpenRate(long openedCount, long sentCount) {
        return CampaignMetrics.calculateOpenRate(openedCount, sentCount);
    }

    /**
     * Open rate from a metrics row (KB item 425 / item 451 / FR-104).
     */
    public static BigDecimal calculateOpenRate(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateOpenRate();
    }

    /**
     * Click rate = clicked / sent when sent &gt; 0; otherwise zero at scale 4 (KB item 426 / item
     * 452 / FR-105).
     */
    public static BigDecimal calculateClickRate(int clickedCount, int sentCount) {
        return CampaignMetrics.calculateClickRate(clickedCount, sentCount);
    }

    /**
     * Click rate from aggregate long counts (KB item 426 / item 452 / FR-105). Used for dashboard
     * and executive totals.
     */
    public static BigDecimal calculateClickRate(long clickedCount, long sentCount) {
        return CampaignMetrics.calculateClickRate(clickedCount, sentCount);
    }

    /**
     * Click rate from a metrics row (KB item 426 / item 452 / FR-105).
     */
    public static BigDecimal calculateClickRate(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateClickRate();
    }

    /**
     * Conversion rate = converted / sent when sent &gt; 0; otherwise zero at scale 4 (KB item 427 /
     * item 453 / FR-106).
     */
    public static BigDecimal calculateConversionRate(int convertedCount, int sentCount) {
        return CampaignMetrics.calculateConversionRate(convertedCount, sentCount);
    }

    /**
     * Conversion rate from aggregate long counts (KB item 427 / item 453 / FR-106). Used for
     * dashboard and executive totals.
     */
    public static BigDecimal calculateConversionRate(long convertedCount, long sentCount) {
        return CampaignMetrics.calculateConversionRate(convertedCount, sentCount);
    }

    /**
     * Conversion rate from a metrics row (KB item 427 / item 453 / FR-106).
     */
    public static BigDecimal calculateConversionRate(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateConversionRate();
    }

    /**
     * Validates and normalizes an estimated cost amount (KB item 428).
     *
     * @param estimatedCost optional non-negative cost; {@code null} allowed
     * @return normalized cost at scale 2, or {@code null}
     */
    public static BigDecimal calculateEstimatedCost(BigDecimal estimatedCost) {
        return CampaignMetrics.calculateEstimatedCost(estimatedCost);
    }

    /**
     * Estimated cost from a metrics row (KB item 428 / {@code estimated_cost}).
     */
    public static BigDecimal calculateEstimatedCost(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateEstimatedCost();
    }

    /**
     * Projects estimated cost as unit cost × quantity (KB item 428).
     */
    public static BigDecimal calculateEstimatedCost(BigDecimal unitCost, int quantity) {
        return CampaignMetrics.calculateEstimatedCost(unitCost, quantity);
    }

    /**
     * Dashboard / executive total estimated cost: sum of per-campaign estimated costs (KB item
     * 428).
     *
     * @return {@code null} when no campaign has a cost set; otherwise non-negative sum at scale 2
     */
    public static BigDecimal totalEstimatedCost(Collection<CampaignMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return null;
        }
        BigDecimal total = null;
        for (CampaignMetrics metrics : metricsList) {
            if (metrics == null) {
                continue;
            }
            BigDecimal cost = calculateEstimatedCost(metrics);
            if (cost == null) {
                continue;
            }
            total = total == null ? cost : total.add(cost);
        }
        return total;
    }

    /**
     * Validates and normalizes an estimated revenue amount (KB item 429).
     *
     * @param estimatedRevenue optional non-negative revenue; {@code null} allowed
     * @return normalized revenue at scale 2, or {@code null}
     */
    public static BigDecimal calculateEstimatedRevenue(BigDecimal estimatedRevenue) {
        return CampaignMetrics.calculateEstimatedRevenue(estimatedRevenue);
    }

    /**
     * Estimated revenue from a metrics row (KB item 429 / {@code estimated_revenue}).
     */
    public static BigDecimal calculateEstimatedRevenue(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateEstimatedRevenue();
    }

    /**
     * Projects estimated revenue as unit revenue × quantity (KB item 429).
     */
    public static BigDecimal calculateEstimatedRevenue(BigDecimal unitRevenue, int quantity) {
        return CampaignMetrics.calculateEstimatedRevenue(unitRevenue, quantity);
    }

    /**
     * Dashboard / executive total estimated revenue: sum of per-campaign estimated revenues (KB
     * item 429).
     *
     * @return {@code null} when no campaign has revenue set; otherwise non-negative sum at scale 2
     */
    public static BigDecimal totalEstimatedRevenue(Collection<CampaignMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return null;
        }
        BigDecimal total = null;
        for (CampaignMetrics metrics : metricsList) {
            if (metrics == null) {
                continue;
            }
            BigDecimal revenue = calculateEstimatedRevenue(metrics);
            if (revenue == null) {
                continue;
            }
            total = total == null ? revenue : total.add(revenue);
        }
        return total;
    }

    /**
     * Estimated ROI = (revenue − cost) / cost when cost &gt; 0; null when cost missing; zero when
     * cost is zero (KB item 430 / item 454 / FR-107).
     */
    public static BigDecimal calculateEstimatedRoi(
            BigDecimal estimatedCost, BigDecimal estimatedRevenue) {
        return CampaignMetrics.calculateEstimatedRoi(estimatedCost, estimatedRevenue);
    }

    /**
     * Estimated ROI from a metrics row (KB item 430 / item 454 / FR-107 / {@code estimated_roi}).
     */
    public static BigDecimal calculateEstimatedRoi(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateEstimatedRoi();
    }

    /**
     * Alias for {@link #calculateEstimatedRoi(BigDecimal, BigDecimal)} (KB {@code calculateRoi()}).
     */
    public static BigDecimal calculateRoi(BigDecimal estimatedCost, BigDecimal estimatedRevenue) {
        return CampaignMetrics.calculateRoi(estimatedCost, estimatedRevenue);
    }

    /**
     * ROI from a metrics row (KB item 430 / item 454 / FR-107).
     */
    public static BigDecimal calculateRoi(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateRoi();
    }

    /**
     * Dashboard / executive estimated ROI from aggregate cost and revenue totals (KB item 430 /
     * item 454 / FR-107).
     *
     * <p>Uses summed estimated cost and revenue across campaigns, not an average of per-campaign
     * ROIs.
     */
    public static BigDecimal totalEstimatedRoi(Collection<CampaignMetrics> metricsList) {
        return calculateEstimatedRoi(
                totalEstimatedCost(metricsList), totalEstimatedRevenue(metricsList));
    }

    /**
     * Per-campaign audience size (KB item 417).
     *
     * <p>{@code audience_size = eligible_count + excluded_count}.
     */
    public static int calculateAudienceSize(int eligibleCount, int excludedCount) {
        return CampaignMetrics.calculateAudienceSize(eligibleCount, excludedCount);
    }

    /**
     * Audience size from a metrics row, recomputed from eligible + excluded so aggregates stay
     * consistent even if a stored {@code audience_size} was stale.
     */
    public static int calculateAudienceSize(CampaignMetrics metrics) {
        Objects.requireNonNull(metrics, "metrics is required");
        return metrics.calculateAudienceSize();
    }

    /**
     * Dashboard / executive total audience size: sum of per-campaign audience sizes (KB FR-102).
     */
    public static long totalAudienceSize(Collection<CampaignMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (CampaignMetrics metrics : metricsList) {
            if (metrics != null) {
                total += calculateAudienceSize(metrics);
            }
        }
        return total;
    }
}
