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
 * KB item 451 acceptance: Open rate is calculated correctly.
 *
 * <p>Item 425 / FR-104 definition: {@code open_rate = opened_count / sent_count} when sent &gt; 0;
 * otherwise {@link BigDecimal#ZERO} at scale 4 (HALF_UP). Dashboard and executive rates use
 * aggregate opened ÷ aggregate sent (not an average of per-campaign rates). Product performance
 * uses the same formula on product-level totals.
 *
 * <p>Companion unit coverage also lives in {@link CalculateOpenRateTests} (item 425).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("451 Open rate is calculated correctly")
class OpenRateIsCalculatedCorrectlyTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000451");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000452");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000451");

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
    @DisplayName("Formula: open rate = opened / sent (scale 4)")
    class Formula {

        @Test
        void pureFormulaIsOpenedDividedBySent() {
            assertThat(CampaignMetrics.calculateOpenRate(5, 10)).isEqualByComparingTo("0.5000");
            assertThat(CampaignMetrics.calculateOpenRate(1, 4)).isEqualByComparingTo("0.2500");
            assertThat(CampaignMetrics.calculateOpenRate(0, 10)).isEqualByComparingTo("0.0000");
            assertThat(CampaignMetrics.calculateOpenRate(3, 3)).isEqualByComparingTo("1.0000");

            assertThat(AnalyticsCalculations.calculateOpenRate(40L, 80L))
                    .isEqualByComparingTo("0.5000");
            assertThat(AnalyticsRates.openRate(40L, 80L)).isEqualByComparingTo("0.5000");
        }

        @Test
        void pureFormulaIsZeroWhenNothingSent() {
            assertThat(CampaignMetrics.calculateOpenRate(0, 0)).isEqualByComparingTo(ZERO_RATE);
            assertThat(CampaignMetrics.calculateOpenRate(5, 0)).isEqualByComparingTo(ZERO_RATE);
            assertThat(CampaignMetrics.calculateOpenRate(5L, 0L)).isEqualByComparingTo(ZERO_RATE);
            assertThat(AnalyticsCalculations.calculateOpenRate(1L, 0L))
                    .isEqualByComparingTo(ZERO_RATE);
            assertThat(AnalyticsRates.openRate(1L, 0L)).isEqualByComparingTo(ZERO_RATE);
        }

        @Test
        void pureFormulaRejectsNegativeInputs() {
            assertThatThrownBy(() -> CampaignMetrics.calculateOpenRate(-1, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Opened count must not be negative");
            assertThatThrownBy(() -> CampaignMetrics.calculateOpenRate(1, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sent count must not be negative");
            assertThatThrownBy(() -> CampaignMetrics.calculateOpenRate(-1L, 10L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Opened count must not be negative");
            assertThatThrownBy(() -> AnalyticsCalculations.calculateOpenRate(-5L, 10L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void pureFormulaUsesScaleFourHalfUp() {
            // 1/3 = 0.3333..., HALF_UP at scale 4 → 0.3333
            assertThat(CampaignMetrics.calculateOpenRate(1, 3)).isEqualByComparingTo("0.3333");
            // 2/3 = 0.6666..., HALF_UP at scale 4 → 0.6667
            assertThat(CampaignMetrics.calculateOpenRate(2, 3)).isEqualByComparingTo("0.6667");
        }

        @Test
        void metricsRowOpenRateUsesOpenedAndSentCounts() {
            CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
            metrics.recordLaunchCounts(10, 0, 10);
            metrics.recordEngagementCounts(4, 0, 0, 0);

            assertThat(metrics.calculateOpenRate()).isEqualByComparingTo("0.4000");
            assertThat(AnalyticsCalculations.calculateOpenRate(metrics))
                    .isEqualByComparingTo("0.4000");
            assertThat(
                            CampaignMetrics.calculateOpenRate(
                                    metrics.calculateOpenedCount(), metrics.calculateSentCount()))
                    .isEqualByComparingTo("0.4000");
        }
    }

    @Nested
    @DisplayName("Aggregates: dashboard / executive / product")
    class Aggregates {

        @Test
        void dashboardOpenRateUsesAggregateOpenedOverSentNotAverageOfCampaignRates() {
            Campaign campaignA = activeCampaign(CAMPAIGN_A, "A");
            Campaign campaignB = sampleCampaign(CAMPAIGN_B, "B");

            // Campaign A: 1/10 = 0.10 ; Campaign B: 9/10 = 0.90 ; average of rates = 0.50
            // Aggregate opened/sent = 10/20 = 0.50 (same here, but formula must use aggregates).
            CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
            metricsA.recordLaunchCounts(10, 0, 10);
            metricsA.recordEngagementCounts(1, 0, 0, 0);
            CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
            metricsB.recordLaunchCounts(10, 0, 10);
            metricsB.recordEngagementCounts(9, 0, 0, 0);

            when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

            DashboardView dashboard = analyticsService.getDashboard();

            // FR-104: open rate from total opened / total sent = 10 / 20.
            assertThat(dashboard.openedCount()).isEqualTo(10L);
            assertThat(dashboard.messagesSent()).isEqualTo(20L);
            assertThat(dashboard.openRate()).isEqualByComparingTo("0.5000");
            assertThat(
                            AnalyticsCalculations.calculateOpenRate(
                                    dashboard.openedCount(), dashboard.messagesSent()))
                    .isEqualByComparingTo(dashboard.openRate());

            // Unequal sizes: aggregate must not equal naive average of rates.
            // A: 1/10=0.1, B: 1/2=0.5 → average 0.3 ; aggregate 2/12 ≈ 0.1667
            CampaignMetrics metricsC = CampaignMetrics.forCampaign(campaignA);
            metricsC.recordLaunchCounts(10, 0, 10);
            metricsC.recordEngagementCounts(1, 0, 0, 0);
            CampaignMetrics metricsD = CampaignMetrics.forCampaign(campaignB);
            metricsD.recordLaunchCounts(2, 0, 2);
            metricsD.recordEngagementCounts(1, 0, 0, 0);
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsC, metricsD));

            DashboardView uneven = analyticsService.getDashboard();
            assertThat(uneven.openedCount()).isEqualTo(2L);
            assertThat(uneven.messagesSent()).isEqualTo(12L);
            assertThat(uneven.openRate()).isEqualByComparingTo("0.1667");
            BigDecimal averageOfRates =
                    metricsC.calculateOpenRate()
                            .add(metricsD.calculateOpenRate())
                            .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
            assertThat(uneven.openRate()).isNotEqualByComparingTo(averageOfRates);
        }

        @Test
        void executiveOpenRateUsesAggregateOpenedOverSent() {
            Campaign campaign = activeCampaign(CAMPAIGN_A, "Exec");
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            metrics.recordLaunchCounts(40, 10, 30);
            metrics.recordEngagementCounts(12, 6, 4, 3);

            when(campaignRepository.findAll()).thenReturn(List.of(campaign));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
            when(campaignProductRepository.findAll()).thenReturn(List.of());

            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            // 12 / 30 = 0.4
            assertThat(executive.totalOpened()).isEqualTo(12L);
            assertThat(executive.totalSent()).isEqualTo(30L);
            assertThat(executive.overallOpenRate()).isEqualByComparingTo("0.4000");
            assertThat(
                            AnalyticsCalculations.calculateOpenRate(
                                    executive.totalOpened(), executive.totalSent()))
                    .isEqualByComparingTo(executive.overallOpenRate());
        }

        @Test
        void emptyInventoryYieldsZeroOpenRate() {
            when(campaignRepository.findAll()).thenReturn(List.of());
            when(campaignMetricsRepository.findAll()).thenReturn(List.of());
            when(campaignProductRepository.findAll()).thenReturn(List.of());

            DashboardView dashboard = analyticsService.getDashboard();
            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            assertThat(dashboard.openedCount()).isZero();
            assertThat(dashboard.messagesSent()).isZero();
            assertThat(dashboard.openRate()).isEqualByComparingTo(ZERO_RATE);
            assertThat(executive.totalOpened()).isZero();
            assertThat(executive.totalSent()).isZero();
            assertThat(executive.overallOpenRate()).isEqualByComparingTo(ZERO_RATE);
        }

        @Test
        void productPerformanceOpenRateUsesProductLevelOpenedOverSent() {
            Campaign campaignA = sampleCampaign(CAMPAIGN_A, "ProdA");
            Campaign campaignB = sampleCampaign(CAMPAIGN_B, "ProdB");
            CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
            metricsA.recordLaunchCounts(20, 0, 20);
            metricsA.recordEngagementCounts(10, 0, 0, 0);
            CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
            metricsB.recordLaunchCounts(10, 0, 10);
            metricsB.recordEngagementCounts(2, 0, 0, 0);

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
            // 12 opened / 30 sent = 0.4
            assertThat(row.openedCount()).isEqualTo(12L);
            assertThat(row.sentCount()).isEqualTo(30L);
            assertThat(row.openRate()).isEqualByComparingTo("0.4000");
            assertThat(AnalyticsCalculations.calculateOpenRate(row.openedCount(), row.sentCount()))
                    .isEqualByComparingTo(row.openRate());
        }
    }

    @Nested
    @DisplayName("Campaign analytics detail and view mapping")
    class DetailAndViews {

        @Test
        void campaignAnalyticsOpenRateMatchesMetricsFormula() {
            Campaign campaign = activeCampaign(CAMPAIGN_A, "Detail");
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            metrics.recordLaunchCounts(20, 0, 20);
            metrics.recordEngagementCounts(5, 0, 0, 0);

            when(campaignRepository.findById(CAMPAIGN_A)).thenReturn(Optional.of(campaign));
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_A))
                    .thenReturn(Optional.of(metrics));

            CampaignAnalyticsView analytics = analyticsService.getCampaignAnalytics(CAMPAIGN_A);

            assertThat(analytics.metrics()).isNotNull();
            assertThat(analytics.metrics().openedCount()).isEqualTo(5);
            assertThat(analytics.metrics().sentCount()).isEqualTo(20);
            assertThat(analytics.metrics().openRate()).isEqualByComparingTo("0.2500");
        }

        @Test
        void campaignMetricsViewExposesCalculatedOpenRate() {
            CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "View"));
            metrics.recordLaunchCounts(12, 3, 12);
            metrics.recordEngagementCounts(6, 3, 2, 1);

            CampaignMetricsView view = CampaignMetricsView.from(metrics);

            assertThat(view.openedCount()).isEqualTo(6);
            assertThat(view.sentCount()).isEqualTo(12);
            assertThat(view.openRate()).isEqualByComparingTo("0.5000");
        }
    }

    private static Campaign activeCampaign(UUID id, String name) {
        Campaign campaign = sampleCampaign(id, name);
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        return campaign;
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner =
                User.create("openrate451-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
