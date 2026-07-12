package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignMetrics;
import com.bayerwestphalian.campaign.campaign.CampaignMetricsRepository;
import com.bayerwestphalian.campaign.campaign.CampaignProductRepository;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.user.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 430 / FR-107: Calculate estimated ROI.
 *
 * <p>Estimated ROI = (estimated_revenue − estimated_cost) / estimated_cost when cost &gt; 0; {@code
 * null} when cost is missing; zero when cost is zero. Scale 2. Dashboard / executive ROI uses
 * aggregate cost and revenue totals (not an average of per-campaign ROIs).
 *
 * <p>Acceptance coverage for the same rule is also formalized under KB item 454 in {@link
 * RoiIsCalculatedCorrectlyTests}.
 */
@ExtendWith(MockitoExtension.class)
class CalculateEstimatedRoiTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000430");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000431");

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

    @Test
    void estimatedRoiIsRevenueMinusCostOverCost() {
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
    }

    @Test
    void estimatedRoiIsNullWhenCostMissing() {
        assertThat(CampaignMetrics.calculateEstimatedRoi(null, new BigDecimal("100.00"))).isNull();
        assertThat(CampaignMetrics.calculateRoi(null, null)).isNull();
        assertThat(AnalyticsCalculations.calculateEstimatedRoi(null, new BigDecimal("10"))).isNull();
        assertThat(AnalyticsRates.roi(null, new BigDecimal("10"))).isNull();
    }

    @Test
    void estimatedRoiIsZeroWhenCostIsZero() {
        assertThat(
                        CampaignMetrics.calculateEstimatedRoi(
                                BigDecimal.ZERO, new BigDecimal("50.00")))
                .isEqualByComparingTo("0.00");
        assertThat(CampaignMetrics.calculateEstimatedRoi(BigDecimal.ZERO, null))
                .isEqualByComparingTo("0.00");
        assertThat(AnalyticsRates.roi(new BigDecimal("0.00"), new BigDecimal("10.00")))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void estimatedRoiTreatsMissingRevenueAsZero() {
        // (0 - 100) / 100 = -1.00
        assertThat(CampaignMetrics.calculateEstimatedRoi(new BigDecimal("100.00"), null))
                .isEqualByComparingTo("-1.00");
        assertThat(
                        AnalyticsCalculations.calculateEstimatedRoi(
                                new BigDecimal("100.00"), null))
                .isEqualByComparingTo("-1.00");
    }

    @Test
    void estimatedRoiRejectsNegativeCostOrRevenue() {
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
    void metricsRowEstimatedRoiUsesCostAndRevenue() {
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
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "Store"));
        metrics.updateFinancialEstimates(new BigDecimal("40.00"), new BigDecimal("100.00"));

        // (100 - 40) / 40 = 1.50
        assertThat(metrics.getEstimatedRoi()).isEqualByComparingTo("1.50");
        assertThat(metrics.calculateEstimatedRoi()).isEqualByComparingTo("1.50");
    }

    @Test
    void totalEstimatedRoiUsesAggregateCostAndRevenue() {
        CampaignMetrics a = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        a.updateFinancialEstimates(new BigDecimal("100.00"), new BigDecimal("150.00")); // ROI 0.50
        CampaignMetrics b = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_B, "B"));
        b.updateFinancialEstimates(new BigDecimal("25.00"), new BigDecimal("50.00")); // ROI 1.00

        // Aggregate: cost 125, revenue 200 → ROI = 75/125 = 0.60 (not average of 0.50 and 1.00)
        assertThat(AnalyticsCalculations.totalEstimatedRoi(List.of(a, b)))
                .isEqualByComparingTo("0.60");
        assertThat(AnalyticsCalculations.totalEstimatedRoi(List.of())).isNull();
        assertThat(AnalyticsCalculations.totalEstimatedRoi(null)).isNull();
    }

    @Test
    void dashboardEstimatedRoiUsesAggregateCostAndRevenue() {
        Campaign campaignA = sampleCampaign(CAMPAIGN_A, "A");
        ReflectionTestUtils.setField(campaignA, "status", CampaignStatus.ACTIVE);
        Campaign campaignB = sampleCampaign(CAMPAIGN_B, "B");

        CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
        metricsA.recordLaunchCounts(80, 20, 70);
        metricsA.updateFinancialEstimates(new BigDecimal("100.00"), new BigDecimal("150.00"));
        CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
        metricsB.recordLaunchCounts(15, 5, 10);
        metricsB.updateFinancialEstimates(new BigDecimal("25.00"), new BigDecimal("50.00"));

        when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

        DashboardView dashboard = analyticsService.getDashboard();

        // FR-107: dashboard shows estimated ROI from totals cost 125, revenue 200 → 0.60
        assertThat(dashboard.estimatedCost()).isEqualByComparingTo("125.00");
        assertThat(dashboard.estimatedRevenue()).isEqualByComparingTo("200.00");
        assertThat(dashboard.estimatedRoi()).isEqualByComparingTo("0.60");
        assertThat(
                        AnalyticsCalculations.calculateEstimatedRoi(
                                dashboard.estimatedCost(), dashboard.estimatedRevenue()))
                .isEqualByComparingTo(dashboard.estimatedRoi());
    }

    @Test
    void executiveEstimatedRoiMatchesAggregateCalculation() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "Exec");
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(40, 10, 30);
        metrics.updateFinancialEstimates(new BigDecimal("80.00"), new BigDecimal("120.00"));

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
        when(campaignProductRepository.findAll()).thenReturn(List.of());

        ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

        assertThat(executive.totalEstimatedCost()).isEqualByComparingTo("80.00");
        assertThat(executive.totalEstimatedRevenue()).isEqualByComparingTo("120.00");
        assertThat(executive.overallEstimatedRoi()).isEqualByComparingTo("0.50");
    }

    @Test
    void campaignMetricsViewExposesCalculatedEstimatedRoi() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "View");
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(12, 3, 12);
        metrics.updateFinancialEstimates(new BigDecimal("60.00"), new BigDecimal("90.00"));
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        CampaignMetricsView view = CampaignMetricsView.from(metrics);

        assertThat(view.estimatedCost()).isEqualByComparingTo("60.00");
        assertThat(view.estimatedRevenue()).isEqualByComparingTo("90.00");
        assertThat(view.estimatedRoi()).isEqualByComparingTo("0.50");
    }

    @Test
    void estimatedRoiIsNullOnEmptyDashboard() {
        when(campaignRepository.findAll()).thenReturn(List.of());
        when(campaignMetricsRepository.findAll()).thenReturn(List.of());

        DashboardView dashboard = analyticsService.getDashboard();

        assertThat(dashboard.estimatedCost()).isNull();
        assertThat(dashboard.estimatedRevenue()).isNull();
        assertThat(dashboard.estimatedRoi()).isNull();
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner = User.create("estroi-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
