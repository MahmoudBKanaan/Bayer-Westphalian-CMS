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
 * KB item 422: Calculate clicked count.
 *
 * <p>Acceptance that contact events update engagement counters is formalized under KB item 450.
 *
 * <p>Clicked count is the number of CLICKED contact events (BR-034), stored on {@code
 * campaign_metrics.clicked_count} and summed for dashboard {@code clickedCount} / executive {@code
 * totalClicked}. Click rate uses clicked as numerator and sent as denominator.
 */
@ExtendWith(MockitoExtension.class)
class CalculateClickedCountTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000422");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000423");

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
    void clickedCountIsNonNegativeClickTotal() {
        assertThat(CampaignMetrics.calculateClickedCount(0)).isZero();
        assertThat(CampaignMetrics.calculateClickedCount(1)).isEqualTo(1);
        assertThat(CampaignMetrics.calculateClickedCount(100)).isEqualTo(100);

        assertThat(AnalyticsCalculations.calculateClickedCount(42)).isEqualTo(42);
    }

    @Test
    void clickedCountFromLongEventTallyIsConvertedSafely() {
        assertThat(CampaignMetrics.calculateClickedCount(0L)).isZero();
        assertThat(CampaignMetrics.calculateClickedCount(8L)).isEqualTo(8);
        assertThat(AnalyticsCalculations.calculateClickedCount(15L)).isEqualTo(15);
    }

    @Test
    void clickedCountRejectsNegativeValues() {
        assertThatThrownBy(() -> CampaignMetrics.calculateClickedCount(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Clicked count must not be negative");
        assertThatThrownBy(() -> CampaignMetrics.calculateClickedCount(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Clicked count must not be negative");
        assertThatThrownBy(() -> AnalyticsCalculations.calculateClickedCount(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clickedCountRejectsLongValuesOutsideIntRange() {
        assertThatThrownBy(
                        () -> CampaignMetrics.calculateClickedCount((long) Integer.MAX_VALUE + 1L))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void recordEngagementCountsStoresClickedCount() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        metrics.recordLaunchCounts(10, 0, 10);

        metrics.recordEngagementCounts(5, 3, 2, 1);

        assertThat(metrics.getClickedCount()).isEqualTo(3);
        assertThat(metrics.calculateClickedCount()).isEqualTo(3);
        assertThat(AnalyticsCalculations.calculateClickedCount(metrics)).isEqualTo(3);
        // Click rate uses clicked / sent.
        assertThat(metrics.calculateClickRate()).isEqualByComparingTo("0.3000");
    }

    @Test
    void incrementClickedUsesCalculateClickedCount() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        metrics.recordLaunchCounts(0, 0, 0);

        metrics.incrementClicked();
        metrics.incrementClicked();

        assertThat(metrics.calculateClickedCount()).isEqualTo(2);
        assertThat(metrics.getClickedCount()).isEqualTo(2);
    }

    @Test
    void totalClickedCountSumsPerCampaignClicked() {
        CampaignMetrics a = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        a.recordLaunchCounts(80, 20, 70);
        a.recordEngagementCounts(35, 14, 5, 2);
        CampaignMetrics b = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_B, "B"));
        b.recordLaunchCounts(15, 5, 10);
        b.recordEngagementCounts(5, 2, 1, 1);

        assertThat(AnalyticsCalculations.totalClickedCount(List.of(a, b))).isEqualTo(16L);
        assertThat(AnalyticsCalculations.totalClickedCount(List.of())).isZero();
        assertThat(AnalyticsCalculations.totalClickedCount(null)).isZero();
    }

    @Test
    void dashboardClickedCountIsSumOfCampaignClickedCounts() {
        Campaign campaignA = sampleCampaign(CAMPAIGN_A, "A");
        ReflectionTestUtils.setField(campaignA, "status", CampaignStatus.ACTIVE);
        Campaign campaignB = sampleCampaign(CAMPAIGN_B, "B");

        CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
        metricsA.recordLaunchCounts(80, 20, 70);
        metricsA.recordEngagementCounts(35, 14, 5, 2);
        CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
        metricsB.recordLaunchCounts(15, 5, 10);
        metricsB.recordEngagementCounts(5, 2, 1, 1);

        when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

        DashboardView dashboard = analyticsService.getDashboard();

        assertThat(dashboard.clickedCount()).isEqualTo(16L);
        assertThat(dashboard.openedCount()).isEqualTo(40L);
        assertThat(dashboard.messagesSent()).isEqualTo(80L);
        assertThat(dashboard.recentCampaignMetrics())
                .extracting(CampaignMetricsView::clickedCount)
                .containsExactlyInAnyOrder(14, 2);
    }

    @Test
    void executiveDashboardTotalClickedMatchesTotalClickedCalculation() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "Exec");
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(40, 10, 30);
        metrics.recordEngagementCounts(12, 6, 2, 1);

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
        when(campaignProductRepository.findAll()).thenReturn(List.of());

        ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

        assertThat(executive.totalClicked()).isEqualTo(6L);
        assertThat(executive.totalOpened()).isEqualTo(12L);
        assertThat(executive.totalSent()).isEqualTo(30L);
    }

    @Test
    void campaignMetricsViewExposesCalculatedClickedCount() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "View");
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(12, 3, 12);
        metrics.recordEngagementCounts(6, 3, 1, 0);
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        CampaignMetricsView view = CampaignMetricsView.from(metrics);

        assertThat(view.clickedCount()).isEqualTo(3);
        assertThat(view.openedCount()).isEqualTo(6);
        assertThat(view.sentCount()).isEqualTo(12);
        assertThat(view.clickRate()).isEqualByComparingTo("0.2500");
    }

    @Test
    void clickedCountFromContactEventTallyFeedsClickRateNumerator() {
        // Domain: CLICKED events drive clicked_count; click rate = clicked / sent.
        int sent = CampaignMetrics.calculateSentCount(10L);
        int clicked = CampaignMetrics.calculateClickedCount(2L);

        assertThat(clicked).isLessThanOrEqualTo(sent);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "Rate"));
        metrics.recordLaunchCounts(10, 0, sent);
        metrics.recordEngagementCounts(0, clicked, 0, 0);
        assertThat(metrics.calculateClickRate()).isEqualByComparingTo("0.2000");
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner = User.create("clicked-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
