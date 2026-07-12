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
import java.util.Optional;
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
 * KB item 448 acceptance: Excluded count is calculated correctly.
 *
 * <p>Item 419 definition: per-campaign excluded count is the number of {@code campaign_recipients}
 * with status {@code EXCLUDED} (consent, do-not-contact, contact limits, etc.), stored on {@code
 * campaign_metrics.excluded_count} at launch. Dashboard and executive totals sum those
 * per-campaign values. Campaign analytics and related views expose the same validated count.
 *
 * <p>Companion unit coverage also lives in {@link CalculateExcludedCountTests} (item 419).
 *
 * <p>Note: product performance rows aggregate audience/eligible/sent (and engagement) but do not
 * expose a separate excluded field; excluded is still validated via launch metrics and
 * audience = eligible + excluded.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("448 Excluded count is calculated correctly")
class ExcludedCountIsCalculatedCorrectlyTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000448");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000449");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000448");

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
    @DisplayName("Formula: non-negative EXCLUDED recipient total")
    class Formula {

        @Test
        void pureFormulaAcceptsNonNegativeCounts() {
            assertThat(CampaignMetrics.calculateExcludedCount(0)).isZero();
            assertThat(CampaignMetrics.calculateExcludedCount(1)).isEqualTo(1);
            assertThat(CampaignMetrics.calculateExcludedCount(100)).isEqualTo(100);
            assertThat(AnalyticsCalculations.calculateExcludedCount(42)).isEqualTo(42);
        }

        @Test
        void pureFormulaConvertsRecipientRepositoryLongsSafely() {
            assertThat(CampaignMetrics.calculateExcludedCount(0L)).isZero();
            assertThat(CampaignMetrics.calculateExcludedCount(8L)).isEqualTo(8);
            assertThat(AnalyticsCalculations.calculateExcludedCount(15L)).isEqualTo(15);
            assertThat(CampaignMetrics.calculateExcludedCount((long) Integer.MAX_VALUE))
                    .isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        void pureFormulaRejectsNegativeCounts() {
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
        void pureFormulaRejectsLongValuesOutsideIntRange() {
            assertThatThrownBy(
                            () ->
                                    CampaignMetrics.calculateExcludedCount(
                                            (long) Integer.MAX_VALUE + 1L))
                    .isInstanceOf(ArithmeticException.class);
        }

        @Test
        void recordLaunchCountsStoresValidatedExcludedCount() {
            CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));

            metrics.recordLaunchCounts(12, 3, 12);

            assertThat(metrics.getExcludedCount()).isEqualTo(3);
            assertThat(metrics.calculateExcludedCount()).isEqualTo(3);
            assertThat(AnalyticsCalculations.calculateExcludedCount(metrics)).isEqualTo(3);
            assertThat(metrics.getEligibleCount()).isEqualTo(12);
            // Audience formula still consumes excluded correctly (item 446 / 417).
            assertThat(metrics.calculateAudienceSize()).isEqualTo(15);
        }

        @Test
        void recordLaunchCountsRejectsNegativeExcludedInput() {
            CampaignMetrics metrics = CampaignMetrics.forCampaign(sampleCampaign(CAMPAIGN_A, "A"));

            assertThatThrownBy(() -> metrics.recordLaunchCounts(0, -1, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Excluded count must not be negative");
        }
    }

    @Nested
    @DisplayName("Aggregates: dashboard / executive")
    class Aggregates {

        @Test
        void totalExcludedCountSumsPerCampaignExcluded() {
            CampaignMetrics a = launchedMetrics(CAMPAIGN_A, "A", 80, 20, 70);
            CampaignMetrics b = launchedMetrics(CAMPAIGN_B, "B", 15, 5, 10);

            assertThat(AnalyticsCalculations.totalExcludedCount(List.of(a, b))).isEqualTo(25L);
            assertThat(AnalyticsCalculations.totalExcludedCount(List.of(a))).isEqualTo(20L);
            assertThat(AnalyticsCalculations.totalExcludedCount(List.of())).isZero();
            assertThat(AnalyticsCalculations.totalExcludedCount(null)).isZero();
        }

        @Test
        void dashboardExcludedCountIsSumOfCampaignExcludedCounts() {
            Campaign campaignA = activeCampaign(CAMPAIGN_A, "A");
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
        void executiveTotalExcludedMatchesSumOfCampaignExcludedCounts() {
            Campaign campaign = activeCampaign(CAMPAIGN_A, "Exec");
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
        void emptyInventoryYieldsZeroExcludedCount() {
            when(campaignRepository.findAll()).thenReturn(List.of());
            when(campaignMetricsRepository.findAll()).thenReturn(List.of());
            when(campaignProductRepository.findAll()).thenReturn(List.of());

            DashboardView dashboard = analyticsService.getDashboard();
            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            assertThat(dashboard.excludedCount()).isZero();
            assertThat(executive.totalExcluded()).isZero();
        }

        @Test
        void productLinkedCampaignsStillContributeExcludedToAudienceFormula() {
            // Product performance does not expose excludedCount, but audience size remains
            // eligible + excluded from each linked campaign.
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

            when(campaignProductRepository.findAll())
                    .thenReturn(
                            List.of(
                                    CampaignProduct.link(campaignA, product),
                                    CampaignProduct.link(campaignB, product)));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

            List<ProductPerformanceView> rows = analyticsService.getProductPerformance();

            assertThat(rows).hasSize(1);
            ProductPerformanceView row = rows.get(0);
            assertThat(row.eligibleCount()).isEqualTo(50L);
            assertThat(row.audienceSize()).isEqualTo(65L);
            assertThat(row.audienceSize())
                    .isEqualTo(
                            AnalyticsCalculations.calculateEligibleCount(metricsA)
                                    + AnalyticsCalculations.calculateExcludedCount(metricsA)
                                    + AnalyticsCalculations.calculateEligibleCount(metricsB)
                                    + AnalyticsCalculations.calculateExcludedCount(metricsB));
        }
    }

    @Nested
    @DisplayName("Campaign analytics detail")
    class CampaignAnalyticsDetail {

        @Test
        void campaignAnalyticsMetricsExposesExcludedCount() {
            Campaign campaign = activeCampaign(CAMPAIGN_A, "Detail");
            CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
            metrics.recordLaunchCounts(18, 2, 18);

            when(campaignRepository.findById(CAMPAIGN_A)).thenReturn(Optional.of(campaign));
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_A))
                    .thenReturn(Optional.of(metrics));

            CampaignAnalyticsView analytics = analyticsService.getCampaignAnalytics(CAMPAIGN_A);

            assertThat(analytics.metrics()).isNotNull();
            assertThat(analytics.metrics().excludedCount()).isEqualTo(2);
            assertThat(analytics.metrics().eligibleCount()).isEqualTo(18);
            assertThat(analytics.metrics().audienceSize()).isEqualTo(20);
            assertThat(analytics.metrics().audienceSize())
                    .isEqualTo(
                            analytics.metrics().eligibleCount()
                                    + analytics.metrics().excludedCount());
        }

        @Test
        void campaignAnalyticsWithoutMetricsHasNullMetricsPayload() {
            Campaign campaign = sampleCampaign(CAMPAIGN_A, "NoMetrics");

            when(campaignRepository.findById(CAMPAIGN_A)).thenReturn(Optional.of(campaign));
            when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_A))
                    .thenReturn(Optional.empty());

            CampaignAnalyticsView analytics = analyticsService.getCampaignAnalytics(CAMPAIGN_A);

            assertThat(analytics.metrics()).isNull();
        }
    }

    @Nested
    @DisplayName("View mapping consistency")
    class ViewMapping {

        @Test
        void campaignMetricsViewExposesCalculatedExcludedCount() {
            CampaignMetrics metrics = launchedMetrics(CAMPAIGN_A, "View", 11, 4, 11);

            CampaignMetricsView view = CampaignMetricsView.from(metrics);

            assertThat(view.excludedCount()).isEqualTo(4);
            assertThat(view.eligibleCount()).isEqualTo(11);
            assertThat(view.audienceSize()).isEqualTo(15);
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
                User.create("excluded448-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
