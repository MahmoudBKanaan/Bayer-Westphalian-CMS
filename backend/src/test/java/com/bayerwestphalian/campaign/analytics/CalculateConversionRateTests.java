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
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 427 / FR-106: Calculate conversion rate.
 *
 * <p>Conversion rate = converted_count / sent_count when sent &gt; 0; otherwise {@link
 * BigDecimal#ZERO} at scale 4. Dashboard and executive views compute the rate from aggregate
 * converted and sent totals.
 *
 * <p>Acceptance coverage for the same rule is also formalized under KB item 453 in {@link
 * ConversionRateIsCalculatedCorrectlyTests}.
 */
@ExtendWith(MockitoExtension.class)
class CalculateConversionRateTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000427");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000428");

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

    @Test
    void conversionRateIsConvertedDividedBySent() {
        assertThat(CampaignMetrics.calculateConversionRate(1, 10)).isEqualByComparingTo("0.1000");
        assertThat(CampaignMetrics.calculateConversionRate(1, 4)).isEqualByComparingTo("0.2500");
        assertThat(CampaignMetrics.calculateConversionRate(0, 10)).isEqualByComparingTo("0.0000");
        assertThat(CampaignMetrics.calculateConversionRate(3, 3)).isEqualByComparingTo("1.0000");

        assertThat(AnalyticsCalculations.calculateConversionRate(5L, 80L))
                .isEqualByComparingTo("0.0625");
        assertThat(AnalyticsRates.conversionRate(5L, 80L)).isEqualByComparingTo("0.0625");
    }

    @Test
    void conversionRateIsZeroWhenNothingSent() {
        assertThat(CampaignMetrics.calculateConversionRate(0, 0)).isEqualByComparingTo(ZERO_RATE);
        assertThat(CampaignMetrics.calculateConversionRate(5, 0)).isEqualByComparingTo(ZERO_RATE);
        assertThat(CampaignMetrics.calculateConversionRate(5L, 0L)).isEqualByComparingTo(ZERO_RATE);
        assertThat(AnalyticsCalculations.calculateConversionRate(1L, 0L))
                .isEqualByComparingTo(ZERO_RATE);
        assertThat(AnalyticsRates.conversionRate(1L, 0L)).isEqualByComparingTo(ZERO_RATE);
    }

    @Test
    void conversionRateRejectsNegativeInputs() {
        assertThatThrownBy(() -> CampaignMetrics.calculateConversionRate(-1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Converted count must not be negative");
        assertThatThrownBy(() -> CampaignMetrics.calculateConversionRate(1, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sent count must not be negative");
        assertThatThrownBy(() -> CampaignMetrics.calculateConversionRate(-1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Converted count must not be negative");
        assertThatThrownBy(() -> AnalyticsCalculations.calculateConversionRate(-5L, 10L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conversionRateUsesScaleFourHalfUp() {
        // 1/3 = 0.3333..., HALF_UP at scale 4 → 0.3333
        assertThat(CampaignMetrics.calculateConversionRate(1, 3)).isEqualByComparingTo("0.3333");
        // 2/3 = 0.6666..., HALF_UP at scale 4 → 0.6667
        assertThat(CampaignMetrics.calculateConversionRate(2, 3)).isEqualByComparingTo("0.6667");
    }

    @Test
    void metricsRowConversionRateUsesConvertedAndSentCounts() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        metrics.recordLaunchCounts(10, 0, 10);
        metrics.recordEngagementCounts(0, 0, 0, 1);

        assertThat(metrics.calculateConversionRate()).isEqualByComparingTo("0.1000");
        assertThat(AnalyticsCalculations.calculateConversionRate(metrics))
                .isEqualByComparingTo("0.1000");
        assertThat(CampaignMetrics.calculateConversionRate(
                        metrics.calculateConvertedCount(), metrics.calculateSentCount()))
                .isEqualByComparingTo("0.1000");
    }

    @Test
    void dashboardConversionRateUsesAggregateConvertedOverSent() {
        Campaign campaignA = sampleCampaign(CAMPAIGN_A, "A");
        ReflectionTestUtils.setField(campaignA, "status", CampaignStatus.ACTIVE);
        Campaign campaignB = sampleCampaign(CAMPAIGN_B, "B");

        CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
        metricsA.recordLaunchCounts(80, 20, 70);
        metricsA.recordEngagementCounts(35, 14, 7, 4);
        CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
        metricsB.recordLaunchCounts(15, 5, 10);
        metricsB.recordEngagementCounts(5, 2, 1, 1);

        when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

        DashboardView dashboard = analyticsService.getDashboard();

        // FR-106: dashboard shows conversion rate = total converted / total sent = 5 / 80.
        assertThat(dashboard.convertedCount()).isEqualTo(5L);
        assertThat(dashboard.messagesSent()).isEqualTo(80L);
        assertThat(dashboard.conversionRate()).isEqualByComparingTo("0.0625");
        assertThat(AnalyticsCalculations.calculateConversionRate(
                        dashboard.convertedCount(), dashboard.messagesSent()))
                .isEqualByComparingTo(dashboard.conversionRate());
    }

    @Test
    void executiveConversionRateUsesAggregateConvertedOverSent() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "Exec");
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(40, 10, 30);
        metrics.recordEngagementCounts(12, 6, 4, 3);

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
        when(campaignProductRepository.findAll()).thenReturn(List.of());

        ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

        // 3 / 30 = 0.1
        assertThat(executive.totalConverted()).isEqualTo(3L);
        assertThat(executive.totalSent()).isEqualTo(30L);
        assertThat(executive.overallConversionRate()).isEqualByComparingTo("0.1000");
    }

    @Test
    void campaignMetricsViewExposesCalculatedConversionRate() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "View");
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(12, 3, 12);
        metrics.recordEngagementCounts(6, 3, 2, 1);
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        CampaignMetricsView view = CampaignMetricsView.from(metrics);

        assertThat(view.convertedCount()).isEqualTo(1);
        assertThat(view.sentCount()).isEqualTo(12);
        // 1/12 = 0.0833... → 0.0833 at scale 4 HALF_UP
        assertThat(view.conversionRate()).isEqualByComparingTo("0.0833");
        assertThat(view.clickRate()).isEqualByComparingTo("0.2500");
        assertThat(view.openRate()).isEqualByComparingTo("0.5000");
    }

    @Test
    void conversionRateIsZeroOnEmptyDashboard() {
        when(campaignRepository.findAll()).thenReturn(List.of());
        when(campaignMetricsRepository.findAll()).thenReturn(List.of());

        DashboardView dashboard = analyticsService.getDashboard();

        assertThat(dashboard.convertedCount()).isZero();
        assertThat(dashboard.messagesSent()).isZero();
        assertThat(dashboard.conversionRate()).isEqualByComparingTo(ZERO_RATE);
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner =
                User.create("convrate-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
