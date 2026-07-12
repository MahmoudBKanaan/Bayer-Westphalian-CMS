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
 * KB item 454 acceptance: ROI is calculated correctly.
 *
 * <p>Item 430 / FR-107 definition: {@code estimated_roi = (estimated_revenue − estimated_cost) /
 * estimated_cost} when cost &gt; 0; {@code null} when cost is missing; zero when cost is zero
 * (scale 2, HALF_UP). Dashboard and executive ROI use aggregate cost and revenue (not an average
 * of per-campaign ROIs). Product performance uses the same formula on product-level totals.
 *
 * <p>Companion unit coverage also lives in {@link CalculateEstimatedRoiTests} (item 430).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("454 ROI is calculated correctly")
class RoiIsCalculatedCorrectlyTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000454");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000455");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000454");

    private static final BigDecimal ZERO_ROI =
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

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
    @DisplayName("Formula: ROI = (revenue − cost) / cost (scale 2)")
    class Formula {

        @Test
        void pureFormulaIsRevenueMinusCostOverCost() {
            assertThat(
                            CampaignMetrics.calculateEstimatedRoi(
                                    new BigDecimal("100.00"), new BigDecimal("150.00")))
                    .isEqualByComparingTo("0.50");
            assertThat(
                            CampaignMetrics.calculateRoi(
                                    new BigDecimal("100.00"), new BigDecimal("150.00")))
                    .isEqualByComparingTo("0.50");
            assertThat(
                            AnalyticsCalculations.calculateEstimatedRoi(
                                    new BigDecimal("200.00"), new BigDecimal("300.00")))
                    .isEqualByComparingTo("0.50");
            assertThat(AnalyticsRates.roi(new BigDecimal("100.00"), new BigDecimal("50.00")))
                    .isEqualByComparingTo("-0.50");
            // (100 - 40) / 40 = 1.50
            assertThat(
                            CampaignMetrics.calculateEstimatedRoi(
                                    new BigDecimal("40.00"), new BigDecimal("100.00")))
                    .isEqualByComparingTo("1.50");
        }

        @Test
        void pureFormulaIsNullWhenCostMissing() {
            assertThat(CampaignMetrics.calculateEstimatedRoi(null, new BigDecimal("100.00")))
                    .isNull();
            assertThat(CampaignMetrics.calculateRoi(null, null)).isNull();
            assertThat(AnalyticsCalculations.calculateEstimatedRoi(null, new BigDecimal("10")))
                    .isNull();
            assertThat(AnalyticsRates.roi(null, new BigDecimal("10"))).isNull();
        }

        @Test
        void pureFormulaIsZeroWhenCostIsZero() {
            assertThat(
                            CampaignMetrics.calculateEstimatedRoi(
                                    BigDecimal.ZERO, new BigDecimal("50.00")))
                    .isEqualByComparingTo(ZERO_ROI);
            assertThat(CampaignMetrics.calculateEstimatedRoi(BigDecimal.ZERO, null))
                    .isEqualByComparingTo(ZERO_ROI);
            assertThat(AnalyticsRates.roi(new BigDecimal("0.00"), new BigDecimal("10.00")))
                    .isEqualByComparingTo(ZERO_ROI);
        }

        @Test
        void pureFormulaTreatsMissingRevenueAsZero() {
            // (0 - 100) / 100 = -1.00
            assertThat(CampaignMetrics.calculateEstimatedRoi(new BigDecimal("100.00"), null))
                    .isEqualByComparingTo("-1.00");
            assertThat(
                            AnalyticsCalculations.calculateEstimatedRoi(
                                    new BigDecimal("100.00"), null))
                    .isEqualByComparingTo("-1.00");
        }

        @Test
        void pureFormulaRejectsNegativeCostOrRevenue() {
            assertThatThrownBy(
                            () ->
                                    CampaignMetrics.calculateEstimatedRoi(
                                            new BigDecimal("-1.00"), new BigDecimal("10.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Estimated cost must not be negative");
            assertThatThrownBy(
                            () ->
                                    CampaignMetrics.calculateEstimatedRoi(
                                            new BigDecimal("10.00"), new BigDecimal("-1.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Estimated revenue must not be negative");
        }

        @Test
        void pureFormulaUsesScaleTwoHalfUp() {
            // (10 - 3) / 3 = 2.333... → 2.33 at scale 2 HALF_UP
            assertThat(
                            CampaignMetrics.calculateEstimatedRoi(
                                    new BigDecimal("3.00"), new BigDecimal("10.00")))
                    .isEqualByComparingTo("2.33");
            // (10 - 6) / 6 = 0.666... → 0.67 at scale 2 HALF_UP
            assertThat(
                            CampaignMetrics.calculateEstimatedRoi(
                                    new BigDecimal("6.00"), new BigDecimal("10.00")))
                    .isEqualByComparingTo("0.67");
        }

        @Test
        void metricsRowRoiUsesCostAndRevenue() {
            CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
            metrics.updateFinancialEstimates(new BigDecimal("80.00"), new BigDecimal("120.00"));

            // (120 - 80) / 80 = 0.50
            assertThat(metrics.calculateEstimatedRoi()).isEqualByComparingTo("0.50");
            assertThat(metrics.calculateRoi()).isEqualByComparingTo("0.50");
            assertThat(AnalyticsCalculations.calculateEstimatedRoi(metrics))
                    .isEqualByComparingTo("0.50");
            assertThat(metrics.getEstimatedRoi()).isEqualByComparingTo("0.50");
        }

        @Test
        void updateFinancialEstimatesStoresCalculatedRoi() {
            CampaignMetrics metrics =
                    CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "Store"));
            metrics.updateFinancialEstimates(new BigDecimal("40.00"), new BigDecimal("100.00"));

            assertThat(metrics.getEstimatedRoi()).isEqualByComparingTo("1.50");
            assertThat(metrics.calculateEstimatedRoi()).isEqualByComparingTo("1.50");
        }
    }

    @Nested
    @DisplayName("Aggregates: dashboard / executive / product")
    class Aggregates {

        @Test
        void dashboardRoiUsesAggregateCostAndRevenueNotAverageOfCampaignRois() {
            Campaign campaignA = activeCampaign(CAMPAIGN_A, "A");
            Campaign campaignB = sampleCampaign(CAMPAIGN_B, "B");

            // A: cost 100, rev 150 → ROI 0.50 ; B: cost 25, rev 50 → ROI 1.00
            // Average of ROIs = 0.75 ; aggregate: cost 125, rev 200 → ROI 0.60
            CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
            metricsA.recordLaunchCounts(80, 20, 70);
            metricsA.updateFinancialEstimates(new BigDecimal("100.00"), new BigDecimal("150.00"));
            CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
            metricsB.recordLaunchCounts(15, 5, 10);
            metricsB.updateFinancialEstimates(new BigDecimal("25.00"), new BigDecimal("50.00"));

            when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

            DashboardView dashboard = analyticsService.getDashboard();

            // FR-107: estimated ROI from total cost / total revenue aggregates.
            assertThat(dashboard.estimatedCost()).isEqualByComparingTo("125.00");
            assertThat(dashboard.estimatedRevenue()).isEqualByComparingTo("200.00");
            assertThat(dashboard.estimatedRoi()).isEqualByComparingTo("0.60");
            assertThat(
                            AnalyticsCalculations.calculateEstimatedRoi(
                                    dashboard.estimatedCost(), dashboard.estimatedRevenue()))
                    .isEqualByComparingTo(dashboard.estimatedRoi());
            assertThat(AnalyticsCalculations.totalEstimatedRoi(List.of(metricsA, metricsB)))
                    .isEqualByComparingTo("0.60");

            BigDecimal averageOfRois =
                    metricsA.calculateEstimatedRoi()
                            .add(metricsB.calculateEstimatedRoi())
                            .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            assertThat(averageOfRois).isEqualByComparingTo("0.75");
            assertThat(dashboard.estimatedRoi()).isNotEqualByComparingTo(averageOfRois);
        }

        @Test
        void executiveRoiUsesAggregateCostAndRevenue() {
            Campaign campaign = activeCampaign(CAMPAIGN_A, "Exec");
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            metrics.recordLaunchCounts(40, 10, 30);
            metrics.updateFinancialEstimates(new BigDecimal("80.00"), new BigDecimal("120.00"));

            when(campaignRepository.findAll()).thenReturn(List.of(campaign));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
            when(campaignProductRepository.findAll()).thenReturn(List.of());

            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            // (120 - 80) / 80 = 0.50
            assertThat(executive.totalEstimatedCost()).isEqualByComparingTo("80.00");
            assertThat(executive.totalEstimatedRevenue()).isEqualByComparingTo("120.00");
            assertThat(executive.overallEstimatedRoi()).isEqualByComparingTo("0.50");
            assertThat(
                            AnalyticsCalculations.calculateEstimatedRoi(
                                    executive.totalEstimatedCost(),
                                    executive.totalEstimatedRevenue()))
                    .isEqualByComparingTo(executive.overallEstimatedRoi());
        }

        @Test
        void emptyInventoryYieldsNullRoi() {
            when(campaignRepository.findAll()).thenReturn(List.of());
            when(campaignMetricsRepository.findAll()).thenReturn(List.of());
            when(campaignProductRepository.findAll()).thenReturn(List.of());

            DashboardView dashboard = analyticsService.getDashboard();
            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            assertThat(dashboard.estimatedCost()).isNull();
            assertThat(dashboard.estimatedRevenue()).isNull();
            assertThat(dashboard.estimatedRoi()).isNull();
            assertThat(executive.totalEstimatedCost()).isNull();
            assertThat(executive.totalEstimatedRevenue()).isNull();
            assertThat(executive.overallEstimatedRoi()).isNull();
            assertThat(AnalyticsCalculations.totalEstimatedRoi(List.of())).isNull();
            assertThat(AnalyticsCalculations.totalEstimatedRoi(null)).isNull();
        }

        @Test
        void productPerformanceRoiUsesProductLevelCostAndRevenue() {
            Campaign campaignA = sampleCampaign(CAMPAIGN_A, "ProdA");
            Campaign campaignB = sampleCampaign(CAMPAIGN_B, "ProdB");
            CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
            metricsA.recordLaunchCounts(20, 0, 20);
            metricsA.updateFinancialEstimates(new BigDecimal("100.00"), new BigDecimal("150.00"));
            CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
            metricsB.recordLaunchCounts(10, 0, 10);
            metricsB.updateFinancialEstimates(new BigDecimal("25.00"), new BigDecimal("50.00"));

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
            // cost 125, revenue 200 → ROI 0.60
            assertThat(row.estimatedCost()).isEqualByComparingTo("125.00");
            assertThat(row.estimatedRevenue()).isEqualByComparingTo("200.00");
            assertThat(row.estimatedRoi()).isEqualByComparingTo("0.60");
            assertThat(
                            AnalyticsCalculations.calculateEstimatedRoi(
                                    row.estimatedCost(), row.estimatedRevenue()))
                    .isEqualByComparingTo(row.estimatedRoi());
        }
    }

    @Nested
    @DisplayName("Campaign analytics detail and view mapping")
    class DetailAndViews {

        @Test
        void campaignAnalyticsRoiMatchesMetricsFormula() {
            Campaign campaign = activeCampaign(CAMPAIGN_A, "Detail");
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            metrics.recordLaunchCounts(20, 0, 20);
            metrics.updateFinancialEstimates(new BigDecimal("60.00"), new BigDecimal("90.00"));

            when(campaignRepository.findById(CAMPAIGN_A)).thenReturn(Optional.of(campaign));
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_A))
                    .thenReturn(Optional.of(metrics));

            CampaignAnalyticsView analytics = analyticsService.getCampaignAnalytics(CAMPAIGN_A);

            assertThat(analytics.metrics()).isNotNull();
            assertThat(analytics.metrics().estimatedCost()).isEqualByComparingTo("60.00");
            assertThat(analytics.metrics().estimatedRevenue()).isEqualByComparingTo("90.00");
            // (90 - 60) / 60 = 0.50
            assertThat(analytics.metrics().estimatedRoi()).isEqualByComparingTo("0.50");
        }

        @Test
        void campaignMetricsViewExposesCalculatedEstimatedRoi() {
            CampaignMetrics metrics =
                    CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "View"));
            metrics.recordLaunchCounts(12, 3, 12);
            metrics.updateFinancialEstimates(new BigDecimal("60.00"), new BigDecimal("90.00"));

            CampaignMetricsView view = CampaignMetricsView.from(metrics);

            assertThat(view.estimatedCost()).isEqualByComparingTo("60.00");
            assertThat(view.estimatedRevenue()).isEqualByComparingTo("90.00");
            assertThat(view.estimatedRoi()).isEqualByComparingTo("0.50");
        }
    }

    private static Campaign activeCampaign(UUID id, String name) {
        Campaign campaign = sampleCampaign(id, name);
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        return campaign;
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner = User.create("roi454-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
