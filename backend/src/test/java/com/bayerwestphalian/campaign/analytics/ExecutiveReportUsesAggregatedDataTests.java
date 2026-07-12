package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 457 acceptance: Executive report uses aggregated data.
 *
 * <p>Item 434 / COMP-010 definition: executive management reports prefer platform-level aggregates
 * from {@link CampaignMetrics} rather than raw contact-event or recipient detail rows. {@link
 * ExecutiveDashboardView} exposes inventory counts, summed funnel/engagement totals, rates from
 * aggregate numerators ÷ aggregate sent, ROI from summed cost/revenue, and embedded product
 * performance aggregates (item 433).
 *
 * <p>Companion coverage also lives in {@link ExecutiveDashboardEndpointTests} and {@link
 * AnalyticsServiceTests} (item 434).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("457 Executive report uses aggregated data")
class ExecutiveReportUsesAggregatedDataTests {

    private static final UUID CAMPAIGN_A = UUID.fromString("50000000-0000-0000-0000-000000000457");
    private static final UUID CAMPAIGN_B = UUID.fromString("50000000-0000-0000-0000-000000000458");
    private static final UUID CAMPAIGN_C = UUID.fromString("50000000-0000-0000-0000-000000000459");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000457");

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
    @DisplayName("COMP-010: payload is aggregates, not raw event rows")
    class AggregatePayloadShape {

        @Test
        void executiveViewExposesOnlyAggregateFields() {
            Set<String> components =
                    Arrays.stream(ExecutiveDashboardView.class.getRecordComponents())
                            .map(RecordComponent::getName)
                            .collect(Collectors.toSet());

            // Platform aggregates required for management reporting.
            assertThat(components)
                    .contains(
                            "totalCampaigns",
                            "activeCampaigns",
                            "completedCampaigns",
                            "totalAudience",
                            "totalEligible",
                            "totalExcluded",
                            "totalSent",
                            "totalOpened",
                            "totalClicked",
                            "totalReplied",
                            "totalConverted",
                            "overallOpenRate",
                            "overallClickRate",
                            "overallConversionRate",
                            "totalEstimatedCost",
                            "totalEstimatedRevenue",
                            "overallEstimatedRoi",
                            "productPerformance");

            // COMP-010: no per-contact-event or recipient detail collections.
            assertThat(components)
                    .doesNotContain(
                            "contactEvents",
                            "recipients",
                            "recentCampaignMetrics",
                            "events",
                            "customers");
        }

