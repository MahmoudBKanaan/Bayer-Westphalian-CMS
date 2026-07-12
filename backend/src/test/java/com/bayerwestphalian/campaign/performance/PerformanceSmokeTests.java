package com.bayerwestphalian.campaign.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.analytics.AnalyticsCalculations;
import com.bayerwestphalian.campaign.analytics.AnalyticsService;
import com.bayerwestphalian.campaign.analytics.DashboardView;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignMetrics;
import com.bayerwestphalian.campaign.campaign.CampaignMetricsRepository;
import com.bayerwestphalian.campaign.campaign.CampaignProductRepository;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import com.bayerwestphalian.campaign.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Sprint 16 item <b>639</b> / KB <b>NFR-003</b>: performance smoke checks for search and dashboard.
 *
 * <p>KB target: <em>Normal searches under 1 second for project dataset</em>. These smokes exercise
 * project-scale in-memory workloads that mirror customer/product search filtering and dashboard KPI
 * aggregation without requiring Docker or a live database (fast, deterministic CI).
 *
 * <p>Thresholds are intentionally conservative (≤ 1000 ms). Wall-clock times vary by machine; the
 * budget validates that project-scale volumes do not require multi-second pure CPU work.
 */
@ExtendWith(MockitoExtension.class)
class PerformanceSmokeTests {

    /** KB NFR-003 target: under one second for normal project-dataset operations. */
    static final long NFR_003_BUDGET_MS = 1_000L;

    /**
     * Project-scale synthetic catalog size used for search smokes (insurance marketing MVP order of
     * magnitude: thousands of customers/products, not millions).
     */
    static final int PROJECT_DATASET_SIZE = 5_000;

    /** Campaign metrics rows for dashboard aggregation smoke. */
    static final int DASHBOARD_CAMPAIGN_COUNT = 2_000;

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
    @DisplayName("639 / NFR-003: customer-style search over project dataset finishes under 1s")
    void customerStyleSearchCompletesUnderOneSecondForProjectDataset() {
        List<SearchableCustomer> customers = buildProjectCustomerDataset(PROJECT_DATASET_SIZE);
        String term = "schmidt";

        long started = System.nanoTime();
        List<SearchableCustomer> hits = searchCustomers(customers, term);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertThat(hits)
                .as("search should find seeded surname matches")
                .isNotEmpty()
                .allMatch(c -> matchesCustomerTerm(c, term));
        assertThat(elapsedMs)
                .as(
                        "NFR-003: customer search over %d rows must finish under %d ms (took %d ms)",
                        PROJECT_DATASET_SIZE, NFR_003_BUDGET_MS, elapsedMs)
                .isLessThan(NFR_003_BUDGET_MS);
    }

    @Test
    @DisplayName("639 / NFR-003: product-style search over project dataset finishes under 1s")
    void productStyleSearchCompletesUnderOneSecondForProjectDataset() {
        List<SearchableProduct> products = buildProjectProductDataset(PROJECT_DATASET_SIZE);
        String term = "life";

        long started = System.nanoTime();
        List<SearchableProduct> hits = searchProducts(products, term, "LIFE_INSURANCE", true);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertThat(hits)
                .as("product search should return active life products matching term")
                .isNotEmpty()
                .allMatch(
                        p ->
                                p.active()
                                        && "LIFE_INSURANCE".equals(p.productType())
                                        && matchesProductTerm(p, term));
        assertThat(elapsedMs)
                .as(
                        "NFR-003: product search over %d rows must finish under %d ms (took %d ms)",
                        PROJECT_DATASET_SIZE, NFR_003_BUDGET_MS, elapsedMs)
                .isLessThan(NFR_003_BUDGET_MS);
    }

    @Test
    @DisplayName("639 / NFR-003: dashboard KPI aggregation for project dataset finishes under 1s")
    void dashboardAggregationCompletesUnderOneSecondForProjectDataset() {
        List<Campaign> campaigns = new ArrayList<>(DASHBOARD_CAMPAIGN_COUNT);
        List<CampaignMetrics> metrics = new ArrayList<>(DASHBOARD_CAMPAIGN_COUNT);
        for (int i = 0; i < DASHBOARD_CAMPAIGN_COUNT; i++) {
            UUID id = UUID.nameUUIDFromBytes(("perf-campaign-" + i).getBytes());
            Campaign campaign = sampleCampaign(id, "C" + i);
            if (i % 3 == 0) {
                ReflectionTestUtils.setField(campaign, "status", CampaignStatus.ACTIVE);
            }
            CampaignMetrics row = CampaignMetrics.forCampaign(campaign);
            row.recordLaunchCounts(50 + (i % 20), 5 + (i % 5), 40 + (i % 15));
            campaigns.add(campaign);
            metrics.add(row);
        }

        when(campaignRepository.findAll()).thenReturn(campaigns);
        when(campaignMetricsRepository.findAll()).thenReturn(metrics);

        long started = System.nanoTime();
        DashboardView dashboard = analyticsService.getDashboard();
        // Extra pure aggregation pass mirrors multi-KPI report rollups.
        long audience = AnalyticsCalculations.totalAudienceSize(metrics);
        long sent = AnalyticsCalculations.totalSentCount(metrics);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertThat(dashboard.campaignTotal()).isEqualTo(DASHBOARD_CAMPAIGN_COUNT);
        assertThat(dashboard.audienceSize()).isEqualTo(audience);
        assertThat(dashboard.messagesSent()).isEqualTo(sent);
        assertThat(dashboard.audienceSize()).isPositive();
        assertThat(elapsedMs)
                .as(
                        "NFR-003: dashboard aggregation over %d campaigns must finish under %d ms (took %d ms)",
                        DASHBOARD_CAMPAIGN_COUNT, NFR_003_BUDGET_MS, elapsedMs)
                .isLessThan(NFR_003_BUDGET_MS);
    }

