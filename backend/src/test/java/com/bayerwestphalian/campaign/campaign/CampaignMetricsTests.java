package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 412: CampaignMetric entity maps {@code campaign_metrics} and exposes counters, financial
 * estimates, recalculate, and rate/ROI helpers.
 */
class CampaignMetricsTests {

    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000412");

    @Test
    void mapsKbCampaignMetricsTableAsJpaEntity() throws Exception {
        assertThat(CampaignMetrics.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(CampaignMetrics.class.getAnnotation(Table.class).name())
                .isEqualTo("campaign_metrics");
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<CampaignMetrics> constructor = CampaignMetrics.class.getDeclaredConstructor();
        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsAllKbCampaignMetricColumns() throws Exception {
        assertColumn("id", "id", false);
        assertThat(field("id").isAnnotationPresent(Id.class)).isTrue();
        assertColumn("audienceSize", "audience_size", false);
        assertColumn("eligibleCount", "eligible_count", false);
        assertColumn("excludedCount", "excluded_count", false);
        assertColumn("sentCount", "sent_count", false);
        assertColumn("openedCount", "opened_count", false);
        assertColumn("clickedCount", "clicked_count", false);
        assertColumn("repliedCount", "replied_count", false);
        assertColumn("convertedCount", "converted_count", false);
        assertColumn("estimatedCost", "estimated_cost", true);
        assertColumn("estimatedRevenue", "estimated_revenue", true);
        assertColumn("estimatedRoi", "estimated_roi", true);
        assertColumn("updatedAt", "updated_at", false);

        assertThat(field("estimatedCost").getAnnotation(Column.class).precision()).isEqualTo(12);
        assertThat(field("estimatedCost").getAnnotation(Column.class).scale()).isEqualTo(2);
        assertThat(field("estimatedRevenue").getAnnotation(Column.class).precision()).isEqualTo(12);
        assertThat(field("estimatedRoi").getAnnotation(Column.class).precision()).isEqualTo(12);
    }

    @Test
    void mapsCampaignAsRequiredUniqueOneToOneRelationship() throws Exception {
        Field campaign = field("campaign");
        OneToOne oneToOne = campaign.getAnnotation(OneToOne.class);
        JoinColumn joinColumn = campaign.getAnnotation(JoinColumn.class);

        assertThat(oneToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(oneToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo("campaign_id");
        assertThat(joinColumn.nullable()).isFalse();
        assertThat(joinColumn.unique()).isTrue();
        assertThat(campaign.isAnnotationPresent(jakarta.validation.constraints.NotNull.class))
                .isTrue();
    }

    @Test
    void forCampaignCreatesZeroedMetricsBoundToCampaign() {
        Campaign campaign = campaign();
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);

        assertThat(metrics.getCampaign()).isSameAs(campaign);
        assertThat(metrics.getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(metrics.getAudienceSize()).isZero();
        assertThat(metrics.getEligibleCount()).isZero();
        assertThat(metrics.getExcludedCount()).isZero();
        assertThat(metrics.getSentCount()).isZero();
        assertThat(metrics.getOpenedCount()).isZero();
        assertThat(metrics.getClickedCount()).isZero();
        assertThat(metrics.getRepliedCount()).isZero();
        assertThat(metrics.getConvertedCount()).isZero();
        assertThat(metrics.getEstimatedCost()).isNull();
        assertThat(metrics.getEstimatedRevenue()).isNull();
        assertThat(metrics.getEstimatedRoi()).isNull();
    }

    @Test
    void forCampaignRequiresCampaign() {
        assertThatThrownBy(() -> CampaignMetrics.forCampaign(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Campaign is required");
    }

    @Test
    void recordsLaunchCountsAndAudienceSize() {
        // KB item 418: eligible_count from ELIGIBLE recipients.
        // KB item 419: excluded_count from EXCLUDED recipients.
        // KB item 417: audience_size = eligible_count + excluded_count.
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());

        metrics.recordLaunchCounts(8, 2, 8);

        assertThat(metrics.getCampaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(metrics.calculateEligibleCount()).isEqualTo(8);
        assertThat(metrics.getEligibleCount()).isEqualTo(8);
        assertThat(CampaignMetrics.calculateEligibleCount(8)).isEqualTo(8);
        assertThat(CampaignMetrics.calculateEligibleCount(8L)).isEqualTo(8);
        assertThat(metrics.calculateExcludedCount()).isEqualTo(2);
        assertThat(metrics.getExcludedCount()).isEqualTo(2);
        assertThat(CampaignMetrics.calculateExcludedCount(2)).isEqualTo(2);
        assertThat(CampaignMetrics.calculateExcludedCount(2L)).isEqualTo(2);
        assertThat(metrics.calculateSentCount()).isEqualTo(8);
        assertThat(metrics.getSentCount()).isEqualTo(8);
        assertThat(CampaignMetrics.calculateSentCount(8)).isEqualTo(8);
        assertThat(CampaignMetrics.calculateSentCount(8L)).isEqualTo(8);
        assertThat(metrics.calculateAudienceSize()).isEqualTo(10);
        assertThat(metrics.getAudienceSize()).isEqualTo(10);
        assertThat(CampaignMetrics.calculateAudienceSize(8, 2)).isEqualTo(10);
    }

    @Test
    void rejectsNegativeLaunchCounts() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> metrics.recordLaunchCounts(-1, 0, 0))
                .withMessageContaining("Eligible count must not be negative");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> metrics.recordLaunchCounts(0, -1, 0))
                .withMessageContaining("Excluded count must not be negative");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> metrics.recordLaunchCounts(0, 0, -1))
                .withMessageContaining("Sent count must not be negative");
    }

    @Test
    void recordsEngagementCountsFromContactEvents() {
        // KB item 421: opened_count from OPENED contact events (BR-034).
        // KB item 422: clicked_count from CLICKED contact events (BR-034).
        // KB item 423: replied_count from REPLIED contact events (BR-034).
        // KB item 424: converted_count from conversion outcomes (BR-034).
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());
        metrics.recordLaunchCounts(10, 0, 10);

        metrics.recordEngagementCounts(5, 3, 2, 1);

        assertThat(metrics.calculateOpenedCount()).isEqualTo(5);
        assertThat(metrics.getOpenedCount()).isEqualTo(5);
        assertThat(CampaignMetrics.calculateOpenedCount(5)).isEqualTo(5);
        assertThat(CampaignMetrics.calculateOpenedCount(5L)).isEqualTo(5);
        assertThat(metrics.calculateClickedCount()).isEqualTo(3);
        assertThat(metrics.getClickedCount()).isEqualTo(3);
        assertThat(CampaignMetrics.calculateClickedCount(3)).isEqualTo(3);
        assertThat(CampaignMetrics.calculateClickedCount(3L)).isEqualTo(3);
        assertThat(metrics.calculateRepliedCount()).isEqualTo(2);
        assertThat(metrics.getRepliedCount()).isEqualTo(2);
        assertThat(CampaignMetrics.calculateRepliedCount(2)).isEqualTo(2);
        assertThat(CampaignMetrics.calculateRepliedCount(2L)).isEqualTo(2);
        assertThat(metrics.calculateConvertedCount()).isEqualTo(1);
        assertThat(metrics.getConvertedCount()).isEqualTo(1);
        assertThat(CampaignMetrics.calculateConvertedCount(1)).isEqualTo(1);
        assertThat(CampaignMetrics.calculateConvertedCount(1L)).isEqualTo(1);
    }

    @Test
    void rejectsNegativeEngagementCounts() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> metrics.recordEngagementCounts(-1, 0, 0, 0))
                .withMessageContaining("Opened count must not be negative");
    }

    @Test
    void incrementsEngagementAndSentCounters() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());

        metrics.incrementSent();
        metrics.incrementOpened();
        metrics.incrementClicked();
        metrics.incrementReplied();
        metrics.incrementConverted();

        assertThat(metrics.calculateSentCount()).isEqualTo(1);
        assertThat(metrics.getSentCount()).isEqualTo(1);
        assertThat(metrics.calculateOpenedCount()).isEqualTo(1);
        assertThat(metrics.getOpenedCount()).isEqualTo(1);
        assertThat(metrics.calculateClickedCount()).isEqualTo(1);
        assertThat(metrics.getClickedCount()).isEqualTo(1);
        assertThat(metrics.calculateRepliedCount()).isEqualTo(1);
        assertThat(metrics.getRepliedCount()).isEqualTo(1);
        assertThat(metrics.calculateConvertedCount()).isEqualTo(1);
        assertThat(metrics.getConvertedCount()).isEqualTo(1);
    }

    @Test
    void calculateOpenClickAndConversionRatesUseSentAsDenominator() {
        // KB item 425 / FR-104: open_rate = opened_count / sent_count.
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());
        metrics.recordLaunchCounts(10, 0, 10);
        metrics.recordEngagementCounts(5, 2, 1, 1);

        assertThat(metrics.calculateOpenRate())
                .isEqualByComparingTo(new BigDecimal("0.5000"));
        assertThat(CampaignMetrics.calculateOpenRate(5, 10))
                .isEqualByComparingTo(new BigDecimal("0.5000"));
        assertThat(CampaignMetrics.calculateOpenRate(5L, 10L))
                .isEqualByComparingTo(new BigDecimal("0.5000"));
        // KB item 426 / FR-105: click_rate = clicked_count / sent_count.
        assertThat(metrics.calculateClickRate())
                .isEqualByComparingTo(new BigDecimal("0.2000"));
        assertThat(CampaignMetrics.calculateClickRate(2, 10))
                .isEqualByComparingTo(new BigDecimal("0.2000"));
        assertThat(CampaignMetrics.calculateClickRate(2L, 10L))
                .isEqualByComparingTo(new BigDecimal("0.2000"));
        // KB item 427 / FR-106: conversion_rate = converted_count / sent_count.
        assertThat(metrics.calculateConversionRate())
                .isEqualByComparingTo(new BigDecimal("0.1000"));
        assertThat(CampaignMetrics.calculateConversionRate(1, 10))
                .isEqualByComparingTo(new BigDecimal("0.1000"));
        assertThat(CampaignMetrics.calculateConversionRate(1L, 10L))
                .isEqualByComparingTo(new BigDecimal("0.1000"));
    }

    @Test
    void calculateRatesReturnZeroWhenNothingSent() {
        // KB item 425: open rate is zero when sent count is zero.
        // KB item 426: click rate is zero when sent count is zero.
        // KB item 427: conversion rate is zero when sent count is zero.
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());
        metrics.recordEngagementCounts(1, 1, 1, 1);

        assertThat(metrics.calculateOpenRate())
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        assertThat(CampaignMetrics.calculateOpenRate(1, 0))
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        assertThat(metrics.calculateClickRate())
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        assertThat(CampaignMetrics.calculateClickRate(1, 0))
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        assertThat(metrics.calculateConversionRate())
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        assertThat(CampaignMetrics.calculateConversionRate(1, 0))
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
    }

    @Test
    void updateFinancialEstimatesStoresMoneyAndCalculatedRoi() {
        // KB item 428: estimated_cost is normalized non-negative money at scale 2.
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());

        metrics.updateFinancialEstimates(new BigDecimal("100.00"), new BigDecimal("150.00"));

        assertThat(metrics.calculateEstimatedCost()).isEqualByComparingTo("100.00");
        assertThat(metrics.getEstimatedCost()).isEqualByComparingTo("100.00");
        assertThat(CampaignMetrics.calculateEstimatedCost(new BigDecimal("100")))
                .isEqualByComparingTo("100.00");
        // KB item 429: estimated_revenue is normalized non-negative money at scale 2.
        assertThat(metrics.calculateEstimatedRevenue()).isEqualByComparingTo("150.00");
        assertThat(metrics.getEstimatedRevenue()).isEqualByComparingTo("150.00");
        assertThat(CampaignMetrics.calculateEstimatedRevenue(new BigDecimal("150")))
                .isEqualByComparingTo("150.00");
        // KB item 430 / FR-107: ROI = (revenue − cost) / cost.
        assertThat(metrics.getEstimatedRoi()).isEqualByComparingTo("0.50");
        assertThat(metrics.calculateRoi()).isEqualByComparingTo("0.50");
        assertThat(metrics.calculateEstimatedRoi()).isEqualByComparingTo("0.50");
        assertThat(CampaignMetrics.calculateRoi(new BigDecimal("100.00"), new BigDecimal("150.00")))
                .isEqualByComparingTo("0.50");
        assertThat(
                        CampaignMetrics.calculateEstimatedRoi(
                                new BigDecimal("100.00"), new BigDecimal("150.00")))
                .isEqualByComparingTo("0.50");
    }

    @Test
    void calculateRoiHandlesMissingAndZeroCost() {
        // KB item 430: null when cost missing; zero when cost is zero.
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());

        assertThat(metrics.calculateRoi()).isNull();
        assertThat(metrics.calculateEstimatedRoi()).isNull();
        assertThat(CampaignMetrics.calculateEstimatedRoi(null, new BigDecimal("50.00"))).isNull();

        metrics.updateFinancialEstimates(BigDecimal.ZERO, new BigDecimal("50.00"));
        assertThat(metrics.calculateRoi()).isEqualByComparingTo("0.00");
        assertThat(metrics.calculateEstimatedRoi()).isEqualByComparingTo("0.00");

        metrics.updateFinancialEstimates(new BigDecimal("100.00"), null);
        assertThat(metrics.calculateRoi()).isEqualByComparingTo("-1.00");
        assertThat(metrics.calculateEstimatedRoi()).isEqualByComparingTo("-1.00");
    }

    @Test
    void rejectsNegativeFinancialEstimates() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(
                        () ->
                                metrics.updateFinancialEstimates(
                                        new BigDecimal("-1.00"), BigDecimal.ZERO))
                .withMessageContaining("Estimated cost must not be negative");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(
                        () ->
                                metrics.updateFinancialEstimates(
                                        BigDecimal.ZERO, new BigDecimal("-1.00")))
                .withMessageContaining("Estimated revenue must not be negative");
    }

    @Test
    void recalculateRefreshesAudienceSizeAndRoi() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());
        metrics.recordLaunchCounts(7, 3, 7);
        ReflectionTestUtils.setField(metrics, "audienceSize", 0);
        metrics.updateFinancialEstimates(new BigDecimal("200.00"), new BigDecimal("300.00"));
        ReflectionTestUtils.setField(metrics, "estimatedRoi", null);

        metrics.recalculate();

        assertThat(metrics.getAudienceSize()).isEqualTo(10);
        assertThat(metrics.getEstimatedRoi()).isEqualByComparingTo("0.50");
    }

    @Test
    void onCreateAssignsIdAndUpdatedAt() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        assertThat(metrics.getId()).isNotNull();
        assertThat(metrics.getUpdatedAt()).isNotNull();
        assertThat(metrics.getUpdatedAt()).isBeforeOrEqualTo(Instant.now().plusSeconds(1));
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign());
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");
        Instant first = metrics.getUpdatedAt();

        ReflectionTestUtils.invokeMethod(metrics, "onUpdate");

        assertThat(metrics.getUpdatedAt()).isAfterOrEqualTo(first);
    }

    private static void assertColumn(String fieldName, String columnName, boolean nullable)
            throws Exception {
        Column column = field(fieldName).getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
    }

    private static Field field(String name) throws Exception {
        Field field = CampaignMetrics.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Campaign campaign() {
        User owner =
                User.create(
                        "campaign-metrics-owner@test.example",
                        "{noop}password",
                        "Campaign Metrics Owner");
        Campaign campaign =
                Campaign.create(
                        "Metrics campaign",
                        "CampaignMetric entity coverage",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }
}
