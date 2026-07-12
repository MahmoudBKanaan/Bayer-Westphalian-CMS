package com.bayerwestphalian.campaign.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * KB item 461: KPI definition document.
 *
 * <p>Asserts that {@code docs/modules/kpi-definitions.md} is the formal catalog of campaign
 * performance KPIs (formulas, scale/rounding, aggregation rules, FR-100–107 / BR-034 / COMP-010
 * traceability, and acceptance mapping) and is linked from the documentation index and related
 * module docs.
 */
@DisplayName("461 KPI definition document")
class KpiDefinitionDocumentationTests {

    private static final Path KPI_DEFINITIONS_DOC = Path.of("../docs/modules/kpi-definitions.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path ANALYTICS_PACKAGE_INFO =
            Path.of("src/main/java/com/bayerwestphalian/campaign/analytics/package-info.java");
    private static final Path ANALYTICS_MODULE_DOC =
            Path.of("../docs/modules/analytics-module.md");
    private static final Path REPORT_EXPORT_DOC = Path.of("../docs/modules/report-export.md");

    @Test
    void documentsFormalKpiCatalogTitleAndPurpose() throws Exception {
        String documentation = Files.readString(KPI_DEFINITIONS_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("# KPI Definition Document")
                .contains("## Purpose")
                .contains("formal catalog")
                .contains("CampaignMetrics")
                .contains("AnalyticsCalculations")
                .contains("AnalyticsRates")
                .contains("AnalyticsService")
                .contains("FR-109")
                .contains("FR-110")
                .contains("must not invent separate KPI formulas");
    }

    @Test
    void documentsKbTraceabilityFr100ThroughFr110() throws Exception {
        String documentation = Files.readString(KPI_DEFINITIONS_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## KB Traceability")
                .contains("E19")
                .contains("E20")
                .contains("FR-100")
                .contains("FR-101")
                .contains("FR-102")
                .contains("FR-103")
                .contains("FR-104")
                .contains("FR-105")
                .contains("FR-106")
                .contains("FR-107")
                .contains("FR-108")
                .contains("FR-109")
                .contains("FR-110")
                .contains("BR-034")
                .contains("COMP-010")
                .contains("Item **461**");
    }

    @Test
    void documentsPrecisionRoundingAndAggregationRules() throws Exception {
        String documentation = Files.readString(KPI_DEFINITIONS_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Precision and Rounding")
                .contains("Scale 4")
                .contains("Scale 2")
                .contains("HALF_UP")
                .contains("RATE_SCALE")
                .contains("MONEY_SCALE")
                .contains("## Aggregation Rules (Mandatory)")
                .contains("Sum counters first")
                .contains("aggregate numerators and aggregate denominators")
                .contains("not")
                .contains("arithmetic mean of per-campaign rates")
                .contains("not")
                .contains("average of per-campaign ROI");
    }

    @Test
    void documentsCountRateAndFinancialKpiFormulas() throws Exception {
        String documentation = Files.readString(KPI_DEFINITIONS_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Count KPIs")
                .contains("eligible_count + excluded_count")
                .contains("ELIGIBLE")
                .contains("EXCLUDED")
                .contains("sent_count")
                .contains("opened_count")
                .contains("clicked_count")
                .contains("replied_count")
                .contains("converted_count")
                .contains("## Rate KPIs")
                .contains("opened_count / sent_count")
                .contains("clicked_count / sent_count")
                .contains("converted_count / sent_count")
                .contains("## Financial KPIs")
                .contains("(revenue − cost) / cost")
                .contains("if cost missing")
                .contains("if cost is zero");
    }

    @Test
    void documentsTraceabilityLifecycleAndSurfaces() throws Exception {
        String documentation = Files.readString(KPI_DEFINITIONS_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Source of Truth and Traceability")
                .contains("campaign_recipients")
                .contains("contact_events")
                .contains("campaign_metrics")
                .contains("CommunicationService")
                .contains("launchCampaign")
                .contains("## Surfaces That Display KPIs")
                .contains("/api/analytics/dashboard")
                .contains("/api/analytics/executive")
                .contains("/api/reports/campaigns/{id}/csv")
                .contains("/api/reports/campaigns/{id}/pdf")
                .contains("## Product Performance KPIs")
                .contains("## Worked Examples")
                .contains("## Acceptance Mapping");
    }

    @Test
    void documentsAcceptanceMappingForKpiSuites() throws Exception {
        String documentation = Files.readString(KPI_DEFINITIONS_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("AudienceSizeIsCalculatedCorrectlyTests")
                .contains("OpenRateIsCalculatedCorrectlyTests")
                .contains("ClickRateIsCalculatedCorrectlyTests")
                .contains("ConversionRateIsCalculatedCorrectlyTests")
                .contains("RoiIsCalculatedCorrectlyTests")
                .contains("ExecutiveReportUsesAggregatedDataTests")
                .contains("EngagementCountsUpdateFromContactEventsTests")
                .contains("446")
                .contains("454")
                .contains("457");
    }

    @Test
    void documentsRelatedModulesAndImplementationEvidence() throws Exception {
        String documentation = Files.readString(KPI_DEFINITIONS_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Related Documentation")
                .contains("analytics-module.md")
                .contains("report-export.md")
                .contains("communication-tracking.md")
                .contains("campaign-launch.md")
                .contains("## Implementation Evidence")
                .contains("CampaignMetrics.java")
                .contains("AnalyticsCalculations.java")
                .contains("kpi-definitions.md")
                .contains("KpiDefinitionDocumentationTests");
    }

    @Test
    void documentationIndexLinksKpiDefinitionDocument() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("modules/kpi-definitions.md")
                .contains("KPI Definition Document");
    }

    @Test
    void analyticsPackageInfoReferencesKpiDefinitionDocument() throws Exception {
        String packageInfo = Files.readString(ANALYTICS_PACKAGE_INFO, StandardCharsets.UTF_8);

        assertThat(packageInfo)
                .contains("docs/modules/kpi-definitions.md")
                .contains("item 461");
    }

    @Test
    void analyticsAndReportModuleDocsLinkKpiDefinitions() throws Exception {
        String analyticsDoc = Files.readString(ANALYTICS_MODULE_DOC, StandardCharsets.UTF_8);
        String reportDoc = Files.readString(REPORT_EXPORT_DOC, StandardCharsets.UTF_8);

        assertThat(analyticsDoc)
                .contains("kpi-definitions.md")
                .contains("KPI Definition Document");
        assertThat(reportDoc)
                .contains("kpi-definitions.md")
                .contains("KPI Definition Document");
    }
}
