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
 * KB item 424: Calculate converted count.
 *
 * <p>Acceptance that contact events update engagement counters is formalized under KB item 450.
 *
 * <p>Converted count is the number of conversion outcomes (BR-034), stored on {@code
 * campaign_metrics.converted_count} and summed for dashboard {@code convertedCount} / executive
 * {@code totalConverted}. Conversion rate uses converted as numerator and sent as denominator.
 */
@ExtendWith(MockitoExtension.class)
class CalculateConvertedCountTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000424");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000425");

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
    void convertedCountIsNonNegativeConversionTotal() {
        assertThat(CampaignMetrics.calculateConvertedCount(0)).isZero();
        assertThat(CampaignMetrics.calculateConvertedCount(1)).isEqualTo(1);
        assertThat(CampaignMetrics.calculateConvertedCount(100)).isEqualTo(100);

        assertThat(AnalyticsCalculations.calculateConvertedCount(42)).isEqualTo(42);
    }

    @Test
    void convertedCountFromLongEventTallyIsConvertedSafely() {
        assertThat(CampaignMetrics.calculateConvertedCount(0L)).isZero();
        assertThat(CampaignMetrics.calculateConvertedCount(8L)).isEqualTo(8);
        assertThat(AnalyticsCalculations.calculateConvertedCount(15L)).isEqualTo(15);
    }

    @Test
    void convertedCountRejectsNegativeValues() {
        assertThatThrownBy(() -> CampaignMetrics.calculateConvertedCount(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Converted count must not be negative");
        assertThatThrownBy(() -> CampaignMetrics.calculateConvertedCount(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Converted count must not be negative");
        assertThatThrownBy(() -> AnalyticsCalculations.calculateConvertedCount(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertedCountRejectsLongValuesOutsideIntRange() {
        assertThatThrownBy(
                        () ->
                                CampaignMetrics.calculateConvertedCount(
                                        (long) Integer.MAX_VALUE + 1L))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void recordEngagementCountsStoresConvertedCount() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        metrics.recordLaunchCounts(10, 0, 10);

        metrics.recordEngagementCounts(5, 3, 2, 1);

        assertThat(metrics.getConvertedCount()).isEqualTo(1);
        assertThat(metrics.calculateConvertedCount()).isEqualTo(1);
        assertThat(AnalyticsCalculations.calculateConvertedCount(metrics)).isEqualTo(1);
        // Conversion rate uses converted / sent.
        assertThat(metrics.calculateConversionRate()).isEqualByComparingTo("0.1000");
    }

    @Test
    void incrementConvertedUsesCalculateConvertedCount() {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        metrics.recordLaunchCounts(0, 0, 0);

        metrics.incrementConverted();
        metrics.incrementConverted();

        assertThat(metrics.calculateConvertedCount()).isEqualTo(2);
        assertThat(metrics.getConvertedCount()).isEqualTo(2);
    }

    @Test
    void totalConvertedCountSumsPerCampaignConverted() {
        CampaignMetrics a = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
        a.recordLaunchCounts(80, 20, 70);
        a.recordEngagementCounts(35, 14, 7, 4);
        CampaignMetrics b = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_B, "B"));
        b.recordLaunchCounts(15, 5, 10);
        b.recordEngagementCounts(5, 2, 1, 1);

        assertThat(AnalyticsCalculations.totalConvertedCount(List.of(a, b))).isEqualTo(5L);
        assertThat(AnalyticsCalculations.totalConvertedCount(List.of())).isZero();
        assertThat(AnalyticsCalculations.totalConvertedCount(null)).isZero();
    }

    @Test
    void dashboardConvertedCountIsSumOfCampaignConvertedCounts() {
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

        assertThat(dashboard.convertedCount()).isEqualTo(5L);
        assertThat(dashboard.repliedCount()).isEqualTo(8L);
        assertThat(dashboard.clickedCount()).isEqualTo(16L);
        assertThat(dashboard.openedCount()).isEqualTo(40L);
        assertThat(dashboard.recentCampaignMetrics())
                .extracting(CampaignMetricsView::convertedCount)
                .containsExactlyInAnyOrder(4, 1);
    }

    @Test
    void executiveDashboardTotalConvertedMatchesTotalConvertedCalculation() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "Exec");
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(40, 10, 30);
        metrics.recordEngagementCounts(12, 6, 4, 3);

        when(campaignRepository.findAll()).thenReturn(List.of(campaign));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
        when(campaignProductRepository.findAll()).thenReturn(List.of());

        ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

        assertThat(executive.totalConverted()).isEqualTo(3L);
        assertThat(executive.totalReplied()).isEqualTo(4L);
        assertThat(executive.totalClicked()).isEqualTo(6L);
        assertThat(executive.totalOpened()).isEqualTo(12L);
        assertThat(executive.totalSent()).isEqualTo(30L);
    }

    @Test
    void campaignMetricsViewExposesCalculatedConvertedCount() {
        Campaign campaign = sampleCampaign(CAMPAIGN_A, "View");
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(12, 3, 12);
        metrics.recordEngagementCounts(6, 3, 2, 1);
        ReflectionTestUtils.invokeMethod(metrics, "onCreate");

        CampaignMetricsView view = CampaignMetricsView.from(metrics);

        assertThat(view.convertedCount()).isEqualTo(1);
        assertThat(view.repliedCount()).isEqualTo(2);
        assertThat(view.clickedCount()).isEqualTo(3);
        assertThat(view.openedCount()).isEqualTo(6);
        assertThat(view.sentCount()).isEqualTo(12);
        assertThat(view.conversionRate()).isEqualByComparingTo("0.0833");
    }

    @Test
    void convertedCountFromContactEventTallyFeedsConversionRateNumerator() {
        // Domain: conversion outcomes drive converted_count; conversion rate = converted / sent.
        int sent = CampaignMetrics.calculateSentCount(10L);
        int converted = CampaignMetrics.calculateConvertedCount(1L);

        assertThat(converted).isLessThanOrEqualTo(sent);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "Rate"));
        metrics.recordLaunchCounts(10, 0, sent);
        metrics.recordEngagementCounts(0, 0, 0, converted);
        assertThat(metrics.calculateConversionRate()).isEqualByComparingTo("0.1000");
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner =
                User.create("converted-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
