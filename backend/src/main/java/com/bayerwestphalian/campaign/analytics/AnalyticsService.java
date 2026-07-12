package com.bayerwestphalian.campaign.analytics;

import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignMetrics;
import com.bayerwestphalian.campaign.campaign.CampaignMetricsRepository;
import com.bayerwestphalian.campaign.campaign.CampaignProduct;
import com.bayerwestphalian.campaign.campaign.CampaignProductRepository;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.product.Product;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Campaign analytics and dashboard aggregation (KB epic E19 / item 415).
 *
 * <p>Builds {@link DashboardView} (item 431), {@link CampaignAnalyticsView} (item 432), product
 * performance rows (item 433), and {@link ExecutiveDashboardView} (item 434 / item 457 / COMP-010)
 * from {@link CampaignMetrics} and campaign product links.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final String ANALYTICS_READ =
            "@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER', "
                    + "'MARKETING_ANALYST', 'EXECUTIVE_VIEWER')";

    private static final int DASHBOARD_RECENT_LIMIT = 10;

    private final CampaignRepository campaignRepository;
    private final CampaignMetricsRepository campaignMetricsRepository;
    private final CampaignProductRepository campaignProductRepository;

    public AnalyticsService(
            CampaignRepository campaignRepository,
            CampaignMetricsRepository campaignMetricsRepository,
            CampaignProductRepository campaignProductRepository) {
        this.campaignRepository = campaignRepository;
        this.campaignMetricsRepository = campaignMetricsRepository;
        this.campaignProductRepository = campaignProductRepository;
    }

    /**
     * Platform dashboard KPIs (KB item 431 / FR-100–FR-107 / GET {@code /api/analytics/dashboard}).
     *
     * <p>Aggregates campaign inventory and {@link CampaignMetrics} into a single {@link
     * DashboardView} for authorized analytics roles.
     */
    @PreAuthorize(ANALYTICS_READ)
    public DashboardView getDashboard() {
        List<Campaign> campaigns = campaignRepository.findAll();
        List<CampaignMetrics> allMetrics = campaignMetricsRepository.findAll();

        // FR-100: dashboard shows campaign totals.
        long campaignTotal = campaigns.size();
        // FR-101: dashboard shows active campaigns.
        long activeCampaigns =
                campaigns.stream().filter(campaign -> campaign.getStatus() == CampaignStatus.ACTIVE)
                        .count();

        // KB item 417 / FR-102: dashboard audience size is the sum of per-campaign audience sizes.
        long audienceSize = AnalyticsCalculations.totalAudienceSize(allMetrics);
        // KB item 418 / item 447: dashboard eligible count is the sum of per-campaign eligible counts.
        long eligibleCount = AnalyticsCalculations.totalEligibleCount(allMetrics);
        // KB item 419 / item 448: dashboard excluded count is the sum of per-campaign excluded counts.
        long excludedCount = AnalyticsCalculations.totalExcludedCount(allMetrics);
        // KB item 420 / item 449 / FR-103: dashboard messages sent is the sum of launch-updated
        // per-campaign sent counts.
        long messagesSent = AnalyticsCalculations.totalSentCount(allMetrics);
        // KB item 421: dashboard opened count is the sum of per-campaign opened counts.
        long openedCount = AnalyticsCalculations.totalOpenedCount(allMetrics);
        // KB item 422: dashboard clicked count is the sum of per-campaign clicked counts.
        long clickedCount = AnalyticsCalculations.totalClickedCount(allMetrics);
        // KB item 423: dashboard replied count is the sum of per-campaign replied counts.
        long repliedCount = AnalyticsCalculations.totalRepliedCount(allMetrics);
        // KB item 424: dashboard converted count is the sum of per-campaign converted counts.
        long convertedCount = AnalyticsCalculations.totalConvertedCount(allMetrics);
        // KB item 428: dashboard estimated cost is the sum of per-campaign estimated costs.
        BigDecimal estimatedCost = AnalyticsCalculations.totalEstimatedCost(allMetrics);
        // KB item 429: dashboard estimated revenue is the sum of per-campaign revenues.
        BigDecimal estimatedRevenue = AnalyticsCalculations.totalEstimatedRevenue(allMetrics);
        List<CampaignMetricsView> recent =
                allMetrics.stream()
                        .sorted(
                                Comparator.comparing(
                                                CampaignMetrics::getUpdatedAt,
                                                Comparator.nullsLast(Comparator.naturalOrder()))
                                        .reversed())
                        .limit(DASHBOARD_RECENT_LIMIT)
                        .map(CampaignMetricsView::from)
                        .toList();

        return new DashboardView(
                campaignTotal,
                activeCampaigns,
                audienceSize,
                messagesSent,
                eligibleCount,
                excludedCount,
                openedCount,
                clickedCount,
                repliedCount,
                convertedCount,
                // KB item 425 / item 451 / FR-104: dashboard open rate from total opened / total sent.
                AnalyticsCalculations.calculateOpenRate(openedCount, messagesSent),
                // KB item 426 / item 452 / FR-105: dashboard click rate from total clicked / total sent.
                AnalyticsCalculations.calculateClickRate(clickedCount, messagesSent),
                // KB item 427 / item 453 / FR-106: dashboard conversion rate from total converted / total sent.
                AnalyticsCalculations.calculateConversionRate(convertedCount, messagesSent),
                estimatedCost,
                estimatedRevenue,
                // KB item 430 / item 454 / FR-107: dashboard ROI from aggregate cost and revenue.
                AnalyticsCalculations.calculateEstimatedRoi(estimatedCost, estimatedRevenue),
                recent);
    }

    /**
     * Single-campaign analytics detail (KB item 432 / GET {@code
     * /api/analytics/campaigns/{campaignId}}).
     *
     * <p>Loads campaign identity and optional {@link CampaignMetrics} for the campaign (via {@link
     * CampaignMetricsRepository#findByCampaignId}). Metrics are mapped through {@link
     * CampaignMetricsView} so KPI calculators (audience, rates, ROI, etc.) are applied consistently
     * with the dashboard.
     */
    @PreAuthorize(ANALYTICS_READ)
    public CampaignAnalyticsView getCampaignAnalytics(UUID campaignId) {
        if (campaignId == null) {
            throw new ValidationException(
                    "Campaign analytics validation failed", List.of("campaignId: is required"));
        }

        Campaign campaign =
                campaignRepository
                        .findById(campaignId)
                        .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        // Optional: draft / not-yet-launched campaigns may have no metrics row.
        CampaignMetrics metrics =
                campaignMetricsRepository.findByCampaignId(campaignId).orElse(null);

        return CampaignAnalyticsView.of(
                campaign.getId(),
                campaign.getName(),
                campaign.getObjective(),
                campaign.getStatus(),
                campaign.getChannel(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                campaign.getOwnerUserId(),
                campaign.getOwner() == null ? null : campaign.getOwner().getFullName(),
                metrics);
    }

    /**
     * Product performance rows (KB item 433 / GET {@code /api/analytics/products/performance}).
     *
     * <p>Aggregates {@link CampaignProduct} links with per-campaign {@link CampaignMetrics} into one
     * row per product (sorted by product name). Rates and ROI use the same KPI calculators as the
     * dashboard.
     */
    @PreAuthorize(ANALYTICS_READ)
    public List<ProductPerformanceView> getProductPerformance() {
        return buildProductPerformance();
    }

    /**
     * Executive aggregate dashboard (KB item 434 / item 457 / COMP-010 / GET {@code
     * /api/analytics/executive}).
     *
     * <p>Builds a single {@link ExecutiveDashboardView} from campaign inventory and summed {@link
     * CampaignMetrics}: totals for active/completed campaigns, audience funnel, engagement, rates
     * and ROI derived from aggregates (not per-campaign averages), plus product performance rows
     * from {@link #buildProductPerformance()} for management drill-down. Acceptance item 457:
     * executive report uses aggregated data only (COMP-010).
     */
    @PreAuthorize(ANALYTICS_READ)
    public ExecutiveDashboardView getExecutiveDashboard() {
        List<Campaign> campaigns = campaignRepository.findAll();
        List<CampaignMetrics> allMetrics = campaignMetricsRepository.findAll();

        // KB item 434 / item 457 / COMP-010: campaign inventory aggregates for executive reporting.
        long totalCampaigns = campaigns.size();
        long activeCampaigns =
                campaigns.stream().filter(c -> c.getStatus() == CampaignStatus.ACTIVE).count();
        long completedCampaigns =
                campaigns.stream().filter(c -> c.getStatus() == CampaignStatus.COMPLETED).count();

        MetricsTotals totals = sumMetrics(allMetrics);
        // KB item 417 / FR-102: executive total audience is the sum of per-campaign audience sizes.
        long totalAudience = AnalyticsCalculations.totalAudienceSize(allMetrics);
        // KB item 418 / item 447: executive total eligible is the sum of per-campaign eligible counts.
        long totalEligible = AnalyticsCalculations.totalEligibleCount(allMetrics);
        // KB item 419 / item 448: executive total excluded is the sum of per-campaign excluded counts.
        long totalExcluded = AnalyticsCalculations.totalExcludedCount(allMetrics);
        // KB item 420 / item 449 / FR-103: executive total sent is the sum of launch-updated
        // per-campaign sent counts.
        long totalSent = AnalyticsCalculations.totalSentCount(allMetrics);
        // KB item 421: executive total opened is the sum of per-campaign opened counts.
        long totalOpened = AnalyticsCalculations.totalOpenedCount(allMetrics);
        // KB item 422: executive total clicked is the sum of per-campaign clicked counts.
        long totalClicked = AnalyticsCalculations.totalClickedCount(allMetrics);
        // KB item 423: executive total replied is the sum of per-campaign replied counts.
        long totalReplied = AnalyticsCalculations.totalRepliedCount(allMetrics);
        // KB item 424: executive total converted is the sum of per-campaign converted counts.
        long totalConverted = AnalyticsCalculations.totalConvertedCount(allMetrics);
        // KB item 433/434: product performance summary embedded for executive drill-down.
        List<ProductPerformanceView> productPerformance = buildProductPerformance();

        return new ExecutiveDashboardView(
                totalCampaigns,
                activeCampaigns,
                completedCampaigns,
                totalAudience,
                totalEligible,
                totalExcluded,
                totalSent,
                totalOpened,
                totalClicked,
                totalReplied,
                totalConverted,
                // KB item 425 / item 451 / FR-104: executive open rate from total opened / total sent.
                AnalyticsCalculations.calculateOpenRate(totalOpened, totalSent),
                // KB item 426 / item 452 / FR-105: executive click rate from total clicked / total sent.
                AnalyticsCalculations.calculateClickRate(totalClicked, totalSent),
                // KB item 427 / item 453 / FR-106: executive conversion rate from total converted / total sent.
                AnalyticsCalculations.calculateConversionRate(totalConverted, totalSent),
                // KB item 428: executive total estimated cost is the sum of per-campaign costs.
                totals.estimatedCost(),
                // KB item 429: executive total estimated revenue is the sum of per-campaign revenues.
                totals.estimatedRevenue(),
                // KB item 430 / item 454 / FR-107: executive ROI from aggregate cost and revenue.
                AnalyticsCalculations.calculateEstimatedRoi(
                        totals.estimatedCost(), totals.estimatedRevenue()),
                productPerformance);
    }

    /**
     * Builds product performance rows for item 433 (and executive dashboard product summary).
     *
     * <p>Groups campaign–product links by product id, sums linked campaign metrics, and derives
     * rates from product-level opened/clicked/converted over sent.
     */
    private List<ProductPerformanceView> buildProductPerformance() {
        List<CampaignProduct> links = campaignProductRepository.findAll();
        if (links.isEmpty()) {
            return List.of();
        }

        Map<UUID, CampaignMetrics> metricsByCampaignId = indexMetricsByCampaignId();
        Map<UUID, ProductAccumulator> byProduct = new LinkedHashMap<>();

        for (CampaignProduct link : links) {
            Product product = link.getProduct();
            if (product == null || product.getId() == null) {
                continue;
            }
            UUID productId = product.getId();
            ProductAccumulator accumulator =
                    byProduct.computeIfAbsent(
                            productId,
                            id ->
                                    new ProductAccumulator(
                                            productId,
                                            product.getName(),
                                            product.getProductType()));

            UUID campaignId = link.getCampaignId();
            CampaignMetrics metrics =
                    campaignId == null ? null : metricsByCampaignId.get(campaignId);
            // Campaigns without metrics still count toward campaignCount.
            accumulator.addCampaign(metrics);
        }

        return byProduct.values().stream()
                .map(ProductAccumulator::toView)
                .sorted(
                        Comparator.comparing(
                                ProductPerformanceView::productName,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private Map<UUID, CampaignMetrics> indexMetricsByCampaignId() {
        Map<UUID, CampaignMetrics> byCampaign = new HashMap<>();
        for (CampaignMetrics metrics : campaignMetricsRepository.findAll()) {
            UUID campaignId = metrics.getCampaignId();
            if (campaignId != null) {
                byCampaign.put(campaignId, metrics);
            }
        }
        return byCampaign;
    }

    private static MetricsTotals sumMetrics(List<CampaignMetrics> metricsList) {
        long audience = 0L;
        long eligible = 0L;
        long excluded = 0L;
        long sent = 0L;
        long opened = 0L;
        long clicked = 0L;
        long replied = 0L;
        long converted = 0L;
        BigDecimal cost = null;
        BigDecimal revenue = null;

        for (CampaignMetrics metrics : metricsList) {
            if (metrics == null) {
                continue;
            }
            // Prefer derived audience size (eligible + excluded) over a possibly stale stored value.
            audience += AnalyticsCalculations.calculateAudienceSize(metrics);
            // KB item 418: eligible count from metrics (ELIGIBLE recipient total at launch).
            eligible += AnalyticsCalculations.calculateEligibleCount(metrics);
            // KB item 419 / item 448: excluded count from metrics (EXCLUDED recipient total at launch).
            excluded += AnalyticsCalculations.calculateExcludedCount(metrics);
            // KB item 420 / item 449: sent count from metrics (SENT contact events after launch).
            sent += AnalyticsCalculations.calculateSentCount(metrics);
            // KB item 421: opened count from metrics (OPENED contact events).
            opened += AnalyticsCalculations.calculateOpenedCount(metrics);
            // KB item 422: clicked count from metrics (CLICKED contact events).
            clicked += AnalyticsCalculations.calculateClickedCount(metrics);
            // KB item 423: replied count from metrics (REPLIED contact events).
            replied += AnalyticsCalculations.calculateRepliedCount(metrics);
            // KB item 424: converted count from metrics (conversion outcomes).
            converted += AnalyticsCalculations.calculateConvertedCount(metrics);
            // KB item 428: estimated cost from metrics (normalized monetary estimate).
            cost = addMoney(cost, AnalyticsCalculations.calculateEstimatedCost(metrics));
            // KB item 429: estimated revenue from metrics (normalized monetary estimate).
            revenue = addMoney(revenue, AnalyticsCalculations.calculateEstimatedRevenue(metrics));
        }

        return new MetricsTotals(
                audience, eligible, excluded, sent, opened, clicked, replied, converted, cost,
                revenue);
    }

    private static BigDecimal addMoney(BigDecimal current, BigDecimal addend) {
        if (addend == null) {
            return current;
        }
        if (current == null) {
            return addend;
        }
        return current.add(addend);
    }

    private record MetricsTotals(
            long audienceSize,
            long eligibleCount,
            long excludedCount,
            long sentCount,
            long openedCount,
            long clickedCount,
            long repliedCount,
            long convertedCount,
            BigDecimal estimatedCost,
            BigDecimal estimatedRevenue) {}

    private static final class ProductAccumulator {
        private final UUID productId;
        private final String productName;
        private final com.bayerwestphalian.campaign.product.ProductType productType;
        private long campaignCount;
        private long audienceSize;
        private long eligibleCount;
        private long sentCount;
        private long openedCount;
        private long clickedCount;
        private long convertedCount;
        private BigDecimal estimatedCost;
        private BigDecimal estimatedRevenue;

        private ProductAccumulator(
                UUID productId,
                String productName,
                com.bayerwestphalian.campaign.product.ProductType productType) {
            this.productId = Objects.requireNonNull(productId);
            this.productName = productName;
            this.productType = productType;
        }

        private void addCampaign(CampaignMetrics metrics) {
            campaignCount++;
            if (metrics == null) {
                return;
            }
            audienceSize += AnalyticsCalculations.calculateAudienceSize(metrics);
            eligibleCount += AnalyticsCalculations.calculateEligibleCount(metrics);
            sentCount += AnalyticsCalculations.calculateSentCount(metrics);
            // KB item 421: product-level opened aggregate from per-campaign opened counts.
            openedCount += AnalyticsCalculations.calculateOpenedCount(metrics);
            // KB item 422: product-level clicked aggregate from per-campaign clicked counts.
            clickedCount += AnalyticsCalculations.calculateClickedCount(metrics);
            // KB item 424: product-level converted aggregate from per-campaign converted counts.
            convertedCount += AnalyticsCalculations.calculateConvertedCount(metrics);
            // KB item 428: product-level estimated cost aggregate.
            estimatedCost =
                    addMoney(estimatedCost, AnalyticsCalculations.calculateEstimatedCost(metrics));
            // KB item 429: product-level estimated revenue aggregate.
            estimatedRevenue =
                    addMoney(
                            estimatedRevenue,
                            AnalyticsCalculations.calculateEstimatedRevenue(metrics));
        }

        private ProductPerformanceView toView() {
            return ProductPerformanceView.of(
                    productId,
                    productName,
                    productType,
                    campaignCount,
                    audienceSize,
                    eligibleCount,
                    sentCount,
                    openedCount,
                    clickedCount,
                    convertedCount,
                    estimatedCost,
                    estimatedRevenue,
                    // KB item 430 / item 454 / FR-107: product-level ROI from aggregated cost/revenue.
                    AnalyticsCalculations.calculateEstimatedRoi(estimatedCost, estimatedRevenue));
        }
    }
}
