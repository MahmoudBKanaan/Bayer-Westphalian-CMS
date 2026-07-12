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
 * KB item 428: Calculate estimated cost.
 *
 * <p>Estimated cost is an optional non-negative monetary value stored on {@code
 * campaign_metrics.estimated_cost} (scale 2). It can also be projected as unit cost × quantity.
 * Dashboard / executive totals sum per-campaign estimated costs.
 */
@ExtendWith(MockitoExtension.class)
class CalculateEstimatedCostTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000428");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000429");

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
    void estimatedCostNormalizesToScaleTwo() {
        assertThat(CampaignMetrics.calculateEstimatedCost(new BigDecimal("100")))
                .isEqualByComparingTo("100.00");
        assertThat(CampaignMetrics.calculateEstimatedCost(new BigDecimal("10.5")))
                .isEqualByComparingTo("10.50");
        assertThat(CampaignMetrics.calculateEstimatedCost(BigDecimal.ZERO))
                .isEqualByComparingTo("0.00");
        assertThat(AnalyticsCalculations.calculateEstimatedCost(new BigDecimal("42.1")))
                .isEqualByComparingTo("42.10");
    }

    @Test
    void estimatedCostAllowsNull() {
        assertThat(CampaignMetrics.calculateEstimatedCost((BigDecimal) null)).isNull();
        assertThat(AnalyticsCalculations.calculateEstimatedCost((BigDecimal) null)).isNull();
    }

    @Test
    void estimatedCostRejectsNegativeValues() {
        assertThatThrownBy(() -> CampaignMetrics.calculateEstimatedCost(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estimated cost must not be negative");
        assertThatThrownBy(
                        () -> AnalyticsCalculations.calculateEstimatedCost(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void estimatedCostFromUnitCostTimesQuantity() {
        assertThat(CampaignMetrics.calculateEstimatedCost(new BigDecimal("0.05"), 1000))
                .isEqualByComparingTo("50.00");
        assertThat(AnalyticsCalculations.calculateEstimatedCost(new BigDecimal("1.25"), 4))
                .isEqualByComparingTo("5.00");
        assertThat(CampaignMetrics.calculateEstimatedCost(new BigDecimal("0.10"), 0))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void estimatedCostFromUnitCostRejectsInvalidInputs() {
        assertThatThrownBy(() -> CampaignMetrics.calculateEstimatedCost(null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit cost is required");
        assertThatThrownBy(
                        () -> CampaignMetrics.calculateEstimatedCost(new BigDecimal("-1"), 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit cost must not be negative");
        assertThatThrownBy(
                        () -> CampaignMetrics.calculateEstimatedCost(new BigDecimal("1.00"), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must not be negative");
    }

    @Test
    void updateFinancialEstimatesUsesCalculateEstimatedCost() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));

        metrics.updateFinancialEstimates(new BigDecimal("75.5"), new BigDecimal("100"));

        assertThat(metrics.getEstimatedCost()).isEqualByComparingTo("75.50");
        assertThat(metrics.calculateEstimatedCost()).isEqualByComparingTo("75.50");
        assertThat(AnalyticsCalculations.calculateEstimatedCost(metrics))
                .isEqualByComparingTo("75.50");
    }

    @Test
    void totalEstimatedCostSumsPerCampaignCosts() {
        CampaignMetrics a = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        a.updateFinancialEstimates(new BigDecimal("100.00"), new BigDecimal("150.00"));
        CampaignMetrics b = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_B, "B"));
        b.updateFinancialEstimates(new BigDecimal("25.50"), new BigDecimal("40.00"));
        CampaignMetrics c = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "C-no-cost"));
        // no financial estimates → null cost

        assertThat(AnalyticsCalculations.totalEstimatedCost(List.of(a, b, c)))
                .isEqualByComparingTo("125.50");
        assertThat(AnalyticsCalculations.totalEstimatedCost(List.of())).isNull();
        assertThat(AnalyticsCalculations.totalEstimatedCost(null)).isNull();
        assertThat(AnalyticsCalculations.totalEstimatedCost(List.of(c))).isNull();
    }

    @Test
    void dashboardEstimatedCostIsSumOfCampaignCosts() {
        Campaign campaignA = sampleCampaign(CAMPAIGN_A, "A");
        ReflectionTestUtils.setField(campaignA, "status", CampaignStatus.ACTIVE);
        Campaign campaignB = sampleCampaign(CAMPAIGN_B, "B");

        CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
        metricsA.recordLaunchCounts(80, 20, 70);
        metricsA.updateFinancialEstimates(new BigDecimal("100.00"), new BigDecimal("200.00"));
        CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
        metricsB.recordLaunchCounts(15, 5, 10);
        metricsB.updateFinancialEstimates(new BigDecimal("25.00"), new BigDecimal("50.00"));

        when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

        DashboardView dashboard = analyticsService.getDashboard();

        assertThat(dashboard.estimatedCost()).isEqualByComparingTo("125.00");
        assertThat(AnalyticsCalculations.totalEstimatedCost(List.of(metricsA, metricsB)))
                .isEqualByComparingTo(dashboard.estimatedCost());
    }

    @Test
    void executiveEstimatedCostMatchesTotalEstimatedCostCalculation() {
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
    }

    @Test
    void campaignMetricsViewExposesCalculatedEstimatedCost() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "View");
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(12, 3, 12);
        metrics.updateFinancialEstimates(new BigDecimal("60.00"), new BigDecimal("90.00"));
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        CampaignMetricsView view = CampaignMetricsView.from(metrics);

        assertThat(view.estimatedCost()).isEqualByComparingTo("60.00");
    }

    @Test
    void projectedCostFromUnitAndSentCanSeedFinancialEstimate() {
        // Domain helper: project cost from unit price × messages sent, then store.
        int sent = CampaignMetrics.calculateSentCount(200);
        BigDecimal projected =
                CampaignMetrics.calculateEstimatedCost(new BigDecimal("0.12"), sent);

        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "Proj"));
        metrics.recordLaunchCounts(200, 0, sent);
        metrics.updateFinancialEstimates(projected, null);

        assertThat(metrics.calculateEstimatedCost()).isEqualByComparingTo("24.00");
        assertThat(metrics.calculateSentCount()).isEqualTo(200);
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner = User.create("estcost-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
