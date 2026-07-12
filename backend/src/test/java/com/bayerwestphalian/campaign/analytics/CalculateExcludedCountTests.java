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
 * KB item 419: Calculate excluded count.
 *
 * <p>Excluded count is the number of campaign recipients with status {@code EXCLUDED} (consent,
 * do-not-contact, contact limits, etc.), stored on {@code campaign_metrics.excluded_count} at
 * launch and summed for dashboard / executive totals.
 *
 * <p>Acceptance coverage for the same rule is also formalized under KB item 448 in {@link
 * ExcludedCountIsCalculatedCorrectlyTests}.
 */
@ExtendWith(MockitoExtension.class)
class CalculateExcludedCountTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000419");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000420");

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
    void excludedCountIsNonNegativeRecipientTotal() {
        assertThat(CampaignMetrics.calculateExcludedCount(0)).isZero();
        assertThat(CampaignMetrics.calculateExcludedCount(1)).isEqualTo(1);
        assertThat(CampaignMetrics.calculateExcludedCount(100)).isEqualTo(100);

        assertThat(AnalyticsCalculations.calculateExcludedCount(42)).isEqualTo(42);
    }

    @Test
    void excludedCountFromRecipientRepositoryLongIsConvertedSafely() {
        assertThat(CampaignMetrics.calculateExcludedCount(0L)).isZero();
        assertThat(CampaignMetrics.calculateExcludedCount(8L)).isEqualTo(8);
        assertThat(AnalyticsCalculations.calculateExcludedCount(15L)).isEqualTo(15);
    }

    @Test
    void excludedCountRejectsNegativeValues() {
        assertThatThrownBy(() -> CampaignMetrics.calculateExcludedCount(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Excluded count must not be negative");
        assertThatThrownBy(() -> CampaignMetrics.calculateExcludedCount(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Excluded count must not be negative");
        assertThatThrownBy(() -> AnalyticsCalculations.calculateExcludedCount(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void excludedCountRejectsLongValuesOutsideIntRange() {
        assertThatThrownBy(
                        () ->
                                CampaignMetrics.calculateExcludedCount(
                                        (long) Integer.MAX_VALUE + 1L))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void recordLaunchCountsStoresExcludedCount() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));

        metrics.recordLaunchCounts(8, 2, 8);

        assertThat(metrics.getExcludedCount()).isEqualTo(2);
        assertThat(metrics.calculateExcludedCount()).isEqualTo(2);
        assertThat(AnalyticsCalculations.calculateExcludedCount(metrics)).isEqualTo(2);
        assertThat(metrics.calculateEligibleCount()).isEqualTo(8);
        assertThat(metrics.calculateAudienceSize()).isEqualTo(10);
    }

    @Test
    void totalExcludedCountSumsPerCampaignExcluded() {
        CampaignMetrics a = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        a.recordLaunchCounts(80, 20, 70);
        CampaignMetrics b = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_B, "B"));
        b.recordLaunchCounts(15, 5, 10);

        assertThat(AnalyticsCalculations.totalExcludedCount(List.of(a, b))).isEqualTo(25L);
        assertThat(AnalyticsCalculations.totalExcludedCount(List.of())).isZero();
        assertThat(AnalyticsCalculations.totalExcludedCount(null)).isZero();
    }

    @Test
    void dashboardExcludedCountIsSumOfCampaignExcludedCounts() {
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

        assertThat(dashboard.excludedCount()).isEqualTo(25L);
        assertThat(dashboard.eligibleCount()).isEqualTo(95L);
        assertThat(dashboard.audienceSize())
                .isEqualTo(dashboard.eligibleCount() + dashboard.excludedCount());
        assertThat(dashboard.recentCampaignMetrics())
                .extracting(CampaignMetricsView::excludedCount)
                .containsExactlyInAnyOrder(20, 5);
    }

    @Test
    void executiveDashboardExcludedCountMatchesTotalExcludedCalculation() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "Exec");
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(40, 10, 30);

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
        when(campaignProductRepository.findAll()).thenReturn(List.of());

        ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

        assertThat(executive.totalExcluded()).isEqualTo(10L);
        assertThat(executive.totalEligible()).isEqualTo(40L);
        assertThat(executive.totalAudience())
                .isEqualTo(executive.totalEligible() + executive.totalExcluded());
    }

    @Test
    void campaignMetricsViewExposesCalculatedExcludedCount() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "View");
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(12, 3, 12);
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        CampaignMetricsView view = CampaignMetricsView.from(metrics);

        assertThat(view.excludedCount()).isEqualTo(3);
        assertThat(view.eligibleCount()).isEqualTo(12);
        assertThat(view.audienceSize()).isEqualTo(15);
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner = User.create("excluded-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
