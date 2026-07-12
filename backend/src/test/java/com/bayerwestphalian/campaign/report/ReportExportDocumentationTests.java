package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * KB item 460: Report export documentation.
 *
 * <p>Asserts that {@code docs/modules/report-export.md} describes the E20 report export module
 * (CSV/PDF APIs, document content, export history lifecycle, authorization item 458, frontend, and
 * analytics traceability) and is linked from the documentation index.
 */
@DisplayName("460 Report export documentation")
class ReportExportDocumentationTests {

    private static final Path REPORT_EXPORT_DOC = Path.of("../docs/modules/report-export.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path REPORT_PACKAGE_INFO =
            Path.of("src/main/java/com/bayerwestphalian/campaign/report/package-info.java");
    private static final Path ANALYTICS_MODULE_DOC =
            Path.of("../docs/modules/analytics-module.md");

    @Test
    void documentsReportExportModuleBoundaryAndApiSurface() throws Exception {
        String documentation = Files.readString(REPORT_EXPORT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("# Report Export Documentation")
                .contains("## Package Boundary")
                .contains("com.bayerwestphalian.campaign.report")
                .contains("ReportController")
                .contains("ReportService")
                .contains("CampaignReportDocument")
                .contains("ReportExport")
                .contains("ReportExportRepository")
                .contains("ReportExportType")
                .contains("ReportExportStatus")
                .contains("ReportExportView")
                .contains("ReportFile")
                .contains("## REST API Surface")
                .contains("/api/reports")
                .contains("/api/reports/campaigns/{campaignId}/csv")
                .contains("/api/reports/campaigns/{campaignId}/pdf")
                .contains("/api/reports/exports")
                .contains("/api/reports/exports/{exportId}");
    }

    @Test
    void documentsKbTraceabilityFr109Fr110AndRelatedItems() throws Exception {
        String documentation = Files.readString(REPORT_EXPORT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## KB Traceability")
                .contains("E20")
                .contains("FR-109")
                .contains("FR-110")
                .contains("COMP-010")
                .contains("Item **435**")
                .contains("Item **437**")
                .contains("Item **438**")
                .contains("Item **439**")
                .contains("Item **458**")
                .contains("item 455");
    }

    @Test
    void documentsCsvAndPdfContentAndAttachmentHeaders() throws Exception {
        String documentation = Files.readString(REPORT_EXPORT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Campaign CSV Content (FR-109)")
                .contains("campaignId")
                .contains("openRate")
                .contains("estimatedRoi")
                .contains("exportCampaignCsv")
                .contains("## Campaign PDF Content (FR-110)")
                .contains("PDF 1.4")
                .contains("Bayer-Westphalian Campaign Report")
                .contains("generateCampaignPdf")
                .contains("Metrics: (none recorded yet)")
                .contains("Content-Disposition")
                .contains("text/csv")
                .contains("application/pdf");
    }

    @Test
    void documentsExportHistoryLifecycle() throws Exception {
        String documentation = Files.readString(REPORT_EXPORT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Export History Lifecycle (Item 439)")
                .contains("report_exports")
                .contains("REQUESTED")
                .contains("COMPLETED")
                .contains("FAILED")
                .contains("Campaign CSV:")
                .contains("Campaign PDF:")
                .contains("local://reports/")
                .contains("AnalyticsService")
                .contains("mine")
                .contains("status");
    }

    @Test
    void documentsAuthorizationAndUnauthorizedExportRules() throws Exception {
        String documentation = Files.readString(REPORT_EXPORT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Authorization (Item 458)")
                .contains("ADMIN")
                .contains("BI_ANALYST")
                .contains("CAMPAIGN_MANAGER")
                .contains("MARKETING_ANALYST")
                .contains("EXECUTIVE_VIEWER")
                .contains("PRODUCT_MANAGER")
                .contains("SYSTEM_AUDITOR")
                .contains("401 Unauthorized")
                .contains("403 Forbidden")
                .contains("canViewReports")
                .contains("exportAuditReport")
                .contains("COMPLIANCE_OFFICER");
    }

    @Test
    void documentsFrontendScreensAndAnalyticsTraceability() throws Exception {
        String documentation = Files.readString(REPORT_EXPORT_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Frontend Screens")
                .contains("ReportsPage")
                .contains("ReportDownloadPanel")
                .contains("frontend/src/api/reports.ts")
                .contains("## Traceability to Analytics and Contact Events")
                .contains("CampaignAnalyticsView")
                .contains("campaign_metrics")
                .contains("analytics-module.md")
                .contains("Item 466")
                .contains("ReportsUseAggregatedDataAndMetricsAreTraceableTests")
                .contains("## Related Documentation")
                .contains("## Implementation Evidence")
                .contains("CsvExportWorksTests")
                .contains("PdfExportWorksTests")
                .contains("UnauthorizedUserCannotExportRestrictedReportsTests");
    }

    @Test
    void documentationIndexLinksReportExportDocumentation() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("modules/report-export.md")
                .contains("Report Export Documentation");
    }

    @Test
    void reportPackageInfoReferencesModuleDocumentation() throws Exception {
        String packageInfo = Files.readString(REPORT_PACKAGE_INFO, StandardCharsets.UTF_8);

        assertThat(packageInfo)
                .contains("docs/modules/report-export.md")
                .contains("item 460");
    }

    @Test
    void analyticsModuleDocumentationLinksReportExportDoc() throws Exception {
        String analyticsDoc = Files.readString(ANALYTICS_MODULE_DOC, StandardCharsets.UTF_8);

        assertThat(analyticsDoc)
                .contains("report-export.md")
                .contains("Report Export Documentation");
    }
}
