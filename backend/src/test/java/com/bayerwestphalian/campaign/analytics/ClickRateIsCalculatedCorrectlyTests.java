package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignMetrics;
import com.bayerwestphalian.campaign.campaign.CampaignMetricsRepository;
import com.bayerwestphalian.campaign.campaign.CampaignProduct;
import com.bayerwestphalian.campaign.campaign.CampaignProductRepository;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 452 acceptance: Click rate is calculated correctly.
 *
 * <p>Item 426 / FR-105 definition: {@code click_rate = clicked_count / sent_count} when sent &gt; 0;
 * otherwise {@link BigDecimal#ZERO} at scale 4 (HALF_UP). Dashboard and executive rates use
 * aggregate clicked ÷ aggregate sent (not an average of per-campaign rates). Product performance
 * uses the same formula on product-level totals.
 *
 * <p>Companion unit coverage also lives in {@link CalculateClickRateTests} (item 426).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("452 Click rate is calculated correctly")
class ClickRateIsCalculatedCorrectlyTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000452");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000453");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000452");

    private static final BigDecimal ZERO_RATE =
            BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

    @Mock private CampaignRepository campaignRepository;
    @Mock private CampaignMetricsRepository campaignMetricsRepository;
    @Mock private CampaignProductRepository campaignProductRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService =
                new AnalyticsService(
                        campaignRepository, campaignMetricsRepository, campaignProductRepository);
    }

    @Nested
    @DisplayName("Formula: click rate = clicked / sent (scale 4)")
    class Formula {

        @Test
        void pureFormulaIsClickedDividedBySent() {
            assertThat(CampaignMetrics.calculateClickRate(2, 10)).isEqualByComparingTo("0.2000");
            assertThat(CampaignMetrics.calculateClickRate(1, 4)).isEqualByComparingTo("0.2500");
            assertThat(CampaignMetrics.calculateClickRate(0, 10)).isEqualByComparingTo("0.0000");
            assertThat(CampaignMetrics.calculateClickRate(3, 3)).isEqualByComparingTo("1.0000");

            assertThat(AnalyticsCalculations.calculateClickRate(16L, 80L))
                    .isEqualByComparingTo("0.2000");
            assertThat(AnalyticsRates.clickRate(16L, 80L)).isEqualByComparingTo("0.2000");
        }

        @Test
        void pureFormulaIsZeroWhenNothingSent() {
            assertThat(CampaignMetrics.calculateClickRate(0, 0)).isEqualByComparingTo(ZERO_RATE);
            assertThat(CampaignMetrics.calculateClickRate(5, 0)).isEqualByComparingTo(ZERO_RATE);
            assertThat(CampaignMetrics.calculateClickRate(5L, 0L)).isEqualByComparingTo(ZERO_RATE);
            assertThat(AnalyticsCalculations.calculateClickRate(1L, 0L))
                    .isEqualByComparingTo(ZERO_RATE);
            assertThat(AnalyticsRates.clickRate(1L, 0L)).isEqualByComparingTo(ZERO_RATE);
        }

        @Test
        void pureFormulaRejectsNegativeInputs() {
            assertThatThrownBy(() -> CampaignMetrics.calculateClickRate(-1, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Clicked count must not be negative");
            assertThatThrownBy(() -> CampaignMetrics.calculateClickRate(1, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sent count must not be negative");
            assertThatThrownBy(() -> CampaignMetrics.calculateClickRate(-1L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Clicked count must not be negative");
            assertThatThrownBy(() -> AnalyticsCalculations.calculateClickRate(-5L, 10L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void pureFormulaUsesScaleFourHalfUp() {
            // 1/3 = 0.3333..., HALF_UP at scale 4 → 0.3333
            assertThat(CampaignMetrics.calculateClickRate(1, 3)).isEqualByComparingTo("0.3333");
            // 2/3 = 0.6666..., HALF_UP at scale 4 → 0.6667
            assertThat(CampaignMetrics.calculateClickRate(2, 3)).isEqualByComparingTo("0.6667");
        }

        @Test
        void metricsRowClickRateUsesClickedAndSentCounts() {
            CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
            metrics.recordLaunchCounts(10, 0, 10);
            metrics.recordEngagementCounts(0, 2, 0, 0);

            assertThat(metrics.calculateClickRate()).isEqualByComparingTo("0.2000");
            assertThat(AnalyticsCalculations.calculateClickRate(metrics))
                    .isEqualByComparingTo("0.2000");
            assertThat(
                            CampaignMetrics.calculateClickRate(
                                    metrics.calculateClickedCount(), metrics.calculateSentCount()))
                    .isEqualByComparingTo("0.2000");
        }
    }

    @Nested
    @DisplayName("Aggregates: dashboard / executive / product")
    class Aggregates {

        @Test
        void dashboardClickRateUsesAggregateClickedOverSentNotAverageOfCampaignRates() {
            Campaign campaignA = activeCampaign(CAMPAIGN_A, "A");
            Campaign campaignB = sampleCampaign(CAMPAIGN_B, "B");

            // Campaign A: 1/10 = 0.10 ; Campaign B: 9/10 = 0.90 ; average of rates = 0.50
            // Aggregate clicked/sent = 10/20 = 0.50 (same here, but formula must use aggregates).
            CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
            metricsA.recordLaunchCounts(10, 0, 10);
            metricsA.recordEngagementCounts(0, 1, 0, 0);
            CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
            metricsB.recordLaunchCounts(10, 0, 10);
            metricsB.recordEngagementCounts(0, 9, 0, 0);

            when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

            DashboardView dashboard = analyticsService.getDashboard();

            // FR-105: click rate from total clicked / total sent = 10 / 20.
            assertThat(dashboard.clickedCount()).isEqualTo(10L);
            assertThat(dashboard.messagesSent()).isEqualTo(20L);
            assertThat(dashboard.clickRate()).isEqualByComparingTo("0.5000");
            assertThat(
                            AnalyticsCalculations.calculateClickRate(
                                    dashboard.clickedCount(), dashboard.messagesSent()))
                    .isEqualByComparingTo(dashboard.clickRate());

            // Unequal sizes: aggregate must not equal naive average of rates.
            // A: 1/10=0.1, B: 1/2=0.5 → average 0.3 ; aggregate 2/12 ≈ 0.1667
            CampaignMetrics metricsC = CampaignMetrics.forCampaign(campaignA);
            metricsC.recordLaunchCounts(10, 0, 10);
            metricsC.recordEngagementCounts(0, 1, 0, 0);
            CampaignMetrics metricsD = CampaignMetrics.forCampaign(campaignB);
            metricsD.recordLaunchCounts(2, 0, 2);
            metricsD.recordEngagementCounts(0, 1, 0, 0);
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsC, metricsD));

            DashboardView uneven = analyticsService.getDashboard();
            assertThat(uneven.clickedCount()).isEqualTo(2L);
            assertThat(uneven.messagesSent()).isEqualTo(12L);
            assertThat(uneven.clickRate()).isEqualByComparingTo("0.1667");
            BigDecimal averageOfRates =
                    metricsC.calculateClickRate()
                            .add(metricsD.calculateClickRate())
                            .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
            assertThat(uneven.clickRate()).isNotEqualByComparingTo(averageOfRates);
        }

        @Test
        void executiveClickRateUsesAggregateClickedOverSent() {
            Campaign campaign = activeCampaign(CAMPAIGN_A, "Exec");
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            metrics.recordLaunchCounts(40, 10, 30);
            metrics.recordEngagementCounts(12, 6, 4, 3);

            when(campaignRepository.findAll()).thenReturn(List.of(campaign));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
            when(campaignProductRepository.findAll()).thenReturn(List.of());

            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            // 6 / 30 = 0.2
            assertThat(executive.totalClicked()).isEqualTo(6L);
            assertThat(executive.totalSent()).isEqualTo(30L);
            assertThat(executive.overallClickRate()).isEqualByComparingTo("0.2000");
            assertThat(
                            AnalyticsCalculations.calculateClickRate(
                                    executive.totalClicked(), executive.totalSent()))
                    .isEqualByComparingTo(executive.overallClickRate());
        }

        @Test
        void emptyInventoryYieldsZeroClickRate() {
            when(campaignRepository.findAll()).thenReturn(List.of());
            when(campaignMetricsRepository.findAll()).thenReturn(List.of());
            when(campaignProductRepository.findAll()).thenReturn(List.of());

            DashboardView dashboard = analyticsService.getDashboard();
            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            assertThat(dashboard.clickedCount()).isZero();
            assertThat(dashboard.messagesSent()).isZero();
            assertThat(dashboard.clickRate()).isEqualByComparingTo(ZERO_RATE);
            assertThat(executive.totalClicked()).isZero();
            assertThat(executive.totalSent()).isZero();
            assertThat(executive.overallClickRate()).isEqualByComparingTo(ZERO_RATE);
        }

        @Test
        void productPerformanceClickRateUsesProductLevelClickedOverSent() {
            Campaign campaignA = sampleCampaign(CAMPAIGN_A, "ProdA");
            Campaign campaignB = sampleCampaign(CAMPAIGN_B, "ProdB");
            CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
            metricsA.recordLaunchCounts(20, 0, 20);
            metricsA.recordEngagementCounts(0, 10, 0, 0);
            CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
            metricsB.recordLaunchCounts(10, 0, 10);
            metricsB.recordEngagementCounts(0, 2, 0, 0);

            Product product =
                    Product.create(
                            "Life Protection", ProductType.LIFE_INSURANCE, BigDecimal.TEN, 12);
            ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

            when(campaignProductRepository.findAll())
                    .thenReturn(
                            List.of(
                                    CampaignProduct.link(campaignA, product),
                                    CampaignProduct.link(campaignB, product)));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

            List<ProductPerformanceView> rows = analyticsService.getProductPerformance();

            assertThat(rows).hasSize(1);
            ProductPerformanceView row = rows.get(0);
            // 12 clicked / 30 sent = 0.4
            assertThat(row.clickedCount()).isEqualTo(12L);
            assertThat(row.sentCount()).isEqualTo(30L);
            assertThat(row.clickRate()).isEqualByComparingTo("0.4000");
            assertThat(AnalyticsCalculations.calculateClickRate(row.clickedCount(), row.sentCount()))
                    .isEqualByComparingTo(row.clickRate());
        }
    }

    @Nested
    @DisplayName("Campaign analytics detail and view mapping")
    class DetailAndViews {

        @Test
        void campaignAnalyticsClickRateMatchesMetricsFormula() {
            Campaign campaign = activeCampaign(CAMPAIGN_A, "Detail");
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            metrics.recordLaunchCounts(20, 0, 20);
            metrics.recordEngagementCounts(0, 5, 0, 0);

            when(campaignRepository.findById(CAMPAIGN_A)).thenReturn(Optional.of(campaign));
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_A))
                    .thenReturn(Optional.of(metrics));

            CampaignAnalyticsView analytics = analyticsService.getCampaignAnalytics(CAMPAIGN_A);

            assertThat(analytics.metrics()).isNotNull();
            assertThat(analytics.metrics().clickedCount()).isEqualTo(5);
            assertThat(analytics.metrics().sentCount()).isEqualTo(20);
            assertThat(analytics.metrics().clickRate()).isEqualByComparingTo("0.2500");
        }

        @Test
        void campaignMetricsViewExposesCalculatedClickRate() {
            CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "View"));
            metrics.recordLaunchCounts(12, 3, 12);
            metrics.recordEngagementCounts(6, 3, 2, 1);

            CampaignMetricsView view = CampaignMetricsView.from(metrics);

            assertThat(view.clickedCount()).isEqualTo(3);
            assertThat(view.sentCount()).isEqualTo(12);
            assertThat(view.clickRate()).isEqualByComparingTo("0.2500");
        }
    }

    private static Campaign activeCampaign(UUID id, String name) {
        Campaign campaign = sampleCampaign(id, name);
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        return campaign;
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner =
                User.create("clickrate452-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
