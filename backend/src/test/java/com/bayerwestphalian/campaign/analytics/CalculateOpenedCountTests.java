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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 421: Calculate opened count.
 *
 * <p>Opened count is the number of OPENED contact events (BR-034), stored on {@code
 * campaign_metrics.opened_count} and summed for dashboard {@code openedCount} / executive {@code
 * totalOpened}. Open rate uses opened as numerator and sent as denominator.
 *
 * <p>Acceptance that contact events update engagement counters is formalized under KB item 450 in
 * {@code EngagementCountsUpdateFromContactEventsTests}.
 */
@ExtendWith(MockitoExtension.class)
class CalculateOpenedCountTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000421");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000422");

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
    void openedCountIsNonNegativeOpenTotal() {
        assertThat(CampaignMetrics.calculateOpenedCount(0)).isZero();
        assertThat(CampaignMetrics.calculateOpenedCount(1)).isEqualTo(1);
        assertThat(CampaignMetrics.calculateOpenedCount(100)).isEqualTo(100);

        assertThat(AnalyticsCalculations.calculateOpenedCount(42)).isEqualTo(42);
    }

    @Test
    void openedCountFromLongEventTallyIsConvertedSafely() {
        assertThat(CampaignMetrics.calculateOpenedCount(0L)).isZero();
        assertThat(CampaignMetrics.calculateOpenedCount(8L)).isEqualTo(8);
        assertThat(AnalyticsCalculations.calculateOpenedCount(15L)).isEqualTo(15);
    }

    @Test
    void openedCountRejectsNegativeValues() {
        assertThatThrownBy(() -> CampaignMetrics.calculateOpenedCount(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Opened count must not be negative");
        assertThatThrownBy(() -> CampaignMetrics.calculateOpenedCount(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Opened count must not be negative");
        assertThatThrownBy(() -> AnalyticsCalculations.calculateOpenedCount(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void openedCountRejectsLongValuesOutsideIntRange() {
        assertThatThrownBy(
                        () -> CampaignMetrics.calculateOpenedCount((long) Integer.MAX_VALUE + 1L))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void recordEngagementCountsStoresOpenedCount() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        metrics.recordLaunchCounts(10, 0, 10);

        metrics.recordEngagementCounts(5, 3, 2, 1);

        assertThat(metrics.getOpenedCount()).isEqualTo(5);
        assertThat(metrics.calculateOpenedCount()).isEqualTo(5);
        assertThat(AnalyticsCalculations.calculateOpenedCount(metrics)).isEqualTo(5);
        // Open rate uses opened / sent.
        assertThat(metrics.calculateOpenRate()).isEqualByComparingTo("0.5000");
    }

    @Test
    void incrementOpenedUsesCalculateOpenedCount() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        metrics.recordLaunchCounts(0, 0, 0);

        metrics.incrementOpened();
        metrics.incrementOpened();

        assertThat(metrics.calculateOpenedCount()).isEqualTo(2);
        assertThat(metrics.getOpenedCount()).isEqualTo(2);
    }

    @Test
    void totalOpenedCountSumsPerCampaignOpened() {
        CampaignMetrics a = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        a.recordLaunchCounts(80, 20, 70);
        a.recordEngagementCounts(35, 10, 5, 2);
        CampaignMetrics b = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_B, "B"));
        b.recordLaunchCounts(15, 5, 10);
        b.recordEngagementCounts(5, 2, 1, 1);

        assertThat(AnalyticsCalculations.totalOpenedCount(List.of(a, b))).isEqualTo(40L);
        assertThat(AnalyticsCalculations.totalOpenedCount(List.of())).isZero();
        assertThat(AnalyticsCalculations.totalOpenedCount(null)).isZero();
    }

    @Test
    void dashboardOpenedCountIsSumOfCampaignOpenedCounts() {
        Campaign campaignA = sampleCampaign(CAMPAIGN_A, "A");
        ReflectionTestUtils.setField(campaignA, "status", CampaignStatus.ACTIVE);
        Campaign campaignB = sampleCampaign(CAMPAIGN_B, "B");

        CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
        metricsA.recordLaunchCounts(80, 20, 70);
        metricsA.recordEngagementCounts(35, 10, 5, 2);
        CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
        metricsB.recordLaunchCounts(15, 5, 10);
        metricsB.recordEngagementCounts(5, 2, 1, 1);

        when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

        DashboardView dashboard = analyticsService.getDashboard();

        assertThat(dashboard.openedCount()).isEqualTo(40L);
        assertThat(dashboard.messagesSent()).isEqualTo(80L);
        assertThat(dashboard.recentCampaignMetrics())
                .extracting(CampaignMetricsView::openedCount)
                .containsExactlyInAnyOrder(35, 5);
    }

    @Test
    void executiveDashboardTotalOpenedMatchesTotalOpenedCalculation() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "Exec");
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(40, 10, 30);
        metrics.recordEngagementCounts(12, 4, 2, 1);

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
        when(campaignProductRepository.findAll()).thenReturn(List.of());

        ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

        assertThat(executive.totalOpened()).isEqualTo(12L);
        assertThat(executive.totalSent()).isEqualTo(30L);
    }

    @Test
    void campaignMetricsViewExposesCalculatedOpenedCount() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "View");
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(12, 3, 12);
        metrics.recordEngagementCounts(6, 2, 1, 0);
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        CampaignMetricsView view = CampaignMetricsView.from(metrics);

        assertThat(view.openedCount()).isEqualTo(6);
        assertThat(view.sentCount()).isEqualTo(12);
        assertThat(view.openRate()).isEqualByComparingTo("0.5000");
    }

    @Test
    void openedCountFromContactEventTallyFeedsOpenRateNumerator() {
        // Domain: OPENED events drive opened_count; open rate = opened / sent.
        int sent = CampaignMetrics.calculateSentCount(10L);
        int opened = CampaignMetrics.calculateOpenedCount(4L);

        assertThat(opened).isLessThanOrEqualTo(sent);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "Rate"));
        metrics.recordLaunchCounts(10, 0, sent);
        metrics.recordEngagementCounts(opened, 0, 0, 0);
        assertThat(metrics.calculateOpenRate()).isEqualByComparingTo("0.4000");
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner = User.create("opened-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
