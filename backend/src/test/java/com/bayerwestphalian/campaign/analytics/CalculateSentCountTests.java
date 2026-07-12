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
 * KB item 420 / FR-103: Calculate sent count.
 *
 * <p>Sent count is the number of messages successfully sent or queued (typically one SENT contact
 * event per eligible recipient at launch), stored on {@code campaign_metrics.sent_count} and
 * summed as dashboard {@code messagesSent}.
 *
 * <p>Acceptance coverage that {@code sent_count} updates after campaign launch is formalized under
 * KB item 449 in {@code SentCountUpdatesAfterLaunchTests}.
 */
@ExtendWith(MockitoExtension.class)
class CalculateSentCountTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000420");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000421");

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
    void sentCountIsNonNegativeMessageTotal() {
        assertThat(CampaignMetrics.calculateSentCount(0)).isZero();
        assertThat(CampaignMetrics.calculateSentCount(1)).isEqualTo(1);
        assertThat(CampaignMetrics.calculateSentCount(100)).isEqualTo(100);

        assertThat(AnalyticsCalculations.calculateSentCount(42)).isEqualTo(42);
    }

    @Test
    void sentCountFromLongEventTallyIsConvertedSafely() {
        assertThat(CampaignMetrics.calculateSentCount(0L)).isZero();
        assertThat(CampaignMetrics.calculateSentCount(8L)).isEqualTo(8);
        assertThat(AnalyticsCalculations.calculateSentCount(15L)).isEqualTo(15);
    }

    @Test
    void sentCountRejectsNegativeValues() {
        assertThatThrownBy(() -> CampaignMetrics.calculateSentCount(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sent count must not be negative");
        assertThatThrownBy(() -> CampaignMetrics.calculateSentCount(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sent count must not be negative");
        assertThatThrownBy(() -> AnalyticsCalculations.calculateSentCount(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sentCountRejectsLongValuesOutsideIntRange() {
        assertThatThrownBy(
                        () -> CampaignMetrics.calculateSentCount((long) Integer.MAX_VALUE + 1L))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void recordLaunchCountsStoresSentCount() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));

        metrics.recordLaunchCounts(8, 2, 8);

        assertThat(metrics.getSentCount()).isEqualTo(8);
        assertThat(metrics.calculateSentCount()).isEqualTo(8);
        assertThat(AnalyticsCalculations.calculateSentCount(metrics)).isEqualTo(8);
        // Rates use sent count as denominator.
        metrics.recordEngagementCounts(4, 2, 1, 1);
        assertThat(metrics.calculateOpenRate()).isEqualByComparingTo("0.5000");
    }

    @Test
    void incrementSentUsesCalculateSentCount() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        metrics.recordLaunchCounts(0, 0, 0);

        metrics.incrementSent();
        metrics.incrementSent();

        assertThat(metrics.calculateSentCount()).isEqualTo(2);
        assertThat(metrics.getSentCount()).isEqualTo(2);
    }

    @Test
    void totalSentCountSumsPerCampaignSent() {
        CampaignMetrics a = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        a.recordLaunchCounts(80, 20, 70);
        CampaignMetrics b = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_B, "B"));
        b.recordLaunchCounts(15, 5, 10);

        assertThat(AnalyticsCalculations.totalSentCount(List.of(a, b))).isEqualTo(80L);
        assertThat(AnalyticsCalculations.totalSentCount(List.of())).isZero();
        assertThat(AnalyticsCalculations.totalSentCount(null)).isZero();
    }

    @Test
    void dashboardMessagesSentIsSumOfCampaignSentCounts() {
        Campaign campaignA = sampleCampaign(CAMPAIGN_A, "A");
        ReflectionTestUtils.setField(campaignA, "status", CampaignStatus.ACTIVE);
        Campaign campaignB = sampleCampaign(CAMPAIGN_B, "B");

        CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
        metricsA.recordLaunchCounts(80, 20, 70);
        CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
        metricsB.recordLaunchCounts(15, 5, 10);

        when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

        DashboardView dashboard = analyticsService.getDashboard();

        // FR-103: dashboard shows messages sent.
        assertThat(dashboard.messagesSent()).isEqualTo(80L);
        assertThat(dashboard.recentCampaignMetrics())
                .extracting(CampaignMetricsView::sentCount)
                .containsExactlyInAnyOrder(70, 10);
    }

    @Test
    void executiveDashboardTotalSentMatchesTotalSentCalculation() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "Exec");
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(40, 10, 30);

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
        when(campaignProductRepository.findAll()).thenReturn(List.of());

        ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

        assertThat(executive.totalSent()).isEqualTo(30L);
        assertThat(executive.totalEligible()).isEqualTo(40L);
    }

    @Test
    void campaignMetricsViewExposesCalculatedSentCount() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "View");
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(12, 3, 12);
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        CampaignMetricsView view = CampaignMetricsView.from(metrics);

        assertThat(view.sentCount()).isEqualTo(12);
        assertThat(view.eligibleCount()).isEqualTo(12);
        assertThat(view.excludedCount()).isEqualTo(3);
    }

    @Test
    void launchSentCountTypicallyDoesNotExceedEligibleCount() {
        // Domain expectation at launch: one SENT event per eligible recipient that was messaged.
        int eligible = 10;
        int sent = CampaignMetrics.calculateSentCount(10L);
        int excluded = CampaignMetrics.calculateExcludedCount(3L);

        assertThat(sent).isLessThanOrEqualTo(eligible);
        assertThat(CampaignMetrics.calculateAudienceSize(eligible, excluded)).isEqualTo(13);
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner = User.create("sent-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
