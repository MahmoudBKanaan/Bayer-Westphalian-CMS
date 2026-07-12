package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignMetrics;
import com.bayerwestphalian.campaign.campaign.CampaignMetricsRepository;
import com.bayerwestphalian.campaign.campaign.CampaignProduct;
import com.bayerwestphalian.campaign.campaign.CampaignProductRepository;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.User;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 415: AnalyticsService aggregates campaign metrics for dashboard, campaign detail, product
 * performance, and executive views (E19 / FR-100–FR-107).
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000415");
    private static final UUID CAMPAIGN_B_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000416");
    private static final UUID PRODUCT_ID = UUID.fromString("40000000-0000-0000-0000-000000000415");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000415");

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
    void analyticsReadMethodsDeclareMethodLevelAuthorization() throws Exception {
        for (String methodName :
                List.of(
                        "getDashboard",
                        "getProductPerformance",
                        "getExecutiveDashboard")) {
            Method method = AnalyticsService.class.getMethod(methodName);
            assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
            assertThat(method.getAnnotation(PreAuthorize.class).value())
                    .contains("BI_ANALYST")
                    .contains("CAMPAIGN_MANAGER")
                    .contains("EXECUTIVE_VIEWER");
        }

        Method campaignAnalytics =
                AnalyticsService.class.getMethod("getCampaignAnalytics", UUID.class);
        assertThat(campaignAnalytics.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(campaignAnalytics.getAnnotation(PreAuthorize.class).value())
                .contains("BI_ANALYST");
    }

    @Test
    void getDashboardAggregatesCampaignTotalsAndRates() {
        // KB item 431 / FR-100–FR-107: service-side dashboard aggregation for the endpoint.
        Campaign active = sampleCampaign(CAMPAIGN_ID, "Active campaign");
        ReflectionTestUtils.setField(active, "status", CampaignStatus.ACTIVE);
        Campaign draft = sampleCampaign(CAMPAIGN_B_ID, "Draft campaign");

        CampaignMetrics metricsA = CampaignMetrics.forCampaign(active);
        metricsA.recordLaunchCounts(80, 20, 70);
        metricsA.recordEngagementCounts(35, 14, 7, 4);
        metricsA.updateFinancialEstimates(new BigDecimal("100.00"), new BigDecimal("150.00"));
        ReflectionTestUtils.setField(metricsA, "updatedAt", Instant.parse("2026-07-11T12:00:00Z"));

        CampaignMetrics metricsB = CampaignMetrics.forCampaign(draft);
        metricsB.recordLaunchCounts(20, 5, 10);
        metricsB.recordEngagementCounts(5, 2, 1, 1);
        metricsB.updateFinancialEstimates(new BigDecimal("50.00"), new BigDecimal("50.00"));
        ReflectionTestUtils.setField(metricsB, "updatedAt", Instant.parse("2026-07-10T12:00:00Z"));

        when(campaignRepository.findAll()).thenReturn(List.of(active, draft));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metricsA, metricsB));

        DashboardView dashboard = analyticsService.getDashboard();

        assertThat(dashboard.campaignTotal()).isEqualTo(2L); // FR-100
        assertThat(dashboard.activeCampaigns()).isEqualTo(1L); // FR-101
        assertThat(dashboard.audienceSize()).isEqualTo(125L); // FR-102
        assertThat(dashboard.eligibleCount()).isEqualTo(100L);
        assertThat(dashboard.excludedCount()).isEqualTo(25L);
        assertThat(dashboard.messagesSent()).isEqualTo(80L); // FR-103
        assertThat(dashboard.openedCount()).isEqualTo(40L);
        assertThat(dashboard.clickedCount()).isEqualTo(16L);
        assertThat(dashboard.convertedCount()).isEqualTo(5L);
        assertThat(dashboard.openRate()).isEqualByComparingTo("0.5000"); // FR-104
        assertThat(dashboard.clickRate()).isEqualByComparingTo("0.2000"); // FR-105
        assertThat(dashboard.conversionRate()).isEqualByComparingTo("0.0625"); // FR-106
        assertThat(dashboard.estimatedCost()).isEqualByComparingTo("150.00");
        assertThat(dashboard.estimatedRevenue()).isEqualByComparingTo("200.00");
        assertThat(dashboard.estimatedRoi()).isEqualByComparingTo("0.33"); // FR-107
        assertThat(dashboard.recentCampaignMetrics()).hasSize(2);
        assertThat(dashboard.recentCampaignMetrics().get(0).campaignId()).isEqualTo(CAMPAIGN_ID);
    }

    @Test
    void getDashboardReturnsZeroedViewWhenNoData() {
        when(campaignRepository.findAll()).thenReturn(List.of());
        when(campaignMetricsRepository.findAll()).thenReturn(List.of());

        DashboardView dashboard = analyticsService.getDashboard();

        assertThat(dashboard.campaignTotal()).isZero();
        assertThat(dashboard.activeCampaigns()).isZero();
        assertThat(dashboard.messagesSent()).isZero();
        assertThat(dashboard.openRate()).isEqualByComparingTo("0.0000");
        assertThat(dashboard.recentCampaignMetrics()).isEmpty();
    }

    @Test
    void getCampaignAnalyticsReturnsCampaignWithMetrics() {
        // KB item 432: service-side campaign analytics for GET /api/analytics/campaigns/{id}.
        Campaign campaign = sampleCampaign(CAMPAIGN_ID, "Detail campaign");
        ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(10, 0, 10);
        metrics.recordEngagementCounts(5, 2, 1, 1);
        metrics.updateFinancialEstimates(new BigDecimal("40.00"), new BigDecimal("60.00"));

        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID))
                .thenReturn(Optional.of(metrics));

        CampaignAnalyticsView view = analyticsService.getCampaignAnalytics(CAMPAIGN_ID);

        assertThat(view.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(view.campaignName()).isEqualTo("Detail campaign");
        assertThat(view.status()).isEqualTo(CampaignStatus.ACTIVE);
        assertThat(view.channel()).isEqualTo(CampaignChannel.EMAIL);
        assertThat(view.ownerUserId()).isEqualTo(OWNER_ID);
        assertThat(view.metrics()).isNotNull();
        assertThat(view.metrics().audienceSize()).isEqualTo(10);
        assertThat(view.metrics().eligibleCount()).isEqualTo(10);
        assertThat(view.metrics().sentCount()).isEqualTo(10);
        assertThat(view.metrics().openedCount()).isEqualTo(5);
        assertThat(view.metrics().clickedCount()).isEqualTo(2);
        assertThat(view.metrics().repliedCount()).isEqualTo(1);
        assertThat(view.metrics().convertedCount()).isEqualTo(1);
        assertThat(view.metrics().openRate()).isEqualByComparingTo("0.5000");
        assertThat(view.metrics().clickRate()).isEqualByComparingTo("0.2000");
        assertThat(view.metrics().conversionRate()).isEqualByComparingTo("0.1000");
        assertThat(view.metrics().estimatedCost()).isEqualByComparingTo("40.00");
        assertThat(view.metrics().estimatedRevenue()).isEqualByComparingTo("60.00");
        assertThat(view.metrics().estimatedRoi()).isEqualByComparingTo("0.50");
        assertThat(view.generatedAt()).isNotNull();
        verify(campaignMetricsRepository).findByCampaignId(CAMPAIGN_ID);
    }

    @Test
    void getCampaignAnalyticsAllowsMissingMetricsRow() {
        Campaign campaign = sampleCampaign(CAMPAIGN_ID, "No metrics yet");
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignMetricsRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.empty());

        CampaignAnalyticsView view = analyticsService.getCampaignAnalytics(CAMPAIGN_ID);

        assertThat(view.campaignId()).isEqualTo(CAMPAIGN_ID);
        assertThat(view.metrics()).isNull();
    }

    @Test
    void getCampaignAnalyticsRejectsNullAndMissingCampaign() {
        assertThatThrownBy(() -> analyticsService.getCampaignAnalytics(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Campaign analytics validation failed");

        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> analyticsService.getCampaignAnalytics(CAMPAIGN_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Campaign");
    }

    @Test
    void getProductPerformanceAggregatesMetricsByProduct() {
        // KB item 433: service-side product performance for GET /api/analytics/products/performance.
        Campaign campaign = sampleCampaign(CAMPAIGN_ID, "Product linked campaign");
        Product product =
                Product.create("Life Protection", ProductType.LIFE_INSURANCE, BigDecimal.TEN, 12);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

        CampaignMetrics metrics = CampaignMetrics.forCampaign(campaign);
        metrics.recordLaunchCounts(50, 10, 40);
        metrics.recordEngagementCounts(20, 8, 4, 2);
        metrics.updateFinancialEstimates(new BigDecimal("200.00"), new BigDecimal("300.00"));

        CampaignProduct link = CampaignProduct.link(campaign, product);

        when(campaignProductRepository.findAll()).thenReturn(List.of(link));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));

        List<ProductPerformanceView> rows = analyticsService.getProductPerformance();

        assertThat(rows).hasSize(1);
        ProductPerformanceView row = rows.get(0);
        assertThat(row.productId()).isEqualTo(PRODUCT_ID);
        assertThat(row.productName()).isEqualTo("Life Protection");
        assertThat(row.productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(row.campaignCount()).isEqualTo(1L);
        assertThat(row.audienceSize()).isEqualTo(60L);
        assertThat(row.eligibleCount()).isEqualTo(50L);
        assertThat(row.sentCount()).isEqualTo(40L);
        assertThat(row.openedCount()).isEqualTo(20L);
        assertThat(row.clickedCount()).isEqualTo(8L);
        assertThat(row.convertedCount()).isEqualTo(2L);
        assertThat(row.openRate()).isEqualByComparingTo("0.5000");
        assertThat(row.clickRate()).isEqualByComparingTo("0.2000");
        assertThat(row.conversionRate()).isEqualByComparingTo("0.0500");
        assertThat(row.estimatedCost()).isEqualByComparingTo("200.00");
        assertThat(row.estimatedRevenue()).isEqualByComparingTo("300.00");
        assertThat(row.estimatedRoi()).isEqualByComparingTo("0.50");
    }

    @Test
    void getProductPerformanceReturnsEmptyWhenNoLinks() {
        when(campaignProductRepository.findAll()).thenReturn(List.of());

        assertThat(analyticsService.getProductPerformance()).isEmpty();
    }

    @Test
    void getExecutiveDashboardAggregatesPlatformAndProductPerformance() {
        // KB item 434 / item 457 / COMP-010: service-side executive aggregates for GET
        // /api/analytics/executive. See also ExecutiveReportUsesAggregatedDataTests.
        Campaign active = sampleCampaign(CAMPAIGN_ID, "Active");
        ReflectionTestUtils.setField(active, "status", CampaignStatus.ACTIVE);
        Campaign completed = sampleCampaign(CAMPAIGN_B_ID, "Completed");
        ReflectionTestUtils.setField(completed, "status", CampaignStatus.COMPLETED);

        CampaignMetrics metrics = CampaignMetrics.forCampaign(active);
        metrics.recordLaunchCounts(10, 0, 10);
        metrics.recordEngagementCounts(5, 2, 1, 1);
        metrics.updateFinancialEstimates(new BigDecimal("100.00"), new BigDecimal("120.00"));

        Product product =
                Product.create("Auto Cover", ProductType.AUTO_INSURANCE, BigDecimal.ONE, 6);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        CampaignProduct link = CampaignProduct.link(active, product);

        when(campaignRepository.findAll()).thenReturn(List.of(active, completed));
        when(campaignMetricsRepository.findAll()).thenReturn(List.of(metrics));
        when(campaignProductRepository.findAll()).thenReturn(List.of(link));

        ExecutiveDashboardView view = analyticsService.getExecutiveDashboard();

        assertThat(view.totalCampaigns()).isEqualTo(2L);
        assertThat(view.activeCampaigns()).isEqualTo(1L);
        assertThat(view.completedCampaigns()).isEqualTo(1L);
        assertThat(view.totalAudience()).isEqualTo(10L);
        assertThat(view.totalEligible()).isEqualTo(10L);
        assertThat(view.totalExcluded()).isEqualTo(0L);
        assertThat(view.totalSent()).isEqualTo(10L);
        assertThat(view.totalOpened()).isEqualTo(5L);
        assertThat(view.totalClicked()).isEqualTo(2L);
        assertThat(view.totalReplied()).isEqualTo(1L);
        assertThat(view.totalConverted()).isEqualTo(1L);
        assertThat(view.overallOpenRate()).isEqualByComparingTo("0.5000");
        assertThat(view.overallClickRate()).isEqualByComparingTo("0.2000");
        assertThat(view.overallConversionRate()).isEqualByComparingTo("0.1000");
        assertThat(view.totalEstimatedCost()).isEqualByComparingTo("100.00");
        assertThat(view.totalEstimatedRevenue()).isEqualByComparingTo("120.00");
        assertThat(view.overallEstimatedRoi()).isEqualByComparingTo("0.20");
        assertThat(view.productPerformance()).hasSize(1);
        assertThat(view.productPerformance().get(0).productId()).isEqualTo(PRODUCT_ID);
        assertThat(view.productPerformance().get(0).productName()).isEqualTo("Auto Cover");
    }

    @Test
    void getExecutiveDashboardReturnsZeroedAggregatesWhenNoCampaigns() {
        // KB item 434: empty inventory yields zeroed executive KPIs and empty product rows.
        when(campaignRepository.findAll()).thenReturn(List.of());
        when(campaignMetricsRepository.findAll()).thenReturn(List.of());
        when(campaignProductRepository.findAll()).thenReturn(List.of());

        ExecutiveDashboardView view = analyticsService.getExecutiveDashboard();

        assertThat(view.totalCampaigns()).isZero();
        assertThat(view.activeCampaigns()).isZero();
        assertThat(view.completedCampaigns()).isZero();
        assertThat(view.totalAudience()).isZero();
        assertThat(view.totalSent()).isZero();
        assertThat(view.overallOpenRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(view.overallClickRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(view.overallConversionRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(view.totalEstimatedCost()).isNull();
        assertThat(view.totalEstimatedRevenue()).isNull();
        assertThat(view.overallEstimatedRoi()).isNull();
        assertThat(view.productPerformance()).isEmpty();
    }

    private static Campaign sampleCampaign(UUID campaignId, String name) {
        User owner = User.create("analytics-svc@test.example", "{noop}x", "Analytics Owner");
        ReflectionTestUtils.setField(owner, "id", OWNER_ID);
        Campaign campaign =
                Campaign.create(name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", campaignId);
        return campaign;
    }
}
