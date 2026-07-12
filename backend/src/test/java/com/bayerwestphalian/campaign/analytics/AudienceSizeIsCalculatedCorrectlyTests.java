package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignMetrics;
import com.bayerwestphalian.campaign.campaign.CampaignMetricsRepository;
import com.bayerwestphalian.campaign.campaign.CampaignProduct;
import com.bayerwestphalian.campaign.campaign.CampaignProductRepository;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 446 acceptance: Audience size is calculated correctly.
 *
 * <p>FR-102 / item 417 definition: per-campaign audience size = {@code eligible_count +
 * excluded_count}. Dashboard and executive totals sum those per-campaign values. Product
 * performance and campaign analytics expose the same derived size (never a stale stored column when
 * recalculated).
 *
 * <p>Companion unit coverage also lives in {@link CalculateAudienceSizeTests} (item 417).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("446 Audience size is calculated correctly")
class AudienceSizeIsCalculatedCorrectlyTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000446");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000447");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000446");

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

    @Nested
    @DisplayName("Formula: audience = eligible + excluded")
    class Formula {

        @Test
        void pureFormulaAddsEligibleAndExcludedCounts() {
            assertThat(CampaignMetrics.calculateAudienceSize(0, 0)).isZero();
            assertThat(CampaignMetrics.calculateAudienceSize(1, 0)).isEqualTo(1);
            assertThat(CampaignMetrics.calculateAudienceSize(0, 1)).isEqualTo(1);
            assertThat(CampaignMetrics.calculateAudienceSize(80, 20)).isEqualTo(100);
            assertThat(CampaignMetrics.calculateAudienceSize(7, 3)).isEqualTo(10);

            assertThat(AnalyticsCalculations.calculateAudienceSize(80, 20)).isEqualTo(100);
            assertThat(AnalyticsCalculations.calculateAudienceSize(0, 0)).isZero();
        }

        @Test
        void pureFormulaRejectsNegativeInputs() {
            assertThatThrownBy(() -> CampaignMetrics.calculateAudienceSize(-1, 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Eligible count must not be negative");
            assertThatThrownBy(() -> CampaignMetrics.calculateAudienceSize(5, -2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Excluded count must not be negative");
        }

        @Test
        void entityAudienceSizeMatchesEligiblePlusExcludedAfterLaunch() {
            CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));

            metrics.recordLaunchCounts(12, 3, 12);

            assertThat(metrics.getEligibleCount()).isEqualTo(12);
            assertThat(metrics.getExcludedCount()).isEqualTo(3);
            assertThat(metrics.calculateAudienceSize()).isEqualTo(15);
            assertThat(metrics.getAudienceSize()).isEqualTo(15);
            assertThat(metrics.getAudienceSize())
                    .isEqualTo(metrics.getEligibleCount() + metrics.getExcludedCount());
        }

        @Test
        void recalculateRepairsStaleStoredAudienceSizeColumn() {
            CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));
            metrics.recordLaunchCounts(9, 1, 9);
            ReflectionTestUtils.setField(metrics, "audienceSize", 999);

            assertThat(metrics.getAudienceSize()).isEqualTo(999);
            assertThat(metrics.calculateAudienceSize()).isEqualTo(10);
            assertThat(AnalyticsCalculations.calculateAudienceSize(metrics)).isEqualTo(10);

            metrics.recalculate();
            assertThat(metrics.getAudienceSize()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Aggregates: dashboard / executive / product")
    class Aggregates {

        @Test
        void totalAudienceSizeSumsPerCampaignDerivedAudience() {
            CampaignMetrics a = launchedMetrics(CAMPAIGN_A, "A", 80, 20, 70);
            CampaignMetrics b = launchedMetrics(CAMPAIGN_B, "B", 15, 5, 10);

            assertThat(AnalyticsCalculations.totalAudienceSize(List.of(a, b))).isEqualTo(120L);
            assertThat(AnalyticsCalculations.totalAudienceSize(List.of(a))).isEqualTo(100L);
            assertThat(AnalyticsCalculations.totalAudienceSize(List.of())).isZero();
            assertThat(AnalyticsCalculations.totalAudienceSize(null)).isZero();
        }

        @Test
        void dashboardAudienceSizeEqualsSumOfEligibleAndExcludedTotals() {
            Campaign campaignA = activeCampaign(CAMPAIGN_A, "A");
            Campaign campaignB = sampleCampaign(CAMPAIGN_B, "B");
            CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
            metricsA.recordLaunchCounts(80, 20, 70);
            CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
            metricsB.recordLaunchCounts(15, 5, 10);

            when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

            DashboardView dashboard = analyticsService.getDashboard();

            // FR-102: dashboard shows audience size.
            assertThat(dashboard.audienceSize()).isEqualTo(120L);
            assertThat(dashboard.eligibleCount()).isEqualTo(95L);
            assertThat(dashboard.excludedCount()).isEqualTo(25L);
            assertThat(dashboard.audienceSize())
                    .isEqualTo(dashboard.eligibleCount() + dashboard.excludedCount());
            assertThat(dashboard.recentCampaignMetrics())
                    .extracting(CampaignMetricsView::audienceSize)
                    .containsExactlyInAnyOrder(100, 20);
        }

        @Test
        void dashboardUsesDerivedAudienceWhenStoredColumnIsStale() {
            Campaign campaign = sampleCampaign(CAMPAIGN_A, "Stale");
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            metrics.recordLaunchCounts(25, 5, 25);
            ReflectionTestUtils.setField(metrics, "audienceSize", 2);

            when(campaignRepository.findAll()).thenReturn(List.of(campaign));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));

            DashboardView dashboard = analyticsService.getDashboard();

            assertThat(metrics.getAudienceSize()).isEqualTo(2);
            assertThat(dashboard.audienceSize()).isEqualTo(30L);
            assertThat(dashboard.recentCampaignMetrics()).hasSize(1);
            assertThat(dashboard.recentCampaignMetrics().get(0).audienceSize()).isEqualTo(30);
        }

        @Test
        void executiveTotalAudienceMatchesEligiblePlusExcludedAggregates() {
            Campaign campaign = activeCampaign(CAMPAIGN_A, "Exec");
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

        @Test
        void emptyInventoryYieldsZeroAudienceSize() {
            when(campaignRepository.findAll()).thenReturn(List.of());
            when(campaignMetricsRepository.findAll()).thenReturn(List.of());
            when(campaignProductRepository.findAll()).thenReturn(List.of());

            DashboardView dashboard = analyticsService.getDashboard();
            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            assertThat(dashboard.audienceSize()).isZero();
            assertThat(dashboard.eligibleCount()).isZero();
            assertThat(dashboard.excludedCount()).isZero();
            assertThat(executive.totalAudience()).isZero();
            assertThat(executive.totalEligible()).isZero();
            assertThat(executive.totalExcluded()).isZero();
        }

        @Test
        void productPerformanceAudienceSizeSumsLinkedCampaignAudiences() {
            Campaign campaignA = sampleCampaign(CAMPAIGN_A, "ProdA");
            Campaign campaignB = sampleCampaign(CAMPAIGN_B, "ProdB");
            CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
            metricsA.recordLaunchCounts(30, 10, 30);
            CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
            metricsB.recordLaunchCounts(20, 5, 20);

            Product product =
                    Product.create(
                            "Life Protection", ProductType.LIFE_INSURANCE, BigDecimal.TEN, 12);
            ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

            CampaignProduct linkA = CampaignProduct.link(campaignA, product);
            CampaignProduct linkB = CampaignProduct.link(campaignB, product);

            when(campaignProductRepository.findAll()).thenReturn(List.of(linkA, linkB));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

            List<ProductPerformanceView> rows = analyticsService.getProductPerformance();

            assertThat(rows).hasSize(1);
            ProductPerformanceView row = rows.get(0);
            assertThat(row.audienceSize()).isEqualTo(65L);
            assertThat(row.eligibleCount()).isEqualTo(50L);
            // Product rows sum eligible and audience from derived formulas (eligible+excluded).
            assertThat(row.audienceSize())
                    .isEqualTo(
                            AnalyticsCalculations.calculateAudienceSize(metricsA)
                                    + AnalyticsCalculations.calculateAudienceSize(metricsB));
        }
    }

    @Nested
    @DisplayName("Campaign analytics detail")
    class CampaignAnalyticsDetail {

        @Test
        void campaignAnalyticsMetricsAudienceSizeIsEligiblePlusExcluded() {
            Campaign campaign = activeCampaign(CAMPAIGN_A, "Detail");
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            metrics.recordLaunchCounts(18, 2, 18);
            ReflectionTestUtils.setField(metrics, "audienceSize", 1);

            when(campaignRepository.findById(CAMPAIGN_A)).thenReturn(java.util.Optional.of(campaign));
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_A))
                    .thenReturn(java.util.Optional.of(metrics));

            CampaignAnalyticsView analytics = analyticsService.getCampaignAnalytics(CAMPAIGN_A);

            assertThat(analytics.metrics()).isNotNull();
            assertThat(analytics.metrics().audienceSize()).isEqualTo(20);
            assertThat(analytics.metrics().eligibleCount()).isEqualTo(18);
            assertThat(analytics.metrics().excludedCount()).isEqualTo(2);
            assertThat(analytics.metrics().audienceSize())
                    .isEqualTo(
                            analytics.metrics().eligibleCount()
                                    + analytics.metrics().excludedCount());
        }

        @Test
        void campaignAnalyticsWithoutMetricsHasNullMetricsPayload() {
            Campaign campaign = sampleCampaign(CAMPAIGN_A, "NoMetrics");

            when(campaignRepository.findById(CAMPAIGN_A)).thenReturn(java.util.Optional.of(campaign));
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_A))
                    .thenReturn(java.util.Optional.empty());

            CampaignAnalyticsView analytics = analyticsService.getCampaignAnalytics(CAMPAIGN_A);

            assertThat(analytics.metrics()).isNull();
        }
    }

    @Nested
    @DisplayName("View mapping consistency")
    class ViewMapping {

        @Test
        void campaignMetricsViewAudienceSizeUsesDerivedCalculation() {
            CampaignMetrics metrics =
                    launchedMetrics(CAMPAIGN_A, "View", 11, 4, 11);
            ReflectionTestUtils.setField(metrics, "audienceSize", 0);

            CampaignMetricsView view = CampaignMetricsView.from(metrics);

            assertThat(view.audienceSize()).isEqualTo(15);
            assertThat(view.eligibleCount()).isEqualTo(11);
            assertThat(view.excludedCount()).isEqualTo(4);
            assertThat(view.audienceSize())
                    .isEqualTo(view.eligibleCount() + view.excludedCount());
        }
    }

    private static CampaignMetrics launchedMetrics(
            UUID campaignId, String name, int eligible, int excluded, int sent) {
        CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(campaignId, name));
        metrics.recordLaunchCounts(eligible, excluded, sent);
        return metrics;
    }

    private static Campaign activeCampaign(UUID id, String name) {
        Campaign campaign = sampleCampaign(id, name);
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        return campaign;
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner =
                User.create("audience446-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
