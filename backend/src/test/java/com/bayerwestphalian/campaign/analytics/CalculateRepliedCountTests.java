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
 * KB item 423: Calculate replied count.
 *
 * <p>Acceptance that contact events update engagement counters is formalized under KB item 450.
 *
 * <p>Replied count is the number of REPLIED contact events (BR-034), stored on {@code
 * campaign_metrics.replied_count} and summed for dashboard {@code repliedCount} / executive {@code
 * totalReplied}.
 */
@ExtendWith(MockitoExtension.class)
class CalculateRepliedCountTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000423");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000424");

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
    void repliedCountIsNonNegativeReplyTotal() {
        assertThat(CampaignMetrics.calculateRepliedCount(0)).isZero();
        assertThat(CampaignMetrics.calculateRepliedCount(1)).isEqualTo(1);
        assertThat(CampaignMetrics.calculateRepliedCount(100)).isEqualTo(100);

        assertThat(AnalyticsCalculations.calculateRepliedCount(42)).isEqualTo(42);
    }

    @Test
    void repliedCountFromLongEventTallyIsConvertedSafely() {
        assertThat(CampaignMetrics.calculateRepliedCount(0L)).isZero();
        assertThat(CampaignMetrics.calculateRepliedCount(8L)).isEqualTo(8);
        assertThat(AnalyticsCalculations.calculateRepliedCount(15L)).isEqualTo(15);
    }

    @Test
    void repliedCountRejectsNegativeValues() {
        assertThatThrownBy(() -> CampaignMetrics.calculateRepliedCount(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Replied count must not be negative");
        assertThatThrownBy(() -> CampaignMetrics.calculateRepliedCount(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Replied count must not be negative");
        assertThatThrownBy(() -> AnalyticsCalculations.calculateRepliedCount(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void repliedCountRejectsLongValuesOutsideIntRange() {
        assertThatThrownBy(
                        () -> CampaignMetrics.calculateRepliedCount((long) Integer.MAX_VALUE + 1L))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void recordEngagementCountsStoresRepliedCount() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        metrics.recordLaunchCounts(10, 0, 10);

        metrics.recordEngagementCounts(5, 3, 2, 1);

        assertThat(metrics.getRepliedCount()).isEqualTo(2);
        assertThat(metrics.calculateRepliedCount()).isEqualTo(2);
        assertThat(AnalyticsCalculations.calculateRepliedCount(metrics)).isEqualTo(2);
    }

    @Test
    void incrementRepliedUsesCalculateRepliedCount() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        metrics.recordLaunchCounts(0, 0, 0);

        metrics.incrementReplied();
        metrics.incrementReplied();

        assertThat(metrics.calculateRepliedCount()).isEqualTo(2);
        assertThat(metrics.getRepliedCount()).isEqualTo(2);
    }

    @Test
    void totalRepliedCountSumsPerCampaignReplied() {
        CampaignMetrics a = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        a.recordLaunchCounts(80, 20, 70);
        a.recordEngagementCounts(35, 14, 7, 2);
        CampaignMetrics b = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_B, "B"));
        b.recordLaunchCounts(15, 5, 10);
        b.recordEngagementCounts(5, 2, 1, 1);

        assertThat(AnalyticsCalculations.totalRepliedCount(List.of(a, b))).isEqualTo(8L);
        assertThat(AnalyticsCalculations.totalRepliedCount(List.of())).isZero();
        assertThat(AnalyticsCalculations.totalRepliedCount(null)).isZero();
    }

    @Test
    void dashboardRepliedCountIsSumOfCampaignRepliedCounts() {
        Campaign campaignA = sampleCampaign(CAMPAIGN_A, "A");
        ReflectionTestUtils.setField(campaignA, "status", CampaignStatus.ACTIVE);
        Campaign campaignB = sampleCampaign(CAMPAIGN_B, "B");

        CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
        metricsA.recordLaunchCounts(80, 20, 70);
        metricsA.recordEngagementCounts(35, 14, 7, 2);
        CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
        metricsB.recordLaunchCounts(15, 5, 10);
        metricsB.recordEngagementCounts(5, 2, 1, 1);

        when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

        DashboardView dashboard = analyticsService.getDashboard();

        assertThat(dashboard.repliedCount()).isEqualTo(8L);
        assertThat(dashboard.clickedCount()).isEqualTo(16L);
        assertThat(dashboard.openedCount()).isEqualTo(40L);
        assertThat(dashboard.recentCampaignMetrics())
                .extracting(CampaignMetricsView::repliedCount)
                .containsExactlyInAnyOrder(7, 1);
    }

    @Test
    void executiveDashboardTotalRepliedMatchesTotalRepliedCalculation() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "Exec");
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(40, 10, 30);
        metrics.recordEngagementCounts(12, 6, 4, 1);

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
        when(campaignProductRepository.findAll()).thenReturn(List.of());

        ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

        assertThat(executive.totalReplied()).isEqualTo(4L);
        assertThat(executive.totalClicked()).isEqualTo(6L);
        assertThat(executive.totalOpened()).isEqualTo(12L);
        assertThat(executive.totalSent()).isEqualTo(30L);
    }

    @Test
    void campaignMetricsViewExposesCalculatedRepliedCount() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "View");
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(12, 3, 12);
        metrics.recordEngagementCounts(6, 3, 2, 0);
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        CampaignMetricsView view = CampaignMetricsView.from(metrics);

        assertThat(view.repliedCount()).isEqualTo(2);
        assertThat(view.clickedCount()).isEqualTo(3);
        assertThat(view.openedCount()).isEqualTo(6);
        assertThat(view.sentCount()).isEqualTo(12);
    }

    @Test
    void repliedCountFromContactEventTallyIsNonNegativeAndStored() {
        // Domain: REPLIED events drive replied_count (BR-034).
        int replied = CampaignMetrics.calculateRepliedCount(3L);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "Events"));
        metrics.recordLaunchCounts(10, 0, 10);
        metrics.recordEngagementCounts(5, 2, replied, 0);

        assertThat(metrics.calculateRepliedCount()).isEqualTo(3);
        assertThat(AnalyticsCalculations.calculateRepliedCount(metrics)).isEqualTo(3);
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner = User.create("replied-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
