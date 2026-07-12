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
 * KB item 429: Calculate estimated revenue.
 *
 * <p>Estimated revenue is an optional non-negative monetary value stored on {@code
 * campaign_metrics.estimated_revenue} (scale 2). It can also be projected as unit revenue ×
 * quantity. Dashboard / executive totals sum per-campaign estimated revenues.
 */
@ExtendWith(MockitoExtension.class)
class CalculateEstimatedRevenueTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000429");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000430");

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
    void estimatedRevenueNormalizesToScaleTwo() {
        assertThat(CampaignMetrics.calculateEstimatedRevenue(new BigDecimal("150")))
                .isEqualByComparingTo("150.00");
        assertThat(CampaignMetrics.calculateEstimatedRevenue(new BigDecimal("10.5")))
                .isEqualByComparingTo("10.50");
        assertThat(CampaignMetrics.calculateEstimatedRevenue(BigDecimal.ZERO))
                .isEqualByComparingTo("0.00");
        assertThat(AnalyticsCalculations.calculateEstimatedRevenue(new BigDecimal("42.1")))
                .isEqualByComparingTo("42.10");
    }

    @Test
    void estimatedRevenueAllowsNull() {
        assertThat(CampaignMetrics.calculateEstimatedRevenue((BigDecimal) null)).isNull();
        assertThat(AnalyticsCalculations.calculateEstimatedRevenue((BigDecimal) null)).isNull();
    }

    @Test
    void estimatedRevenueRejectsNegativeValues() {
        assertThatThrownBy(
                        () -> CampaignMetrics.calculateEstimatedRevenue(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estimated revenue must not be negative");
        assertThatThrownBy(
                        () ->
                                AnalyticsCalculations.calculateEstimatedRevenue(
                                        new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void estimatedRevenueFromUnitRevenueTimesQuantity() {
        assertThat(CampaignMetrics.calculateEstimatedRevenue(new BigDecimal("25.00"), 4))
                .isEqualByComparingTo("100.00");
        assertThat(AnalyticsCalculations.calculateEstimatedRevenue(new BigDecimal("1.25"), 4))
                .isEqualByComparingTo("5.00");
        assertThat(CampaignMetrics.calculateEstimatedRevenue(new BigDecimal("10.00"), 0))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void estimatedRevenueFromUnitRevenueRejectsInvalidInputs() {
        assertThatThrownBy(() -> CampaignMetrics.calculateEstimatedRevenue(null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit revenue is required");
        assertThatThrownBy(
                        () -> CampaignMetrics.calculateEstimatedRevenue(new BigDecimal("-1"), 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit revenue must not be negative");
        assertThatThrownBy(
                        () -> CampaignMetrics.calculateEstimatedRevenue(new BigDecimal("1.00"), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must not be negative");
    }

    @Test
    void updateFinancialEstimatesUsesCalculateEstimatedRevenue() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));

        metrics.updateFinancialEstimates(new BigDecimal("50.00"), new BigDecimal("75.5"));

        assertThat(metrics.getEstimatedRevenue()).isEqualByComparingTo("75.50");
        assertThat(metrics.calculateEstimatedRevenue()).isEqualByComparingTo("75.50");
        assertThat(AnalyticsCalculations.calculateEstimatedRevenue(metrics))
                .isEqualByComparingTo("75.50");
    }

    @Test
    void totalEstimatedRevenueSumsPerCampaignRevenues() {
        CampaignMetrics a = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        a.updateFinancialEstimates(new BigDecimal("100.00"), new BigDecimal("150.00"));
        CampaignMetrics b = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_B, "B"));
        b.updateFinancialEstimates(new BigDecimal("25.50"), new BigDecimal("40.25"));
        CampaignMetrics c = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "C-no-rev"));
        // no financial estimates → null revenue

        assertThat(AnalyticsCalculations.totalEstimatedRevenue(List.of(a, b, c)))
                .isEqualByComparingTo("190.25");
        assertThat(AnalyticsCalculations.totalEstimatedRevenue(List.of())).isNull();
        assertThat(AnalyticsCalculations.totalEstimatedRevenue(null)).isNull();
        assertThat(AnalyticsCalculations.totalEstimatedRevenue(List.of(c))).isNull();
    }

    @Test
    void dashboardEstimatedRevenueIsSumOfCampaignRevenues() {
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

        assertThat(dashboard.estimatedRevenue()).isEqualByComparingTo("250.00");
        assertThat(dashboard.estimatedCost()).isEqualByComparingTo("125.00");
        assertThat(AnalyticsCalculations.totalEstimatedRevenue(List.of(metricsA, metricsB)))
                .isEqualByComparingTo(dashboard.estimatedRevenue());
    }

    @Test
    void executiveEstimatedRevenueMatchesTotalEstimatedRevenueCalculation() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "Exec");
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(40, 10, 30);
        metrics.updateFinancialEstimates(new BigDecimal("80.00"), new BigDecimal("120.00"));

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
        when(campaignProductRepository.findAll()).thenReturn(List.of());

        ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

        assertThat(executive.totalEstimatedRevenue()).isEqualByComparingTo("120.00");
        assertThat(executive.totalEstimatedCost()).isEqualByComparingTo("80.00");
    }

    @Test
    void campaignMetricsViewExposesCalculatedEstimatedRevenue() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "View");
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(12, 3, 12);
        metrics.updateFinancialEstimates(new BigDecimal("60.00"), new BigDecimal("90.00"));
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        CampaignMetricsView view = CampaignMetricsView.from(metrics);

        assertThat(view.estimatedRevenue()).isEqualByComparingTo("90.00");
        assertThat(view.estimatedCost()).isEqualByComparingTo("60.00");
    }

    @Test
    void projectedRevenueFromUnitAndConversionsCanSeedFinancialEstimate() {
        // Domain helper: project revenue from unit value × conversions, then store.
        int converted = CampaignMetrics.calculateConvertedCount(5);
        BigDecimal projected =
                CampaignMetrics.calculateEstimatedRevenue(new BigDecimal("30.00"), converted);

        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "Proj"));
        metrics.recordLaunchCounts(200, 0, 200);
        metrics.recordEngagementCounts(50, 20, 10, converted);
        metrics.updateFinancialEstimates(new BigDecimal("24.00"), projected);

        assertThat(metrics.calculateEstimatedRevenue()).isEqualByComparingTo("150.00");
        assertThat(metrics.calculateConvertedCount()).isEqualTo(5);
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner = User.create("estrev-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