    @Test
    @DisplayName("639: performance smoke budgets and dataset sizes stay KB-aligned")
    void performanceSmokeBudgetsAreKbAligned() {
        assertThat(NFR_003_BUDGET_MS).isEqualTo(1_000L);
        assertThat(PROJECT_DATASET_SIZE).isGreaterThanOrEqualTo(1_000);
        assertThat(DASHBOARD_CAMPAIGN_COUNT).isGreaterThanOrEqualTo(500);
        assertThat(PerformanceSmokeDocumentation.DOC_PATH)
                .isEqualTo("../docs/testing/performance-smoke.md");
    }

    // —— search helpers mirroring KB customer/product multi-field search semantics ——

    static List<SearchableCustomer> buildProjectCustomerDataset(int size) {
        List<SearchableCustomer> customers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String last = (i % 50 == 0) ? "Schmidt" : "Customer" + i;
            customers.add(
                    new SearchableCustomer(
                            "First" + i,
                            last,
                            "user" + i + "@example.com",
                            "City" + (i % 40),
                            "DE",
                            "+49" + (1000000 + i),
                            "import"));
        }
        return customers;
    }

    static List<SearchableProduct> buildProjectProductDataset(int size) {
        List<SearchableProduct> products = new ArrayList<>(size);
        String[] types = {
            "LIFE_INSURANCE",
            "HOMEOWNER_INSURANCE",
            "INVESTMENT_FUND",
            "HEALTH_INSURANCE",
            "AUTO_INSURANCE",
            "OTHER"
        };
        for (int i = 0; i < size; i++) {
            String type = types[i % types.length];
            String name =
                    type.startsWith("LIFE")
                            ? "Life Protection Plan " + i
                            : "Product Catalog Item " + i;
            products.add(new SearchableProduct(name, type, "Desc " + i, i % 7 != 0));
        }
        return products;
    }

    static List<SearchableCustomer> searchCustomers(
            List<SearchableCustomer> customers, String rawTerm) {
        String term = rawTerm == null ? "" : rawTerm.trim().toLowerCase(Locale.ROOT);
        if (term.isEmpty()) {
            return List.copyOf(customers);
        }
        return customers.stream()
                .filter(c -> matchesCustomerTerm(c, term))
                .collect(Collectors.toList());
    }

    static boolean matchesCustomerTerm(SearchableCustomer c, String term) {
        String t = term.toLowerCase(Locale.ROOT);
        return contains(c.firstName(), t)
                || contains(c.lastName(), t)
                || contains(c.email(), t)
                || contains(c.city(), t)
                || contains(c.country(), t)
                || contains(c.phone(), t)
                || contains(c.source(), t);
    }

    static List<SearchableProduct> searchProducts(
            List<SearchableProduct> products, String rawTerm, String productType, Boolean active) {
        String term = rawTerm == null ? "" : rawTerm.trim().toLowerCase(Locale.ROOT);
        return products.stream()
                .filter(p -> productType == null || productType.equals(p.productType()))
                .filter(p -> active == null || active.equals(p.active()))
                .filter(p -> term.isEmpty() || matchesProductTerm(p, term))
                .collect(Collectors.toList());
    }

    static boolean matchesProductTerm(SearchableProduct p, String term) {
        String t = term.toLowerCase(Locale.ROOT);
        return contains(p.name(), t) || contains(p.description(), t) || contains(p.productType(), t);
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    private static Campaign sampleCampaign(UUID id, String name) {
        User owner = User.create("perf-" + name + "@test.example", "{noop}x", "Owner " + name);
        Campaign campaign =
                Campaign.create(
                        "Campaign " + name, "Objective " + name, owner, null, CampaignChannel.EMAIL);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }

    record SearchableCustomer(
            String firstName,
            String lastName,
            String email,
            String city,
            String country,
            String phone,
            String source) {}

    record SearchableProduct(String name, String productType, String description, boolean active) {}

    /**
     * Path constant shared with documentation tests (item 639).
     */
    static final class PerformanceSmokeDocumentation {
        static final String DOC_PATH = "../docs/testing/performance-smoke.md";

        private PerformanceSmokeDocumentation() {}
    }
}