        @Test
        void emptyInventoryYieldsZeroedAggregatesWithoutDetailRows() {
            when(campaignRepository.findAll()).thenReturn(List.of());
            when(campaignMetricsRepository.findAll()).thenReturn(List.of());
            when(campaignProductRepository.findAll()).thenReturn(List.of());

            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            assertThat(executive.totalCampaigns()).isZero();
            assertThat(executive.activeCampaigns()).isZero();
            assertThat(executive.completedCampaigns()).isZero();
            assertThat(executive.totalAudience()).isZero();
            assertThat(executive.totalEligible()).isZero();
            assertThat(executive.totalExcluded()).isZero();
            assertThat(executive.totalSent()).isZero();
            assertThat(executive.totalOpened()).isZero();
            assertThat(executive.totalClicked()).isZero();
            assertThat(executive.totalReplied()).isZero();
            assertThat(executive.totalConverted()).isZero();
            assertThat(executive.overallOpenRate()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(executive.overallClickRate()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(executive.overallConversionRate()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(executive.totalEstimatedCost()).isNull();
            assertThat(executive.totalEstimatedRevenue()).isNull();
            assertThat(executive.overallEstimatedRoi()).isNull();
            assertThat(executive.productPerformance()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Funnel and engagement: sums of campaign metrics")
    class FunnelAndEngagementTotals {

        @Test
        void inventoryAndFunnelCountsSumAcrossCampaigns() {
            Campaign active = activeCampaign(CAMPAIGN_A, "Active");
            Campaign completed = completedCampaign(CAMPAIGN_B, "Done");
            Campaign draft = sampleCampaign(CAMPAIGN_C, "Draft");

            CampaignMetrics metricsA = CampaignMetrics.forCampaign(active);
            metricsA.recordLaunchCounts(80, 20, 70);
            metricsA.recordEngagementCounts(35, 14, 7, 4);
            CampaignMetrics metricsB = CampaignMetrics.forCampaign(completed);
            metricsB.recordLaunchCounts(15, 5, 10);
            metricsB.recordEngagementCounts(5, 2, 1, 1);

            when(campaignRepository.findAll()).thenReturn(List.of(active, completed, draft));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));
            when(campaignProductRepository.findAll()).thenReturn(List.of());

            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            // Inventory from campaign rows (not contact events).
            assertThat(executive.totalCampaigns()).isEqualTo(3L);
            assertThat(executive.activeCampaigns()).isEqualTo(1L);
            assertThat(executive.completedCampaigns()).isEqualTo(1L);

            // Funnel: sums of per-campaign metrics (COMP-010 aggregates).
            assertThat(executive.totalAudience())
                    .isEqualTo(AnalyticsCalculations.totalAudienceSize(List.of(metricsA, metricsB)));
            assertThat(executive.totalEligible()).isEqualTo(95L); // 80 + 15
            assertThat(executive.totalExcluded()).isEqualTo(25L); // 20 + 5
            assertThat(executive.totalSent()).isEqualTo(80L); // 70 + 10
            assertThat(executive.totalOpened()).isEqualTo(40L); // 35 + 5
            assertThat(executive.totalClicked()).isEqualTo(16L); // 14 + 2
            assertThat(executive.totalReplied()).isEqualTo(8L); // 7 + 1
            assertThat(executive.totalConverted()).isEqualTo(5L); // 4 + 1

            // Totals match AnalyticsCalculations helpers (single source of aggregate truth).
            List<CampaignMetrics> all = List.of(metricsA, metricsB);
            assertThat(executive.totalEligible())
                    .isEqualTo(AnalyticsCalculations.totalEligibleCount(all));
            assertThat(executive.totalExcluded())
                    .isEqualTo(AnalyticsCalculations.totalExcludedCount(all));
            assertThat(executive.totalSent()).isEqualTo(AnalyticsCalculations.totalSentCount(all));
            assertThat(executive.totalOpened())
                    .isEqualTo(AnalyticsCalculations.totalOpenedCount(all));
            assertThat(executive.totalClicked())
                    .isEqualTo(AnalyticsCalculations.totalClickedCount(all));
            assertThat(executive.totalConverted())
                    .isEqualTo(AnalyticsCalculations.totalConvertedCount(all));
        }
    }

    @Nested
    @DisplayName("Rates and ROI from aggregates (not averages of campaign rates)")
    class RatesAndRoiFromAggregates {

        @Test
        void overallRatesUseAggregateNumeratorsOverAggregateSent() {
            Campaign campaignA = activeCampaign(CAMPAIGN_A, "A");
            Campaign campaignB = sampleCampaign(CAMPAIGN_B, "B");

            // Unequal sizes so average-of-rates ≠ aggregate rate.
            // A: open 1/10=0.1, click 1/10=0.1, conv 1/10=0.1
            // B: open 1/2=0.5, click 1/2=0.5, conv 1/2=0.5
            // Average of rates = 0.3 ; aggregate = 2/12 ≈ 0.1667
            CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
            metricsA.recordLaunchCounts(10, 0, 10);
            metricsA.recordEngagementCounts(1, 1, 0, 1);
            CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
            metricsB.recordLaunchCounts(2, 0, 2);
            metricsB.recordEngagementCounts(1, 1, 0, 1);

            when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));
            when(campaignProductRepository.findAll()).thenReturn(List.of());

            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            assertThat(executive.totalOpened()).isEqualTo(2L);
            assertThat(executive.totalClicked()).isEqualTo(2L);
            assertThat(executive.totalConverted()).isEqualTo(2L);
            assertThat(executive.totalSent()).isEqualTo(12L);

            assertThat(executive.overallOpenRate()).isEqualByComparingTo("0.1667");
            assertThat(executive.overallClickRate()).isEqualByComparingTo("0.1667");
            assertThat(executive.overallConversionRate()).isEqualByComparingTo("0.1667");

            assertThat(
                            AnalyticsCalculations.calculateOpenRate(
                                    executive.totalOpened(), executive.totalSent()))
                    .isEqualByComparingTo(executive.overallOpenRate());
            assertThat(
                            AnalyticsCalculations.calculateClickRate(
                                    executive.totalClicked(), executive.totalSent()))
                    .isEqualByComparingTo(executive.overallClickRate());
            assertThat(
                            AnalyticsCalculations.calculateConversionRate(
                                    executive.totalConverted(), executive.totalSent()))
                    .isEqualByComparingTo(executive.overallConversionRate());

            BigDecimal averageOpenRate =
                    metricsA.calculateOpenRate()
                            .add(metricsB.calculateOpenRate())
                            .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
            assertThat(averageOpenRate).isEqualByComparingTo("0.3000");
            assertThat(executive.overallOpenRate()).isNotEqualByComparingTo(averageOpenRate);
        }

        @Test
        void overallRoiUsesAggregateCostAndRevenueNotAverageOfCampaignRois() {
            Campaign campaignA = activeCampaign(CAMPAIGN_A, "A");
            Campaign campaignB = sampleCampaign(CAMPAIGN_B, "B");

            // A: cost 100, rev 150 → ROI 0.50 ; B: cost 25, rev 50 → ROI 1.00
            // Average of ROIs = 0.75 ; aggregate cost 125, rev 200 → ROI 0.60
            CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
            metricsA.recordLaunchCounts(80, 20, 70);
            metricsA.updateFinancialEstimates(new BigDecimal("100.00"), new BigDecimal("150.00"));
            CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
            metricsB.recordLaunchCounts(15, 5, 10);
            metricsB.updateFinancialEstimates(new BigDecimal("25.00"), new BigDecimal("50.00"));

            when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));
            when(campaignProductRepository.findAll()).thenReturn(List.of());

            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            assertThat(executive.totalEstimatedCost()).isEqualByComparingTo("125.00");
            assertThat(executive.totalEstimatedRevenue()).isEqualByComparingTo("200.00");
            assertThat(executive.overallEstimatedRoi()).isEqualByComparingTo("0.60");
            assertThat(
                            AnalyticsCalculations.totalEstimatedRoi(
                                    List.of(metricsA, metricsB)))
                    .isEqualByComparingTo(executive.overallEstimatedRoi());

            BigDecimal averageOfRois =
                    metricsA.calculateEstimatedRoi()
                            .add(metricsB.calculateEstimatedRoi())
                            .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            assertThat(averageOfRois).isEqualByComparingTo("0.75");
            assertThat(executive.overallEstimatedRoi()).isNotEqualByComparingTo(averageOfRois);
        }
    }

