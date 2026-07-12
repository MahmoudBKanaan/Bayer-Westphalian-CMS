package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.analytics.CampaignAnalyticsView;
import com.bayerwestphalian.campaign.analytics.CampaignMetricsView;
import com.bayerwestphalian.campaign.audit.AuditLogView;
import com.bayerwestphalian.campaign.campaign.CampaignChannel;
import com.bayerwestphalian.campaign.campaign.CampaignStatus;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * KB item 436–438: CSV/PDF document builders used by ReportService (item 437 / item 455 CSV /
 * FR-109; item 438 / item 456 PDF / FR-110).
 *
 * <p>Acceptance coverage for campaign CSV export is also formalized under KB item 455 in {@link
 * CsvExportWorksTests}. Acceptance coverage for campaign PDF export is formalized under KB item 456
 * in {@link PdfExportWorksTests}.
 */
class CampaignReportDocumentTests {

    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000436");
    private static final UUID OWNER_ID = UUID.fromString("10000000-0000-0000-0000-000000000436");

    @Test
    void campaignCsvIncludesHeaderAndMetricRow() {
        byte[] bytes = CampaignReportDocument.campaignCsv(sampleAnalytics());
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).startsWith("campaignId,campaignName");
        assertThat(csv).contains(CAMPAIGN_ID.toString());
        assertThat(csv).contains("Spring Life Drive");
        assertThat(csv).contains("0.5000");
        assertThat(csv).contains("150.00");
    }

    @Test
    void campaignCsvEscapesCommasAndQuotes() {
        CampaignAnalyticsView analytics =
                new CampaignAnalyticsView(
                        CAMPAIGN_ID,
                        "Life, \"Premium\" Plan",
                        "Win, retain",
                        CampaignStatus.ACTIVE,
                        CampaignChannel.EMAIL,
                        null,
                        null,
                        OWNER_ID,
                        "Ada, Lovelace",
                        null,
                        Instant.parse("2026-07-11T12:00:00Z"));

        String csv =
                new String(CampaignReportDocument.campaignCsv(analytics), StandardCharsets.UTF_8);

        assertThat(csv).contains("\"Life, \"\"Premium\"\" Plan\"");
        assertThat(csv).contains("\"Win, retain\"");
        assertThat(csv).contains("\"Ada, Lovelace\"");
    }

    @Test
    void campaignPdfIsMinimalValidPdfWithCampaignName() {
        // KB item 438 / FR-110: PDF carries the same KPI narrative as the CSV export.
        byte[] bytes = CampaignReportDocument.campaignPdf(sampleAnalytics());
        String pdf = new String(bytes, StandardCharsets.US_ASCII);

        assertThat(pdf).startsWith("%PDF-1.4");
        assertThat(pdf).contains("Bayer-Westphalian Campaign Report");
        assertThat(pdf).contains("Spring Life Drive");
        assertThat(pdf).contains(CAMPAIGN_ID.toString());
        assertThat(pdf).contains("Audience size");
        assertThat(pdf).contains("Open rate");
        assertThat(pdf).contains("Click rate");
        assertThat(pdf).contains("Conversion rate");
        assertThat(pdf).contains("Estimated ROI");
        assertThat(pdf).contains("%%EOF");
    }

    @Test
    void campaignFilenameSanitizesNameAndAppliesExtension() {
        assertThat(
                        CampaignReportDocument.campaignFilename(
                                sampleAnalytics(), ReportExportType.CSV))
                .isEqualTo("Spring-Life-Drive.csv");
        assertThat(
                        CampaignReportDocument.campaignFilename(
                                sampleAnalytics(), ReportExportType.PDF))
                .isEqualTo("Spring-Life-Drive.pdf");
    }

    @Test
    void auditCsvIncludesLogRows() {
        AuditLogView log =
                new AuditLogView(
                        UUID.fromString("57000000-0000-0000-0000-000000000436"),
                        OWNER_ID,
                        "APPROVE",
                        "campaigns",
                        CAMPAIGN_ID,
                        null,
                        null,
                        null,
                        Instant.parse("2026-07-11T09:00:00Z"));

        String csv =
                new String(CampaignReportDocument.auditCsv(List.of(log)), StandardCharsets.UTF_8);

        assertThat(csv).contains("action,entityType");
        assertThat(csv).contains("APPROVE");
        assertThat(csv).contains(CAMPAIGN_ID.toString());
    }

    private static CampaignAnalyticsView sampleAnalytics() {
        CampaignMetricsView metrics =
                new CampaignMetricsView(
                        null,
                        CAMPAIGN_ID,
                        "Spring Life Drive",
                        CampaignStatus.ACTIVE,
                        100,
                        80,
                        20,
                        80,
                        40,
                        16,
                        8,
                        4,
                        new BigDecimal("0.5000"),
                        new BigDecimal("0.2000"),
                        new BigDecimal("0.0500"),
                        new BigDecimal("100.00"),
                        new BigDecimal("150.00"),
                        new BigDecimal("0.50"),
                        Instant.parse("2026-07-11T10:00:00Z"));
        return new CampaignAnalyticsView(
                CAMPAIGN_ID,
                "Spring Life Drive",
                "Raise awareness",
                CampaignStatus.ACTIVE,
                CampaignChannel.EMAIL,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 1),
                OWNER_ID,
                "Report User",
                metrics,
                Instant.parse("2026-07-11T12:00:00Z"));
    }
}
