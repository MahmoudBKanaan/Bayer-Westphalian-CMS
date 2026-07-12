package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * KB item 462: BI Analyst user guide section.
 *
 * <p>Asserts that {@code docs/user-guides/bi-analyst-guide.md} describes BI Analyst analytics,
 * executive, report, and segmentation-insight workflows, explicit restrictions (TC-009, segment
 * edit rules), and is linked from the documentation index.
 */
@DisplayName("462 BI Analyst user guide section")
class BiAnalystUserGuideDocumentationTests {

    private static final Path BI_ANALYST_GUIDE =
            Path.of("../docs/user-guides/bi-analyst-guide.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");

    @Test
    void documentsBiAnalystScopeCapabilitiesAndRestrictions() throws Exception {
        String guide = Files.readString(BI_ANALYST_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("# BI Analyst User Guide")
                .contains("BI_ANALYST")
                .contains("## Scope")
                .contains("campaign performance")
                .contains("product performance")
                .contains("segmentation insights")
                .contains("## What BI Analysts Can Do")
                .contains("## What BI Analysts Cannot Do Alone")
                .contains("cannot")
                .contains("Edit customers")
                .contains("TC-009")
                .contains("Create, submit, approve, reject, or launch campaigns")
                .contains("Manage products")
                .contains("Manage users");
    }

    @Test
    void documentsDashboardAnalyticsAndExecutiveWorkflows() throws Exception {
        String guide = Files.readString(BI_ANALYST_GUIDE, StandardCharsets.UTF_8);

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
                .contains("aggregate numerators")
                .contains("## Executive Dashboard Workflow")
                .contains("/executive")
                .contains("/api/analytics/executive")
                .contains("COMP-010")
                .contains("kpi-definitions.md")
                .contains("analytics-module.md");
    }

    @Test
    void documentsReportsExportWorkflows() throws Exception {
        String guide = Files.readString(BI_ANALYST_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("## Reports Workflows")
                .contains("/reports")
                .contains("/api/reports/campaigns/{campaignId}/csv")
                .contains("/api/reports/campaigns/{campaignId}/pdf")
                .contains("/api/reports/exports")
                .contains("FR-109")
                .contains("FR-110")
                .contains("export history")
                .contains("401 Unauthorized")
                .contains("403 Forbidden")
                .contains("item **458**")
                .contains("report-export.md")
                .contains("same KPI formulas");
    }

    @Test
    void documentsSegmentationCustomerProductAndPractice() throws Exception {
        String guide = Files.readString(BI_ANALYST_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("## Segmentation Insight Workflows")
                .contains("cannot edit segments unless allowed")
                .contains("eligible")
                .contains("excluded")
                .contains("segmentation-user-guide.md")
                .contains("## Customer And Product Context (Read-Only)")
                .contains("cannot")
                .contains("import customers")
                .contains("## Recommended BI Analyst Practice")
                .contains("BR-034")
                .contains("campaign_metrics");
    }

    @Test
    void documentsAccessErrorsAuditAndKbTraceability() throws Exception {
        String guide = Files.readString(BI_ANALYST_GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("## Access And Error Handling")
                .contains("Backend authorization is authoritative")
                .contains("canViewAnalytics")
                .contains("canViewReports")
                .contains("canExportReports")
                .contains("permissions.ts")
                .contains("## Audit Expectations")
                .contains("report_exports")
                .contains("## Related Documentation")
                .contains("## KB Traceability")
                .contains("Role description")
                .contains("Allowed functions")
                .contains("Screens")
                .contains("FR-100")
                .contains("FR-109")
                .contains("FR-110")
                .contains("BR-034")
                .contains("COMP-010")
                .contains("TC-009")
                .contains("Item **200**")
                .contains("Item **458**")
                .contains("Item **462**");
    }

    @Test
    void documentationIndexLinksBiAnalystGuide() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("user-guides/bi-analyst-guide.md")
                .contains("BI Analyst User Guide");
    }
}
