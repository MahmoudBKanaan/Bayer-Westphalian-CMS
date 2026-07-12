package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * KB item 459: Analytics documentation.
 *
 * <p>Asserts that {@code docs/modules/analytics-module.md} describes the E19 analytics module
 * (API surface, KPIs, BR-034 metrics lifecycle, COMP-010 executive aggregates, authorization, and
 * frontend screens) and is linked from the documentation index.
 */
@DisplayName("459 Analytics documentation")
class AnalyticsDocumentationTests {

    private static final Path ANALYTICS_MODULE_DOC =
            Path.of("../docs/modules/analytics-module.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path ANALYTICS_PACKAGE_INFO =
            Path.of("src/main/java/com/bayerwestphalian/campaign/analytics/package-info.java");

    @Test
    void documentsAnalyticsModuleBoundaryAndApiSurface() throws Exception {
        String documentation = Files.readString(ANALYTICS_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("# Analytics Module Documentation")
                .contains("## Package Boundary")
                .contains("com.bayerwestphalian.campaign.analytics")
                .contains("com.bayerwestphalian.campaign.campaign")
                .contains("AnalyticsController")
                .contains("AnalyticsService")
                .contains("AnalyticsCalculations")
                .contains("AnalyticsRates")
                .contains("DashboardView")
                .contains("CampaignAnalyticsView")
                .contains("CampaignMetricsView")
                .contains("ProductPerformanceView")
                .contains("ExecutiveDashboardView")
                .contains("## REST API Surface")
                .contains("/api/analytics")
                .contains("/api/analytics/dashboard")
                .contains("/api/analytics/campaigns/{campaignId}")
                .contains("/api/analytics/products/performance")
                .contains("/api/analytics/executive");
    }

    @Test
    void documentsKbTraceabilityAndFr100ThroughFr108() throws Exception {
        String documentation = Files.readString(ANALYTICS_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## KB Traceability")
                .contains("E19")
                .contains("FR-100")
                .contains("FR-101")
                .contains("FR-102")
                .contains("FR-103")
                .contains("FR-104")
                .contains("FR-105")
                .contains("FR-106")
                .contains("FR-107")
                .contains("FR-108")
                .contains("BR-034")
                .contains("COMP-010");
    }

    @Test
    void documentsKpiDefinitionsAndAggregateRateRules() throws Exception {
        String documentation = Files.readString(ANALYTICS_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## KPI Definitions")
                .contains("eligible_count + excluded_count")
                .contains("opened_count / sent_count")
                .contains("clicked_count / sent_count")
                .contains("converted_count / sent_count")
                .contains("(revenue − cost) / cost")
                .contains("Scale 4")
                .contains("Scale 2")
                .contains("aggregate numerators ÷ aggregate denominators")
                .contains("not averages of per-campaign rates")
                .contains("### Dashboard aggregation")
                .contains("### Executive aggregation (COMP-010)")
                .contains("### Product performance")
                .contains("### Campaign analytics detail");
    }

    @Test
    void documentsMetricsLifecycleAndBr034Traceability() throws Exception {
        String documentation = Files.readString(ANALYTICS_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Metrics Lifecycle (Traceability)")
                .contains("campaign_recipients")
                .contains("contact_events")
                .contains("campaign_metrics")
                .contains("CommunicationService")
                .contains("launchCampaign")
                .contains("incrementOpened")
                .contains("BR-034");
    }

    @Test
    void documentsAuthorizationRolesAndFrontendScreens() throws Exception {
        String documentation = Files.readString(ANALYTICS_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Authorization")
                .contains("ADMIN")
                .contains("BI_ANALYST")
                .contains("CAMPAIGN_MANAGER")
                .contains("MARKETING_ANALYST")
                .contains("EXECUTIVE_VIEWER")
                .contains("PRODUCT_MANAGER")
                .contains("SYSTEM_AUDITOR")
                .contains("401 Unauthorized")
                .contains("403 Forbidden")
                .contains("## Frontend Screens")
                .contains("/dashboard")
                .contains("/analytics")
                .contains("/executive")
                .contains("DashboardPage")
                .contains("AnalyticsPage")
                .contains("ExecutiveDashboardPage")
                .contains("frontend/src/api/analytics.ts")
                .contains("Recharts");
    }

    @Test
    void documentsRelatedModulesAndImplementationEvidence() throws Exception {
        String documentation = Files.readString(ANALYTICS_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Related Documentation")
                .contains("communication-tracking.md")
                .contains("campaign-launch.md")
                .contains("role-based-access.md")
                .contains("## Implementation Evidence")
                .contains("CampaignMetrics.java")
                .contains("ExecutiveReportUsesAggregatedDataTests")
                .contains("DashboardEndpointTests");
    }

    @Test
    void documentationIndexLinksAnalyticsModuleDocumentation() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("modules/analytics-module.md")
                .contains("Analytics Module Documentation");
    }

    @Test
    void analyticsPackageInfoReferencesModuleDocumentation() throws Exception {
        String packageInfo = Files.readString(ANALYTICS_PACKAGE_INFO, StandardCharsets.UTF_8);

        assertThat(packageInfo)
                .contains("docs/modules/analytics-module.md")
                .contains("item 459");
    }
}
