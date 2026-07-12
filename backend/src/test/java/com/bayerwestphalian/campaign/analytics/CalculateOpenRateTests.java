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
 * KB item 425 / FR-104: Calculate open rate.
 *
 * <p>Open rate = opened_count / sent_count when sent &gt; 0; otherwise {@link BigDecimal#ZERO} at
 * scale 4. Dashboard and executive views compute the rate from aggregate opened and sent totals.
 *
 * <p>Acceptance coverage for the same rule is also formalized under KB item 451 in {@link
 * OpenRateIsCalculatedCorrectlyTests}.
 */
@ExtendWith(MockitoExtension.class)
class CalculateOpenRateTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000425");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000426");

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
    void openRateIsOpenedDividedBySent() {
        assertThat(CampaignMetrics.calculateOpenRate(5, 10)).isEqualByComparingTo("0.5000");
        assertThat(CampaignMetrics.calculateOpenRate(1, 4)).isEqualByComparingTo("0.2500");
        assertThat(CampaignMetrics.calculateOpenRate(0, 10)).isEqualByComparingTo("0.0000");
        assertThat(CampaignMetrics.calculateOpenRate(3, 3)).isEqualByComparingTo("1.0000");

        assertThat(AnalyticsCalculations.calculateOpenRate(40L, 80L)).isEqualByComparingTo("0.5000");
        assertThat(AnalyticsRates.openRate(40L, 80L)).isEqualByComparingTo("0.5000");
    }

    @Test
    void openRateIsZeroWhenNothingSent() {
        assertThat(CampaignMetrics.calculateOpenRate(0, 0)).isEqualByComparingTo(ZERO_RATE);
        assertThat(CampaignMetrics.calculateOpenRate(5, 0)).isEqualByComparingTo(ZERO_RATE);
        assertThat(CampaignMetrics.calculateOpenRate(5L, 0L)).isEqualByComparingTo(ZERO_RATE);
        assertThat(AnalyticsCalculations.calculateOpenRate(1L, 0L)).isEqualByComparingTo(ZERO_RATE);
        assertThat(AnalyticsRates.openRate(1L, 0L)).isEqualByComparingTo(ZERO_RATE);
    }

    @Test
    void openRateRejectsNegativeInputs() {
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
    void openRateUsesScaleFourHalfUp() {
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
        assertThat(AnalyticsCalculations.calculateOpenRate(metrics)).isEqualByComparingTo("0.4000");
        assertThat(CampaignMetrics.calculateOpenRate(
                        metrics.calculateOpenedCount(), metrics.calculateSentCount()))
                .isEqualByComparingTo("0.4000");
    }

    @Test
    void dashboardOpenRateUsesAggregateOpenedOverSent() {
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

        // FR-104: dashboard shows open rate = total opened / total sent = 40 / 80.
        assertThat(dashboard.openedCount()).isEqualTo(40L);
        assertThat(dashboard.messagesSent()).isEqualTo(80L);
        assertThat(dashboard.openRate()).isEqualByComparingTo("0.5000");
        assertThat(AnalyticsCalculations.calculateOpenRate(
                        dashboard.openedCount(), dashboard.messagesSent()))
                .isEqualByComparingTo(dashboard.openRate());
    }

    @Test
    void executiveOpenRateUsesAggregateOpenedOverSent() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "Exec");
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
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
    }

    @Test
    void campaignMetricsViewExposesCalculatedOpenRate() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "View");
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(12, 3, 12);
        metrics.recordEngagementCounts(6, 3, 2, 1);
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        CampaignMetricsView view = CampaignMetricsView.from(metrics);

        assertThat(view.openedCount()).isEqualTo(6);
        assertThat(view.sentCount()).isEqualTo(12);
        assertThat(view.openRate()).isEqualByComparingTo("0.5000");
    }

    @Test
    void openRateIsZeroOnEmptyDashboard() {
        when(campaignRepository.findAll()).thenReturn(List.of());
        when(campaignMetricsRepository.findAll()).thenReturn(List.of());

        DashboardView dashboard = analyticsService.getDashboard();

        assertThat(dashboard.openedCount()).isZero();
        assertThat(dashboard.messagesSent()).isZero();
        assertThat(dashboard.openRate()).isEqualByComparingTo(ZERO_RATE);
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner = User.create("openrate-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
