package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * KB item 463: Executive Viewer guide section.
 *
 * <p>Asserts that {@code docs/user-guides/executive-viewer-guide.md} describes Executive Viewer
 * high-level dashboard, aggregate executive KPIs (COMP-010), management report export workflows,
 * explicit operational restrictions, and is linked from the documentation index.
 */
@DisplayName("463 Executive Viewer guide section")
class ExecutiveViewerUserGuideDocumentationTests {

    private static final Path EXECUTIVE_VIEWER_GUIDE =
            Path.of("../docs/user-guides/executive-viewer-guide.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");

    @Test
    void documentsExecutiveViewerScopeCapabilitiesAndRestrictions() throws Exception {
        String guide = Files.readString(EXECUTIVE_VIEWER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("# Executive Viewer User Guide")
                .contains("EXECUTIVE_VIEWER")
                .contains("## Scope")
                .contains("high-level")
                .contains("read-only")
                .contains("management reports")
                .contains("estimated ROI")
                .contains("## What Executive Viewers Can Do")
                .contains("## What Executive Viewers Cannot Do")
                .contains("cannot")
                .contains("customers")
                .contains("campaigns")
                .contains("products")
                .contains("users")
                .contains("cannot approve or launch campaigns");
    }

    @Test
    void documentsExecutiveDashboardAggregationRules() throws Exception {
        String guide = Files.readString(EXECUTIVE_VIEWER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("## Primary Screen: Executive Dashboard")
                .contains("/executive")
                .contains("/api/analytics/executive")
                .contains("COMP-010")
                .contains("item **457**")
                .contains("### Aggregation rules")
                .contains("sums")
                .contains("Σ numerator ÷ Σ sent")
                .contains("not")
                .contains("averages of per-campaign rates")
                .contains("total cost and total revenue")
                .contains("does **not** list raw contact events")
                .contains("kpi-definitions.md")
                .contains("analytics-module.md");
    }

    @Test
    void documentsDashboardAnalyticsAndReportsWorkflows() throws Exception {
        String guide = Files.readString(EXECUTIVE_VIEWER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("## Dashboard Workflow")
                .contains("/dashboard")
                .contains("/api/analytics/dashboard")
                .contains("FR-100")
                .contains("FR-107")
                .contains("## Analytics Workflows")
                .contains("/analytics")
                .contains("/api/analytics/campaigns/{campaignId}")
                .contains("/api/analytics/products/performance")
                .contains("Recharts")
                .contains("FR-108")
                .contains("## Reports Workflows")
                .contains("/reports")
                .contains("/api/reports/campaigns/{campaignId}/csv")
                .contains("/api/reports/campaigns/{campaignId}/pdf")
                .contains("/api/reports/exports")
                .contains("FR-109")
                .contains("FR-110")
                .contains("401 Unauthorized")
                .contains("403 Forbidden")
                .contains("item **458**")
                .contains("report-export.md")
                .contains("same KPI definitions");
    }

    @Test
    void documentsCampaignProductSummariesAndRecommendedPractice() throws Exception {
        String guide = Files.readString(EXECUTIVE_VIEWER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("## Campaign And Product Summaries")
                .contains("product performance")
                .contains("cannot approve or launch campaigns")
                .contains("## Recommended Executive Viewer Practice")
                .contains("Executive")
                .contains("messages sent")
                .contains("BR-034")
                .contains("board packs");
    }

    @Test
    void documentsAccessErrorsAuditAndKbTraceability() throws Exception {
        String guide = Files.readString(EXECUTIVE_VIEWER_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("## Access And Error Handling")
                .contains("Backend authorization is authoritative")
                .contains("canViewAnalytics")
                .contains("canViewExecutiveDashboard")
                .contains("canViewReports")
                .contains("canExportReports")
                .contains("permissions.ts")
                .contains("## Audit Expectations")
                .contains("report_exports")
                .contains("## Related Documentation")
                .contains("bi-analyst-guide.md")
                .contains("## KB Traceability")
                .contains("Role description")
                .contains("Allowed functions")
                .contains("Screens")
                .contains("As an Executive, I want to view ROI and campaign summaries")
                .contains("FR-100")
                .contains("FR-109")
                .contains("FR-110")
                .contains("COMP-010")
                .contains("BR-034")
                .contains("Item **457**")
                .contains("Item **458**")
                .contains("Item **463**");
    }

    @Test
    void documentationIndexLinksExecutiveViewerGuide() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("user-guides/executive-viewer-guide.md")
                .contains("Executive Viewer User Guide");
    }
}
