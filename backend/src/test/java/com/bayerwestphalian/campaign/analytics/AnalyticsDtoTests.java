package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignMetrics;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 414: analytics DTOs map dashboard, campaign analytics, product performance, and executive
 * aggregate payloads (E19 / FR-100–FR-107).
 */
class AnalyticsDtoTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000414");
    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000414");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000414");

    @Test
    void campaignMetricsViewMapsEntityCountersAndRates() {
        Campaign campaign = sampleCampaign();
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(100, 20, 90);
        metrics.recordEngagementCounts(45, 18, 9, 5);
        metrics.updateFinancialEstimates(new BigDecimal("1000.00"), new BigDecimal("1500.00"));
        ReflectionTestUtils.setField(
                metrics, "id", UUID.fromString("60000000-0000-0000-0000-000000000414"));
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        CampaignMetricsView view = CampaignMetricsView.from(metrics);

        assertThat(view.metricsId()).isEqualTo(metrics.getId());
        assertThat(view.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(view.campaignName()).isEqualTo("Analytics DTO campaign");
        assertThat(view.campaignStatus()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(view.audienceSize()).isEqualTo(120);
        assertThat(view.eligibleCount()).isEqualTo(100);
        assertThat(view.excludedCount()).isEqualTo(20);
        assertThat(view.sentCount()).isEqualTo(90);
        assertThat(view.openedCount()).isEqualTo(45);
        assertThat(view.clickedCount()).isEqualTo(18);
        assertThat(view.repliedCount()).isEqualTo(9);
        assertThat(view.convertedCount()).isEqualTo(5);
        assertThat(view.openRate()).isEqualByComparingTo("0.5000");
        assertThat(view.clickRate()).isEqualByComparingTo("0.2000");
        assertThat(view.conversionRate()).isEqualByComparingTo("0.0556");
        assertThat(view.estimatedCost()).isEqualByComparingTo("1000.00");
        assertThat(view.estimatedRevenue()).isEqualByComparingTo("1500.00");
        assertThat(view.estimatedRoi()).isEqualByComparingTo("0.50");
        assertThat(view.updatedAt()).isNotNull();
    }

    @Test
    void campaignMetricsViewRequiresMetrics() {
        assertThatThrownBy(() -> CampaignMetricsView.from(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("metrics is required");
    }

    @Test
    void dashboardViewHoldsFr100ToFr107KpisAndDefensiveListCopy() {
        CampaignMetricsView row =
                new CampaignMetricsView(
                        null,
                        CAMPAIGN_ID,
                        "Campaign A",
                        CampaignStatus.ACTIVE,
                        10,
                        8,
                        2,
                        8,
                        4,
                        2,
                        1,
                        1,
                        new BigDecimal("0.5000"),
                        new BigDecimal("0.2500"),
                        new BigDecimal("0.1250"),
                        new BigDecimal("100.00"),
                        new BigDecimal("150.00"),
                        new BigDecimal("0.50"),
                        null);
        List<CampaignMetricsView> mutable = new java.util.ArrayList<>(List.of(row));

        DashboardView dashboard =
                new DashboardView(
                        3L,
                        1L,
                        100L,
                        80L,
                        70L,
                        30L,
                        40L,
                        16L,
                        8L,
                        4L,
                        new BigDecimal("0.5000"),
                        new BigDecimal("0.2000"),
                        new BigDecimal("0.0500"),
                        new BigDecimal("200.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("0.50"),
                        mutable);

        mutable.clear();

        assertThat(dashboard.campaignTotal()).isEqualTo(3L); // FR-100
        assertThat(dashboard.activeCampaigns()).isEqualTo(1L); // FR-101
        assertThat(dashboard.audienceSize()).isEqualTo(100L); // FR-102
        assertThat(dashboard.messagesSent()).isEqualTo(80L); // FR-103
        assertThat(dashboard.openRate()).isEqualByComparingTo("0.5000"); // FR-104
        assertThat(dashboard.clickRate()).isEqualByComparingTo("0.2000"); // FR-105
        assertThat(dashboard.conversionRate()).isEqualByComparingTo("0.0500"); // FR-106
        assertThat(dashboard.estimatedRoi()).isEqualByComparingTo("0.50"); // FR-107
        assertThat(dashboard.recentCampaignMetrics()).hasSize(1).containsExactly(row);
    }

    @Test
    void dashboardViewEmptyReturnsZeroedKpis() {
        DashboardView empty = DashboardView.empty();

        assertThat(empty.campaignTotal()).isZero();
        assertThat(empty.activeCampaigns()).isZero();
        assertThat(empty.audienceSize()).isZero();
        assertThat(empty.messagesSent()).isZero();
        assertThat(empty.openRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(empty.clickRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(empty.conversionRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(empty.recentCampaignMetrics()).isEmpty();
    }

    @Test
    void campaignAnalyticsViewWrapsCampaignIdentityAndMetrics() {
        Campaign campaign = sampleCampaign();
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(10, 0, 10);
        metrics.recordEngagementCounts(5, 2, 1, 1);

        CampaignAnalyticsView view =
                CampaignAnalyticsView.of(
                        CAMPAIGN_ID,
                        campaign.getName(),
                        campaign.getObjective(),
                        CampaignStatus.ACTIVE,
                        CampaignChannel.EMAIL,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 8, 1),
                        OWNER_ID,
                        "Analytics Owner",
                        metrics);

        assertThat(view.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(view.campaignName()).isEqualTo("Analytics DTO campaign");
        assertThat(view.status()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(view.channel()).isEqualTo(CampaignChannel.EMAIL);
        assertThat(view.ownerUserId()).isEqualTo(OWNER_ID);
        assertThat(view.metrics()).isNotNull();
        assertThat(view.metrics().sentCount()).isEqualTo(10);
        assertThat(view.metrics().openRate()).isEqualByComparingTo("0.5000");
        assertThat(view.generatedAt()).isNotNull();
    }

    @Test
    void campaignAnalyticsViewRequiresCampaignId() {
        assertThatThrownBy(
                        () ->
                                CampaignAnalyticsView.of(
                                        null,
                                        "x",
                                        "y",
                                        CampaignStatus.DRAFT,
                                        CampaignChannel.EMAIL,
                                        null,
                                        null,
                                        null,
                                        null,
                                        (CampaignMetricsView) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("campaignId is required");
    }

    @Test
    void productPerformanceViewComputesRatesFromTotals() {
        ProductPerformanceView view =
                ProductPerformanceView.of(
                        PRODUCT_ID,
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        2L,
                        200L,
                        150L,
                        100L,
                        50L,
                        20L,
                        10L,
                        new BigDecimal("500.00"),
                        new BigDecimal("800.00"),
                        new BigDecimal("0.60"));

        assertThat(view.productId()).isEqualTo(PRODUCT_ID);
        assertThat(view.productName()).isEqualTo("Life Protection");
        assertThat(view.productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(view.campaignCount()).isEqualTo(2L);
        assertThat(view.sentCount()).isEqualTo(100L);
        assertThat(view.openRate()).isEqualByComparingTo("0.5000");
        assertThat(view.clickRate()).isEqualByComparingTo("0.2000");
        assertThat(view.conversionRate()).isEqualByComparingTo("0.1000");
        assertThat(view.estimatedRoi()).isEqualByComparingTo("0.60");
    }

    @Test
    void productPerformanceViewRequiresProductId() {
        assertThatThrownBy(
                        () ->
                                ProductPerformanceView.of(
                                        null,
                                        "x",
                                        ProductType.OTHER,
                                        0,
                                        0,
                                        0,
                                        0,
                                        0,
                                        0,
                                        0,
                                        null,
                                        null,
                                        null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("productId is required");
    }

    @Test
    void executiveDashboardViewHoldsAggregatesAndDefensiveProductList() {
        // KB item 434 / COMP-010: executive aggregate DTO for GET /api/analytics/executive.
        ProductPerformanceView product =
                ProductPerformanceView.of(
                        PRODUCT_ID,
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        1L,
                        50L,
                        40L,
                        40L,
                        20L,
                        8L,
                        4L,
                        null,
                        null,
                        null);
        List<ProductPerformanceView> mutable = new java.util.ArrayList<>(List.of(product));

        ExecutiveDashboardView view =
                new ExecutiveDashboardView(
                        5L,
                        2L,
                        1L,
                        500L,
                        400L,
                        100L,
                        300L,
                        150L,
                        60L,
                        30L,
                        15L,
                        new BigDecimal("0.5000"),
                        new BigDecimal("0.2000"),
                        new BigDecimal("0.0500"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("1400.00"),
                        new BigDecimal("0.40"),
                        mutable);

        mutable.clear();

        assertThat(view.totalCampaigns()).isEqualTo(5L);
        assertThat(view.activeCampaigns()).isEqualTo(2L);
        assertThat(view.completedCampaigns()).isEqualTo(1L);
        assertThat(view.totalAudience()).isEqualTo(500L);
        assertThat(view.totalSent()).isEqualTo(300L);
        assertThat(view.overallOpenRate()).isEqualByComparingTo("0.5000");
        assertThat(view.overallEstimatedRoi()).isEqualByComparingTo("0.40");
        assertThat(view.productPerformance()).hasSize(1).containsExactly(product);
    }

    @Test
    void executiveDashboardViewEmptyReturnsZeroedAggregates() {
        // KB item 434: empty executive payload shape used when no campaigns/metrics exist.
        ExecutiveDashboardView empty = ExecutiveDashboardView.empty();

        assertThat(empty.totalCampaigns()).isZero();
        assertThat(empty.activeCampaigns()).isZero();
        assertThat(empty.totalSent()).isZero();
        assertThat(empty.overallOpenRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(empty.productPerformance()).isEmpty();
    }

    @Test
    void analyticsRatesHelpersMatchEntityRateSemantics() {
        assertThat(AnalyticsRates.openRate(5, 10)).isEqualByComparingTo("0.5000");
        assertThat(AnalyticsRates.clickRate(2, 10)).isEqualByComparingTo("0.2000");
        assertThat(AnalyticsRates.conversionRate(1, 10)).isEqualByComparingTo("0.1000");
        assertThat(AnalyticsRates.openRate(1, 0))
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        assertThat(AnalyticsRates.roi(new BigDecimal("100.00"), new BigDecimal("150.00")))
                .isEqualByComparingTo("0.50");
        assertThat(AnalyticsRates.roi(null, new BigDecimal("10.00"))).isNull();
        assertThat(AnalyticsRates.roi(BigDecimal.ZERO, new BigDecimal("10.00")))
                .isEqualByComparingTo("0.00");
    }

    private static Campaign sampleCampaign() {
        User owner = User.create("analytics-dto@test.example", "{noop}x", "Analytics Owner");
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        Campaign campaign =
                Campaign.create(
                        "Analytics DTO campaign",
                        "DTO coverage objective",
                        owner,
                        null,
                        CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        return campaign;
    }
}
