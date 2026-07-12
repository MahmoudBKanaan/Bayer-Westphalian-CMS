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
 * KB item 417 / FR-102: Calculate audience size.
 *
 * <p>Audience size for a campaign is {@code eligible_count + excluded_count}. Dashboard and
 * executive totals sum per-campaign audience sizes.
 *
 * <p>Acceptance coverage for the same rule is also formalized under KB item 446 in {@link
 * AudienceSizeIsCalculatedCorrectlyTests}.
 */
@ExtendWith(MockitoExtension.class)
class CalculateAudienceSizeTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000417");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000418");

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
    void audienceSizeIsEligiblePlusExcluded() {
        assertThat(CampaignMetrics.calculateAudienceSize(8, 2)).isEqualTo(10);
        assertThat(CampaignMetrics.calculateAudienceSize(0, 0)).isZero();
        assertThat(CampaignMetrics.calculateAudienceSize(100, 0)).isEqualTo(100);
        assertThat(CampaignMetrics.calculateAudienceSize(0, 15)).isEqualTo(15);

        assertThat(AnalyticsCalculations.calculateAudienceSize(80, 20)).isEqualTo(100);
    }

    @Test
    void audienceSizeRejectsNegativeCounts() {
        assertThatThrownBy(() -> CampaignMetrics.calculateAudienceSize(-1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Eligible count must not be negative");
        assertThatThrownBy(() -> CampaignMetrics.calculateAudienceSize(0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Excluded count must not be negative");
    }

    @Test
    void recordLaunchCountsStoresCalculatedAudienceSize() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));

        metrics.recordLaunchCounts(8, 2, 8);

        assertThat(metrics.getEligibleCount()).isEqualTo(8);
        assertThat(metrics.getExcludedCount()).isEqualTo(2);
        assertThat(metrics.calculateAudienceSize()).isEqualTo(10);
        assertThat(metrics.getAudienceSize()).isEqualTo(10);
    }

    @Test
    void calculateAudienceSizeIgnoresStaleStoredAudienceField() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        metrics.recordLaunchCounts(8, 2, 8);
        // Simulate stale/incorrect stored audience_size column.
        ReflectionTestUtils.setField(metrics, "audienceSize", 999);

        assertThat(metrics.getAudienceSize()).isEqualTo(999);
        assertThat(metrics.calculateAudienceSize()).isEqualTo(10);
        assertThat(AnalyticsCalculations.calculateAudienceSize(metrics)).isEqualTo(10);

        metrics.recalculate();
        assertThat(metrics.getAudienceSize()).isEqualTo(10);
    }

    @Test
    void totalAudienceSizeSumsPerCampaignAudience() {
        CampaignMetrics a = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        a.recordLaunchCounts(80, 20, 70);
        CampaignMetrics b = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_B, "B"));
        b.recordLaunchCounts(15, 5, 10);

        assertThat(AnalyticsCalculations.totalAudienceSize(List.of(a, b))).isEqualTo(120L);
        assertThat(AnalyticsCalculations.totalAudienceSize(List.of())).isZero();
        assertThat(AnalyticsCalculations.totalAudienceSize(null)).isZero();
    }

    @Test
    void dashboardAudienceSizeIsSumOfCampaignAudienceSizes() {
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

        assertThat(dashboard.audienceSize()).isEqualTo(120L);
        assertThat(dashboard.eligibleCount()).isEqualTo(95L);
        assertThat(dashboard.excludedCount()).isEqualTo(25L);
        assertThat(dashboard.audienceSize())
                .isEqualTo(dashboard.eligibleCount() + dashboard.excludedCount());
    }

    @Test
    void dashboardAudienceSizeUsesDerivedValuesWhenStoredAudienceIsStale() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "Stale");
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(10, 5, 10);
        ReflectionTestUtils.setField(metrics, "audienceSize", 1);

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));

        DashboardView dashboard = analyticsService.getDashboard();

        assertThat(metrics.getAudienceSize()).isEqualTo(1);
        assertThat(dashboard.audienceSize()).isEqualTo(15L);
        assertThat(dashboard.recentCampaignMetrics().get(0).audienceSize()).isEqualTo(15);
    }

    @Test
    void executiveDashboardAudienceSizeMatchesTotalAudienceCalculation() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "Exec");
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(40, 10, 30);

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
        when(campaignProductRepository.findAll()).thenReturn(List.of());

        ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

        assertThat(executive.totalAudience()).isEqualTo(50L);
        assertThat(executive.totalEligible()).isEqualTo(40L);
        assertThat(executive.totalExcluded()).isEqualTo(10L);
        assertThat(executive.totalAudience())
                .isEqualTo(executive.totalEligible() + executive.totalExcluded());
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner = User.create("audience-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
