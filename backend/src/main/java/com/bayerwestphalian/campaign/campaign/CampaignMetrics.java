package com.bayerwestphalian.campaign.campaign;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Campaign performance aggregate (KB entity {@code CampaignMetric} / table {@code
 * campaign_metrics}).
 *
 * <p>One row per campaign stores audience, delivery, engagement, conversion counters, and optional
 * estimated cost/revenue/ROI. Used by launch refresh (item 282), analytics (E19), and BR-034 metric
 * updates after contact events.
 *
 * <p>Factory: {@link #forCampaign(Campaign)}. Domain helpers: {@link #recordLaunchCounts}, {@link
 * #recordEngagementCounts}, {@link #updateFinancialEstimates}, {@link #recalculate}, rate and ROI
 * calculators.
 */
@Entity
@Table(name = "campaign_metrics")
public class CampaignMetrics {

    private static final int RATE_SCALE = 4;
    private static final int MONEY_SCALE = 2;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false, unique = true)
    private Campaign campaign;

    @Column(name = "audience_size", nullable = false)
    private int audienceSize;

    @Column(name = "eligible_count", nullable = false)
    private int eligibleCount;

    @Column(name = "excluded_count", nullable = false)
    private int excludedCount;

    @Column(name = "sent_count", nullable = false)
    private int sentCount;

    @Column(name = "opened_count", nullable = false)
    private int openedCount;

    @Column(name = "clicked_count", nullable = false)
    private int clickedCount;

    @Column(name = "replied_count", nullable = false)
    private int repliedCount;

    @Column(name = "converted_count", nullable = false)
    private int convertedCount;

    @Column(name = "estimated_cost", precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "estimated_revenue", precision = 12, scale = 2)
    private BigDecimal estimatedRevenue;

    @Column(name = "estimated_roi", precision = 12, scale = 2)
    private BigDecimal estimatedRoi;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CampaignMetrics() {}

    private CampaignMetrics(Campaign campaign) {
        this.campaign = Objects.requireNonNull(campaign, "Campaign is required");
        this.audienceSize = 0;
        this.eligibleCount = 0;
        this.excludedCount = 0;
        this.sentCount = 0;
        this.openedCount = 0;
        this.clickedCount = 0;
        this.repliedCount = 0;
        this.convertedCount = 0;
    }

    /** Creates zeroed metrics bound to a campaign (KB {@code CampaignMetric(campaign)}). */
    public static CampaignMetrics forCampaign(Campaign campaign) {
        return new CampaignMetrics(campaign);
    }

    public UUID getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public UUID getCampaignId() {
        return campaign == null ? null : campaign.getId();
    }

    public int getAudienceSize() {
        return audienceSize;
    }

    public int getEligibleCount() {
        return eligibleCount;
    }

    public int getExcludedCount() {
        return excludedCount;
    }

    public int getSentCount() {
        return sentCount;
    }

    public int getOpenedCount() {
        return openedCount;
    }

    public int getClickedCount() {
        return clickedCount;
    }

    public int getRepliedCount() {
        return repliedCount;
    }

    public int getConvertedCount() {
        return convertedCount;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public BigDecimal getEstimatedRevenue() {
        return estimatedRevenue;
    }

    public BigDecimal getEstimatedRoi() {
        return estimatedRoi;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Records audience and send counts at campaign launch.
     *
     * <p>KB item 418 / item 447: {@code eligible_count} is the number of recipients with eligibility
     * status {@code ELIGIBLE}.
     *
     * <p>KB item 419 / item 448: {@code excluded_count} is the number of recipients with eligibility
     * status {@code EXCLUDED} (consent, DNC, limits, etc.).
     *
     * <p>KB item 420 / item 449 / FR-103: {@code sent_count} is the number of messages successfully
     * queued or sent at launch (typically one SENT contact event per eligible recipient).
     *
     * <p>KB item 417 / item 446 / FR-102: {@code audience_size} is calculated as eligible + excluded
     * recipient counts.
     */
    public void recordLaunchCounts(int eligibleCount, int excludedCount, int sentCount) {
        this.eligibleCount = calculateEligibleCount(eligibleCount);
        this.excludedCount = calculateExcludedCount(excludedCount);
        this.sentCount = calculateSentCount(sentCount);
        this.audienceSize = calculateAudienceSize();
    }

    /**
     * Replaces engagement and conversion counters from contact-event aggregates (KB BR-034 / item
     * 450).
     *
     * <p>KB item 421: {@code opened_count} is the number of OPENED contact events (or equivalent
     * open tallies) recorded for the campaign.
     *
     * <p>KB item 422: {@code clicked_count} is the number of CLICKED contact events (or equivalent
     * click tallies) recorded for the campaign.
     *
     * <p>KB item 423: {@code replied_count} is the number of REPLIED contact events (or equivalent
     * reply tallies) recorded for the campaign.
     *
     * <p>KB item 424: {@code converted_count} is the number of conversion outcomes (or equivalent
     * conversion tallies) recorded for the campaign.
     */
    public void recordEngagementCounts(
            int openedCount, int clickedCount, int repliedCount, int convertedCount) {
        this.openedCount = calculateOpenedCount(openedCount);
        this.clickedCount = calculateClickedCount(clickedCount);
        this.repliedCount = calculateRepliedCount(repliedCount);
        this.convertedCount = calculateConvertedCount(convertedCount);
    }

    /**
     * Increments opened count by one after an OPENED contact event (KB item 421 / item 450 /
     * BR-034).
     */
    public void incrementOpened() {
        this.openedCount = calculateOpenedCount(this.openedCount + 1);
    }

    /**
     * Increments clicked count by one after a CLICKED contact event (KB item 422 / item 450 /
     * BR-034).
     */
    public void incrementClicked() {
        this.clickedCount = calculateClickedCount(this.clickedCount + 1);
    }

    /**
     * Increments replied count by one after a REPLIED contact event (KB item 423 / item 450 /
     * BR-034).
     */
    public void incrementReplied() {
        this.repliedCount = calculateRepliedCount(this.repliedCount + 1);
    }

    /**
     * Increments converted count by one after a conversion outcome (KB item 424 / item 450 /
     * BR-034).
     */
    public void incrementConverted() {
        this.convertedCount = calculateConvertedCount(this.convertedCount + 1);
    }

    /** Increments sent count by one after a successful send (KB item 420). */
    public void incrementSent() {
        this.sentCount = calculateSentCount(this.sentCount + 1);
    }

    /**
     * Stores estimated cost and revenue, then refreshes {@code estimated_roi} via {@link
     * #calculateRoi()}.
     *
     * <p>KB item 428: {@code estimated_cost} is normalized to a non-negative monetary value at
     * scale 2.
     *
     * <p>KB item 429: {@code estimated_revenue} is normalized to a non-negative monetary value at
     * scale 2.
     */
    public void updateFinancialEstimates(BigDecimal estimatedCost, BigDecimal estimatedRevenue) {
        this.estimatedCost = calculateEstimatedCost(estimatedCost);
        this.estimatedRevenue = calculateEstimatedRevenue(estimatedRevenue);
        this.estimatedRoi = calculateRoi();
    }

    /**
     * Recalculates derived fields: {@code audience_size = eligible + excluded} and {@code
     * estimated_roi} from cost/revenue (KB {@code recalculate()}).
     */
    public void recalculate() {
        this.audienceSize = calculateAudienceSize();
        this.estimatedRoi = calculateRoi();
    }

    /**
     * Eligible recipient count stored on this metrics row (KB item 418 / item 447 / {@code
     * campaign_metrics.eligible_count}).
     *
     * <p>Value is set at launch from {@code campaign_recipients} with status {@code ELIGIBLE}.
     */
    public int calculateEligibleCount() {
        return calculateEligibleCount(this.eligibleCount);
    }

    /**
     * Validates and returns a non-negative eligible count (KB item 418 / item 447).
     *
     * @param eligibleCount number of ELIGIBLE campaign recipients
     * @return the same count when non-negative
     */
    public static int calculateEligibleCount(int eligibleCount) {
        return nonNegative(eligibleCount, "Eligible count");
    }

    /**
     * Converts a recipient repository count into an eligible metrics count (KB item 418 / item 447).
     *
     * @param eligibleRecipientCount result of counting {@code ELIGIBLE} campaign recipients
     * @return non-negative int suitable for {@link #recordLaunchCounts}
     */
    public static int calculateEligibleCount(long eligibleRecipientCount) {
        if (eligibleRecipientCount < 0L) {
            throw new IllegalArgumentException("Eligible count must not be negative");
        }
        return Math.toIntExact(eligibleRecipientCount);
    }

    /**
     * Excluded recipient count stored on this metrics row (KB item 419 / item 448 / {@code
     * campaign_metrics.excluded_count}).
     *
     * <p>Value is set at launch from {@code campaign_recipients} with status {@code EXCLUDED}.
     */
    public int calculateExcludedCount() {
        return calculateExcludedCount(this.excludedCount);
    }

    /**
     * Validates and returns a non-negative excluded count (KB item 419 / item 448).
     *
     * @param excludedCount number of EXCLUDED campaign recipients
     * @return the same count when non-negative
     */
    public static int calculateExcludedCount(int excludedCount) {
        return nonNegative(excludedCount, "Excluded count");
    }

    /**
     * Converts a recipient repository count into an excluded metrics count (KB item 419 / item 448).
     *
     * @param excludedRecipientCount result of counting {@code EXCLUDED} campaign recipients
     * @return non-negative int suitable for {@link #recordLaunchCounts}
     */
    public static int calculateExcludedCount(long excludedRecipientCount) {
        if (excludedRecipientCount < 0L) {
            throw new IllegalArgumentException("Excluded count must not be negative");
        }
        return Math.toIntExact(excludedRecipientCount);
    }

    /**
     * Calculates audience size as eligible + excluded recipients (KB item 417 / item 446 / {@code
     * campaign_metrics.audience_size} / FR-102).
     *
     * <p>Does not mutate the entity; call {@link #recalculate()} or {@link #recordLaunchCounts} to
     * persist the derived value into {@code audienceSize}.
     */
    public int calculateAudienceSize() {
        return calculateAudienceSize(this.eligibleCount, this.excludedCount);
    }

    /**
     * Pure audience-size formula used at launch and for analytics aggregates (KB item 417 / item
     * 446).
     *
     * @param eligibleCount recipients marked eligible for the campaign
     * @param excludedCount recipients marked excluded (consent, DNC, limits, etc.)
     * @return non-negative sum of eligible and excluded counts
     */
    public static int calculateAudienceSize(int eligibleCount, int excludedCount) {
        return calculateEligibleCount(eligibleCount) + calculateExcludedCount(excludedCount);
    }

    /**
     * Sent message count stored on this metrics row (KB item 420 / item 449 / {@code
     * campaign_metrics.sent_count} / FR-103).
     *
     * <p>At launch, equals the number of SENT contact events created for eligible recipients.
     */
    public int calculateSentCount() {
        return calculateSentCount(this.sentCount);
    }

    /**
     * Validates and returns a non-negative sent count (KB item 420 / item 449).
     *
     * @param sentCount number of messages sent or queued for the campaign
     * @return the same count when non-negative
     */
    public static int calculateSentCount(int sentCount) {
        return nonNegative(sentCount, "Sent count");
    }

    /**
     * Converts a long send tally into a metrics sent count (KB item 420 / item 449).
     *
     * @param sentEventCount number of SENT contact events (or equivalent send tally)
     * @return non-negative int suitable for {@link #recordLaunchCounts}
     */
    public static int calculateSentCount(long sentEventCount) {
        if (sentEventCount < 0L) {
            throw new IllegalArgumentException("Sent count must not be negative");
        }
        return Math.toIntExact(sentEventCount);
    }

    /**
     * Opened message count stored on this metrics row (KB item 421 / {@code
     * campaign_metrics.opened_count}).
     *
     * <p>Updated from OPENED contact events (BR-034) via {@link #recordEngagementCounts} or {@link
     * #incrementOpened()}.
     */
    public int calculateOpenedCount() {
        return calculateOpenedCount(this.openedCount);
    }

    /**
     * Validates and returns a non-negative opened count (KB item 421).
     *
     * @param openedCount number of OPENED contact events (or equivalent open tally)
     * @return the same count when non-negative
     */
    public static int calculateOpenedCount(int openedCount) {
        return nonNegative(openedCount, "Opened count");
    }

    /**
     * Converts a long open tally into a metrics opened count (KB item 421).
     *
     * @param openedEventCount number of OPENED contact events (or equivalent open tally)
     * @return non-negative int suitable for {@link #recordEngagementCounts}
     */
    public static int calculateOpenedCount(long openedEventCount) {
        if (openedEventCount < 0L) {
            throw new IllegalArgumentException("Opened count must not be negative");
        }
        return Math.toIntExact(openedEventCount);
    }

    /**
     * Clicked message count stored on this metrics row (KB item 422 / {@code
     * campaign_metrics.clicked_count}).
     *
     * <p>Updated from CLICKED contact events (BR-034) via {@link #recordEngagementCounts} or {@link
     * #incrementClicked()}.
     */
    public int calculateClickedCount() {
        return calculateClickedCount(this.clickedCount);
    }

    /**
     * Validates and returns a non-negative clicked count (KB item 422).
     *
     * @param clickedCount number of CLICKED contact events (or equivalent click tally)
     * @return the same count when non-negative
     */
    public static int calculateClickedCount(int clickedCount) {
        return nonNegative(clickedCount, "Clicked count");
    }

    /**
     * Converts a long click tally into a metrics clicked count (KB item 422).
     *
     * @param clickedEventCount number of CLICKED contact events (or equivalent click tally)
     * @return non-negative int suitable for {@link #recordEngagementCounts}
     */
    public static int calculateClickedCount(long clickedEventCount) {
        if (clickedEventCount < 0L) {
            throw new IllegalArgumentException("Clicked count must not be negative");
        }
        return Math.toIntExact(clickedEventCount);
    }

    /**
     * Replied message count stored on this metrics row (KB item 423 / {@code
     * campaign_metrics.replied_count}).
     *
     * <p>Updated from REPLIED contact events (BR-034) via {@link #recordEngagementCounts} or {@link
     * #incrementReplied()}.
     */
    public int calculateRepliedCount() {
        return calculateRepliedCount(this.repliedCount);
    }

    /**
     * Validates and returns a non-negative replied count (KB item 423).
     *
     * @param repliedCount number of REPLIED contact events (or equivalent reply tally)
     * @return the same count when non-negative
     */
    public static int calculateRepliedCount(int repliedCount) {
        return nonNegative(repliedCount, "Replied count");
    }

    /**
     * Converts a long reply tally into a metrics replied count (KB item 423).
     *
     * @param repliedEventCount number of REPLIED contact events (or equivalent reply tally)
     * @return non-negative int suitable for {@link #recordEngagementCounts}
     */
    public static int calculateRepliedCount(long repliedEventCount) {
        if (repliedEventCount < 0L) {
            throw new IllegalArgumentException("Replied count must not be negative");
        }
        return Math.toIntExact(repliedEventCount);
    }

    /**
     * Converted count stored on this metrics row (KB item 424 / {@code
     * campaign_metrics.converted_count}).
     *
     * <p>Updated from conversion outcomes (BR-034) via {@link #recordEngagementCounts} or {@link
     * #incrementConverted()}.
     */
    public int calculateConvertedCount() {
        return calculateConvertedCount(this.convertedCount);
    }

    /**
     * Validates and returns a non-negative converted count (KB item 424).
     *
     * @param convertedCount number of conversion outcomes (or equivalent conversion tally)
     * @return the same count when non-negative
     */
    public static int calculateConvertedCount(int convertedCount) {
        return nonNegative(convertedCount, "Converted count");
    }

    /**
     * Converts a long conversion tally into a metrics converted count (KB item 424).
     *
     * @param convertedEventCount number of conversion outcomes (or equivalent conversion tally)
     * @return non-negative int suitable for {@link #recordEngagementCounts}
     */
    public static int calculateConvertedCount(long convertedEventCount) {
        if (convertedEventCount < 0L) {
            throw new IllegalArgumentException("Converted count must not be negative");
        }
        return Math.toIntExact(convertedEventCount);
    }

    /**
     * Open rate for this metrics row (KB item 425 / item 451 / FR-104 / {@code
     * calculateOpenRate()}).
     *
     * <p>{@code open_rate = opened_count / sent_count} when sent &gt; 0; otherwise {@link
     * BigDecimal#ZERO} at scale 4.
     */
    public BigDecimal calculateOpenRate() {
        return calculateOpenRate(calculateOpenedCount(), calculateSentCount());
    }

    /**
     * Pure open-rate formula (KB item 425 / item 451 / FR-104).
     *
     * @param openedCount OPENED contact-event tally (numerator)
     * @param sentCount messages sent tally (denominator)
     * @return opened / sent at scale 4, or zero when sent is 0
     */
    public static BigDecimal calculateOpenRate(int openedCount, int sentCount) {
        return calculateOpenRate(
                (long) calculateOpenedCount(openedCount), (long) calculateSentCount(sentCount));
    }

    /**
     * Pure open-rate formula for aggregate long counts (KB item 425 / FR-104).
     *
     * @param openedCount OPENED contact-event tally (numerator)
     * @param sentCount messages sent tally (denominator)
     * @return opened / sent at scale 4, or zero when sent is 0
     */
    public static BigDecimal calculateOpenRate(long openedCount, long sentCount) {
        if (openedCount < 0L) {
            throw new IllegalArgumentException("Opened count must not be negative");
        }
        if (sentCount < 0L) {
            throw new IllegalArgumentException("Sent count must not be negative");
        }
        return rate(openedCount, sentCount);
    }

    /**
     * Click rate for this metrics row (KB item 426 / item 452 / FR-105 / {@code
     * calculateClickRate()}).
     *
     * <p>{@code click_rate = clicked_count / sent_count} when sent &gt; 0; otherwise {@link
     * BigDecimal#ZERO} at scale 4.
     */
    public BigDecimal calculateClickRate() {
        return calculateClickRate(calculateClickedCount(), calculateSentCount());
    }

    /**
     * Pure click-rate formula (KB item 426 / item 452 / FR-105).
     *
     * @param clickedCount CLICKED contact-event tally (numerator)
     * @param sentCount messages sent tally (denominator)
     * @return clicked / sent at scale 4, or zero when sent is 0
     */
    public static BigDecimal calculateClickRate(int clickedCount, int sentCount) {
        return calculateClickRate(
                (long) calculateClickedCount(clickedCount), (long) calculateSentCount(sentCount));
    }

    /**
     * Pure click-rate formula for aggregate long counts (KB item 426 / item 452 / FR-105).
     *
     * @param clickedCount CLICKED contact-event tally (numerator)
     * @param sentCount messages sent tally (denominator)
     * @return clicked / sent at scale 4, or zero when sent is 0
     */
    public static BigDecimal calculateClickRate(long clickedCount, long sentCount) {
        if (clickedCount < 0L) {
            throw new IllegalArgumentException("Clicked count must not be negative");
        }
        if (sentCount < 0L) {
            throw new IllegalArgumentException("Sent count must not be negative");
        }
        return rate(clickedCount, sentCount);
    }

    /**
     * Conversion rate for this metrics row (KB item 427 / item 453 / FR-106 / {@code
     * calculateConversionRate()}).
     *
     * <p>{@code conversion_rate = converted_count / sent_count} when sent &gt; 0; otherwise {@link
     * BigDecimal#ZERO} at scale 4.
     */
    public BigDecimal calculateConversionRate() {
        return calculateConversionRate(calculateConvertedCount(), calculateSentCount());
    }

    /**
     * Pure conversion-rate formula (KB item 427 / item 453 / FR-106).
     *
     * @param convertedCount conversion-outcome tally (numerator)
     * @param sentCount messages sent tally (denominator)
     * @return converted / sent at scale 4, or zero when sent is 0
     */
    public static BigDecimal calculateConversionRate(int convertedCount, int sentCount) {
        return calculateConversionRate(
                (long) calculateConvertedCount(convertedCount),
                (long) calculateSentCount(sentCount));
    }

    /**
     * Pure conversion-rate formula for aggregate long counts (KB item 427 / item 453 / FR-106).
     *
     * @param convertedCount conversion-outcome tally (numerator)
     * @param sentCount messages sent tally (denominator)
     * @return converted / sent at scale 4, or zero when sent is 0
     */
    public static BigDecimal calculateConversionRate(long convertedCount, long sentCount) {
        if (convertedCount < 0L) {
            throw new IllegalArgumentException("Converted count must not be negative");
        }
        if (sentCount < 0L) {
            throw new IllegalArgumentException("Sent count must not be negative");
        }
        return rate(convertedCount, sentCount);
    }

    /**
     * Estimated cost stored on this metrics row (KB item 428 / {@code
     * campaign_metrics.estimated_cost}).
     *
     * <p>Optional monetary estimate; {@code null} when not set. When present, non-negative at scale
     * 2.
     */
    public BigDecimal calculateEstimatedCost() {
        return calculateEstimatedCost(this.estimatedCost);
    }

    /**
     * Validates and normalizes an estimated cost value (KB item 428).
     *
     * @param estimatedCost optional non-negative cost; {@code null} allowed
     * @return {@code null} when input is null; otherwise scale-2 non-negative amount
     */
    public static BigDecimal calculateEstimatedCost(BigDecimal estimatedCost) {
        return normalizeMoney(estimatedCost, "Estimated cost");
    }

    /**
     * Calculates estimated cost as unit cost × quantity (KB item 428 helper).
     *
     * <p>Useful when projecting campaign cost from a per-message or per-recipient unit cost and a
     * send/eligible quantity.
     *
     * @param unitCost non-negative cost per unit (required)
     * @param quantity non-negative unit count (e.g. messages sent)
     * @return non-negative product at scale 2
     */
    public static BigDecimal calculateEstimatedCost(BigDecimal unitCost, int quantity) {
        if (unitCost == null) {
            throw new IllegalArgumentException("Unit cost is required");
        }
        BigDecimal normalizedUnit = normalizeMoney(unitCost, "Unit cost");
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must not be negative");
        }
        return normalizedUnit
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Estimated revenue stored on this metrics row (KB item 429 / {@code
     * campaign_metrics.estimated_revenue}).
     *
     * <p>Optional monetary estimate; {@code null} when not set. When present, non-negative at scale
     * 2.
     */
    public BigDecimal calculateEstimatedRevenue() {
        return calculateEstimatedRevenue(this.estimatedRevenue);
    }

    /**
     * Validates and normalizes an estimated revenue value (KB item 429).
     *
     * @param estimatedRevenue optional non-negative revenue; {@code null} allowed
     * @return {@code null} when input is null; otherwise scale-2 non-negative amount
     */
    public static BigDecimal calculateEstimatedRevenue(BigDecimal estimatedRevenue) {
        return normalizeMoney(estimatedRevenue, "Estimated revenue");
    }

    /**
     * Calculates estimated revenue as unit revenue × quantity (KB item 429 helper).
     *
     * <p>Useful when projecting campaign revenue from a per-conversion or per-recipient unit
     * revenue and a conversion/eligible quantity.
     *
     * @param unitRevenue non-negative revenue per unit (required)
     * @param quantity non-negative unit count (e.g. conversions)
     * @return non-negative product at scale 2
     */
    public static BigDecimal calculateEstimatedRevenue(BigDecimal unitRevenue, int quantity) {
        if (unitRevenue == null) {
            throw new IllegalArgumentException("Unit revenue is required");
        }
        BigDecimal normalizedUnit = normalizeMoney(unitRevenue, "Unit revenue");
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must not be negative");
        }
        return normalizedUnit
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Estimated ROI for this metrics row (KB item 430 / item 454 / FR-107 / {@code calculateRoi()}).
     *
     * <p>{@code estimated_roi = (estimated_revenue − estimated_cost) / estimated_cost} when cost
     * &gt; 0; {@code null} when cost is missing; zero when cost is zero. Scale 2.
     */
    public BigDecimal calculateEstimatedRoi() {
        return calculateRoi();
    }

    /**
     * Pure estimated-ROI formula (KB item 430 / item 454 / FR-107).
     *
     * @param estimatedCost optional non-negative cost (denominator when present)
     * @param estimatedRevenue optional non-negative revenue (numerator side)
     * @return ROI at scale 2, zero when cost is zero, or {@code null} when cost is null
     */
    public static BigDecimal calculateEstimatedRoi(
            BigDecimal estimatedCost, BigDecimal estimatedRevenue) {
        return calculateRoi(estimatedCost, estimatedRevenue);
    }

    /**
     * ROI = (revenue − cost) / cost when cost &gt; 0; {@code null} when cost is missing; zero when
     * cost is zero (KB item 430 / item 454 / FR-107 / {@code calculateRoi()}).
     */
    public BigDecimal calculateRoi() {
        return calculateRoi(calculateEstimatedCost(), calculateEstimatedRevenue());
    }

    /**
     * Pure ROI formula from cost and revenue (KB item 430 / item 454 / FR-107).
     *
     * @param estimatedCost optional non-negative cost
     * @param estimatedRevenue optional non-negative revenue
     * @return ROI at scale 2, zero when cost is zero, or {@code null} when cost is null
     */
    public static BigDecimal calculateRoi(BigDecimal estimatedCost, BigDecimal estimatedRevenue) {
        BigDecimal cost = calculateEstimatedCost(estimatedCost);
        if (cost == null) {
            return null;
        }
        if (cost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal revenue = calculateEstimatedRevenue(estimatedRevenue);
        if (revenue == null) {
            revenue = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return revenue.subtract(cost).divide(cost, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        touch();
    }

    @PreUpdate
    protected void onUpdate() {
        touch();
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    private static BigDecimal rate(int numerator, int denominator) {
        return rate((long) numerator, (long) denominator);
    }

    private static BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0L) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), RATE_SCALE, RoundingMode.HALF_UP);
    }

    private static int nonNegative(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " must not be negative");
        }
        return value;
    }

    private static BigDecimal normalizeMoney(BigDecimal value, String label) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + " must not be negative");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