    @Nested
    @DisplayName("Embedded product performance is product-level aggregates")
    class ProductPerformanceAggregates {

        @Test
        void productPerformanceRowsAggregateLinkedCampaignMetrics() {
            Campaign campaignA = activeCampaign(CAMPAIGN_A, "ProdA");
            Campaign campaignB = sampleCampaign(CAMPAIGN_B, "ProdB");
            CampaignMetrics metricsA = CampaignMetrics.forCampaign(campaignA);
            metricsA.recordLaunchCounts(20, 0, 20);
            metricsA.recordEngagementCounts(10, 4, 2, 2);
            metricsA.updateFinancialEstimates(new BigDecimal("100.00"), new BigDecimal("150.00"));
            CampaignMetrics metricsB = CampaignMetrics.forCampaign(campaignB);
            metricsB.recordLaunchCounts(10, 0, 10);
            metricsB.recordEngagementCounts(2, 2, 1, 1);
            metricsB.updateFinancialEstimates(new BigDecimal("25.00"), new BigDecimal("50.00"));

            Product product =
                    Product.create(
                            "Life Protection", ProductType.LIFE_INSURANCE, BigDecimal.TEN, 12);
            ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

            when(campaignRepository.findAll()).thenReturn(List.of(campaignA, campaignB));
            when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));
            when(campaignProductRepository.findAll())
                    .thenReturn(
                            List.of(
                                    CampaignProduct.link(campaignA, product),
                                    CampaignProduct.link(campaignB, product)));

            ExecutiveDashboardView executive = analyticsService.getExecutiveDashboard();

            assertThat(executive.productPerformance()).hasSize(1);
            ProductPerformanceView row = executive.productPerformance().get(0);
            // Product-level aggregates (same COMP-010 style as platform totals).
            assertThat(row.campaignCount()).isEqualTo(2L);
            assertThat(row.sentCount()).isEqualTo(30L);
            assertThat(row.openedCount()).isEqualTo(12L);
            assertThat(row.clickedCount()).isEqualTo(6L);
            assertThat(row.convertedCount()).isEqualTo(3L);
            assertThat(row.openRate()).isEqualByComparingTo("0.4000"); // 12/30
            assertThat(row.clickRate()).isEqualByComparingTo("0.2000"); // 6/30
            assertThat(row.conversionRate()).isEqualByComparingTo("0.1000"); // 3/30
            assertThat(row.estimatedCost()).isEqualByComparingTo("125.00");
            assertThat(row.estimatedRevenue()).isEqualByComparingTo("200.00");
            assertThat(row.estimatedRoi()).isEqualByComparingTo("0.60");
        }
    }

    private static Campaign activeCampaign(UUID id, String name) {
        Campaign campaign = sampleCampaign(id, name);
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        return campaign;
    }

    private static Campaign completedCampaign(UUID id, String name) {
        Campaign campaign = sampleCampaign(id, name);
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.COMPLETED);
        return campaign;
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner =
                User.create("exec457-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }
}
