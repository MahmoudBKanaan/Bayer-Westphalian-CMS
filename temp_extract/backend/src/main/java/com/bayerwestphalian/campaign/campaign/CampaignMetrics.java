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
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Aggregate campaign performance counters (KB {@code campaign_metrics}). */
@Entity
@Table(name = "campaign_metrics")
public class CampaignMetrics {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
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
    }

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

    public void recordLaunchCounts(int eligibleCount, int excludedCount, int sentCount) {
        this.eligibleCount = nonNegative(eligibleCount, "Eligible count");
        this.excludedCount = nonNegative(excludedCount, "Excluded count");
        this.sentCount = nonNegative(sentCount, "Sent count");
        this.audienceSize = this.eligibleCount + this.excludedCount;
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

    private static int nonNegative(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " must not be negative");
        }
        return value;
    }
}
