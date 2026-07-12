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
 * KB item 426 / FR-105: Calculate click rate.
 *
 * <p>Click rate = clicked_count / sent_count when sent &gt; 0; otherwise {@link BigDecimal#ZERO} at
 * scale 4. Dashboard and executive views compute the rate from aggregate clicked and sent totals.
 *
 * <p>Acceptance coverage for the same rule is also formalized under KB item 452 in {@link
 * ClickRateIsCalculatedCorrectlyTests}.
 */
@ExtendWith(MockitoExtension.class)
class CalculateClickRateTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000426");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000427");

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
    void clickRateIsClickedDividedBySent() {
        assertThat(CampaignMetrics.calculateClickRate(2, 10)).isEqualByComparingTo("0.2000");
        assertThat(CampaignMetrics.calculateClickRate(1, 4)).isEqualByComparingTo("0.2500");
        assertThat(CampaignMetrics.calculateClickRate(0, 10)).isEqualByComparingTo("0.0000");
        assertThat(CampaignMetrics.calculateClickRate(3, 3)).isEqualByComparingTo("1.0000");

        assertThat(AnalyticsCalculations.calculateClickRate(16L, 80L)).isEqualByComparingTo("0.2000");
        assertThat(AnalyticsRates.clickRate(16L, 80L)).isEqualByComparingTo("0.2000");
    }

    @Test
    void clickRateIsZeroWhenNothingSent() {
        assertThat(CampaignMetrics.calculateClickRate(0, 0)).isEqualByComparingTo(ZERO_RATE);
        assertThat(CampaignMetrics.calculateClickRate(5, 0)).isEqualByComparingTo(ZERO_RATE);
        assertThat(CampaignMetrics.calculateClickRate(5L, 0L)).isEqualByComparingTo(ZERO_RATE);
        assertThat(AnalyticsCalculations.calculateClickRate(1L, 0L)).isEqualByComparingTo(ZERO_RATE);
        assertThat(AnalyticsRates.clickRate(1L, 0L)).isEqualByComparingTo(ZERO_RATE);
    }

    @Test
    void clickRateRejectsNegativeInputs() {
        assertThatThrownBy(() -> CampaignMetrics.calculateClickRate(-1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Clicked count must not be negative");
        assertThatThrownBy(() -> CampaignMetrics.calculateClickRate(1, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sent count must not be negative");
        assertThatThrownBy(() -> CampaignMetrics.calculateClickRate(-1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Clicked count must not be negative");
        assertThatThrownBy(() -> AnalyticsCalculations.calculateClickRate(-5L, 10L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clickRateUsesScaleFourHalfUp() {
        // 1/3 = 0.3333..., HALF_UP at scale 4 → 0.3333
        assertThat(CampaignMetrics.calculateClickRate(1, 3)).isEqualByComparingTo("0.3333");
        // 2/3 = 0.6666..., HALF_UP at scale 4 → 0.6667
        assertThat(CampaignMetrics.calculateClickRate(2, 3)).isEqualByComparingTo("0.6667");
    }

    @Test
    void metricsRowClickRateUsesClickedAndSentCounts() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        metrics.recordLaunchCounts(10, 0, 10);
        metrics.recordEngagementCounts(0, 2, 0, 0);

        assertThat(metrics.calculateClickRate()).isEqualByComparingTo("0.2000");
        assertThat(AnalyticsCalculations.calculateClickRate(metrics)).isEqualByComparingTo("0.2000");
        assertThat(CampaignMetrics.calculateClickRate(
                        metrics.calculateClickedCount(), metrics.calculateSentCount()))
                .isEqualByComparingTo("0.2000");
    }

    @Test
    void dashboardClickRateUsesAggregateClickedOverSent() {
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

        // FR-105: dashboard shows click rate = total clicked / total sent = 16 / 80.
        assertThat(dashboard.clickedCount()).isEqualTo(16L);
        assertThat(dashboard.messagesSent()).isEqualTo(80L);
        assertThat(dashboard.clickRate()).isEqualByComparingTo("0.2000");
        assertThat(AnalyticsCalculations.calculateClickRate(
                        dashboard.clickedCount(), dashboard.messagesSent()))
                .isEqualByComparingTo(dashboard.clickRate());
    }

    @Test
    void executiveClickRateUsesAggregateClickedOverSent() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "Exec");
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(40, 10, 30);
        metrics.recordEngagementCounts(12, 6, 4, 3);

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
        when(campaignProductRepository.findAll()).thenReturn(List.of());

        ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

        // 6 / 30 = 0.2
        assertThat(executive.totalClicked()).isEqualTo(6L);
        assertThat(executive.totalSent()).isEqualTo(30L);
        assertThat(executive.overallClickRate()).isEqualByComparingTo("0.2000");
    }

    @Test
    void campaignMetricsViewExposesCalculatedClickRate() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "View");
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(12, 3, 12);
        metrics.recordEngagementCounts(6, 3, 2, 1);
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        CampaignMetricsView view = CampaignMetricsView.from(metrics);

        assertThat(view.clickedCount()).isEqualTo(3);
        assertThat(view.sentCount()).isEqualTo(12);
        assertThat(view.clickRate()).isEqualByComparingTo("0.2500");
        assertThat(view.openRate()).isEqualByComparingTo("0.5000");
    }

    @Test
    void clickRateIsZeroOnEmptyDashboard() {
        when(campaignRepository.findAll()).thenReturn(List.of());
        when(campaignMetricsRepository.findAll()).thenReturn(List.of());

        DashboardView dashboard = analyticsService.getDashboard();

        assertThat(dashboard.clickedCount()).isZero();
        assertThat(dashboard.messagesSent()).isZero();
        assertThat(dashboard.clickRate()).isEqualByComparingTo(ZERO_RATE);
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner = User.create("clickrate-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
