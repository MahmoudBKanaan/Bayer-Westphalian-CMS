package com.bayerwestphalian.campaign.report;

import com.bayerwestphalian.campaign.analytics.CampaignAnalyticsView;
import com.bayerwestphalian.campaign.analytics.CampaignMetricsView;
import com.bayerwestphalian.campaign.audit.AuditLogView;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds campaign and audit report document bytes for {@link ReportService} (KB items 436–438; CSV
 * FR-109 / item 455 / PDF FR-110 / item 456).
 *
 * <p>Item 437 / item 455 campaign CSV: single header row plus one metrics data row (identity + KPI
 * columns). Item 438 / item 456 campaign PDF: minimal single-page PDF 1.4 document (no external PDF
 * library) with the same KPI lines.
 *
 * <p>Item 466 / COMP-010: campaign CSV/PDF serialize <em>aggregated</em> {@link
 * CampaignMetricsView} KPIs only — not raw campaign recipient or contact-event rows. Those
 * aggregates remain traceable to launch recipient tallies and BR-034 contact-event updates via
 * {@code campaign_metrics}.
 */
final class CampaignReportDocument {

    private CampaignReportDocument() {}

    /**
     * Campaign performance CSV bytes (KB item 437 / item 455 / FR-109).
     *
     * <p>Columns: campaign identity, audience/engagement counts, open/click/conversion rates, and
     * estimated cost/revenue/ROI. Used by {@link ReportService#exportCampaignCsv}.
     */
    static byte[] campaignCsv(CampaignAnalyticsView analytics) {
        Objects.requireNonNull(analytics, "analytics is required");
        CampaignMetricsView metrics = analytics.metrics();

        StringBuilder csv = new StringBuilder();
        csv.append(
                String.join(
                        ",",
                        "campaignId",
                        "campaignName",
                        "objective",
                        "status",
                        "channel",
                        "startDate",
                        "endDate",
                        "ownerUserId",
                        "ownerFullName",
                        "audienceSize",
                        "eligibleCount",
                        "excludedCount",
                        "sentCount",
                        "openedCount",
                        "clickedCount",
                        "repliedCount",
                        "convertedCount",
                        "openRate",
                        "clickRate",
                        "conversionRate",
                        "estimatedCost",
                        "estimatedRevenue",
                        "estimatedRoi",
                        "generatedAt"));
        csv.append('\n');
        csv.append(
                String.join(
                        ",",
                        csv(analytics.campaignId()),
                        csv(analytics.campaignName()),
                        csv(analytics.objective()),
                        csv(analytics.status()),
                        csv(analytics.channel()),
                        csv(analytics.startDate()),
                        csv(analytics.endDate()),
                        csv(analytics.ownerUserId()),
                        csv(analytics.ownerFullName()),
                        csv(metrics == null ? null : metrics.audienceSize()),
                        csv(metrics == null ? null : metrics.eligibleCount()),
                        csv(metrics == null ? null : metrics.excludedCount()),
                        csv(metrics == null ? null : metrics.sentCount()),
                        csv(metrics == null ? null : metrics.openedCount()),
                        csv(metrics == null ? null : metrics.clickedCount()),
                        csv(metrics == null ? null : metrics.repliedCount()),
                        csv(metrics == null ? null : metrics.convertedCount()),
                        csv(metrics == null ? null : metrics.openRate()),
                        csv(metrics == null ? null : metrics.clickRate()),
                        csv(metrics == null ? null : metrics.conversionRate()),
                        csv(metrics == null ? null : metrics.estimatedCost()),
                        csv(metrics == null ? null : metrics.estimatedRevenue()),
                        csv(metrics == null ? null : metrics.estimatedRoi()),
                        csv(analytics.generatedAt())));
        csv.append('\n');
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Campaign performance PDF bytes (KB item 438 / item 456 / FR-110).
     *
     * <p>Minimal PDF 1.4 document with campaign identity and KPI lines. Used by {@link
     * ReportService#generateCampaignPdf}.
     */
    static byte[] campaignPdf(CampaignAnalyticsView analytics) {
        Objects.requireNonNull(analytics, "analytics is required");
        CampaignMetricsView metrics = analytics.metrics();
        List<String> lines = new ArrayList<>();
        lines.add("Bayer-Westphalian Campaign Report");
        lines.add("Campaign: " + nullToEmpty(analytics.campaignName()));
        lines.add("Campaign ID: " + nullToEmpty(analytics.campaignId()));
        lines.add("Objective: " + nullToEmpty(analytics.objective()));
        lines.add("Status: " + nullToEmpty(analytics.status()));
        lines.add("Channel: " + nullToEmpty(analytics.channel()));
        lines.add(
                "Schedule: "
                        + nullToEmpty(analytics.startDate())
                        + " - "
                        + nullToEmpty(analytics.endDate()));
        lines.add(
                "Owner: "
                        + nullToEmpty(analytics.ownerFullName())
                        + " ("
                        + nullToEmpty(analytics.ownerUserId())
                        + ")");
        if (metrics == null) {
            lines.add("Metrics: (none recorded yet)");
        } else {
            lines.add("Audience size: " + metrics.audienceSize());
            lines.add("Eligible: " + metrics.eligibleCount());
            lines.add("Excluded: " + metrics.excludedCount());
            lines.add("Sent: " + metrics.sentCount());
            lines.add("Opened: " + metrics.openedCount());
            lines.add("Clicked: " + metrics.clickedCount());
            lines.add("Replied: " + metrics.repliedCount());
            lines.add("Converted: " + metrics.convertedCount());
            lines.add("Open rate: " + nullToEmpty(metrics.openRate()));
            lines.add("Click rate: " + nullToEmpty(metrics.clickRate()));
            lines.add("Conversion rate: " + nullToEmpty(metrics.conversionRate()));
            lines.add("Estimated cost: " + nullToEmpty(metrics.estimatedCost()));
            lines.add("Estimated revenue: " + nullToEmpty(metrics.estimatedRevenue()));
            lines.add("Estimated ROI: " + nullToEmpty(metrics.estimatedRoi()));
        }
        lines.add("Generated at: " + nullToEmpty(analytics.generatedAt()));
        return SimplePdfDocument.of(lines);
    }

    static byte[] auditCsv(List<AuditLogView> logs) {
        Objects.requireNonNull(logs, "logs is required");
        StringBuilder csv = new StringBuilder();
        csv.append(
                String.join(
                        ",",
                        "id",
                        "actorUserId",
                        "action",
                        "entityType",
                        "entityId",
                        "ipAddress",
                        "createdAt"));
        csv.append('\n');
        for (AuditLogView log : logs) {
            csv.append(
                    String.join(
                            ",",
                            csv(log.id()),
                            csv(log.actorUserId()),
                            csv(log.action()),
                            csv(log.entityType()),
                            csv(log.entityId()),
                            csv(log.ipAddress()),
                            csv(log.createdAt())));
            csv.append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    static byte[] auditPdf(List<AuditLogView> logs) {
        Objects.requireNonNull(logs, "logs is required");
        List<String> lines = new ArrayList<>();
        lines.add("Bayer-Westphalian Audit Report");
        lines.add("Entries: " + logs.size());
        lines.add("Generated at: " + Instant.now());
        int limit = Math.min(logs.size(), 40);
        for (int i = 0; i < limit; i++) {
            AuditLogView log = logs.get(i);
            lines.add(
                    nullToEmpty(log.createdAt())
                            + " | "
                            + nullToEmpty(log.action())
                            + " | "
                            + nullToEmpty(log.entityType())
                            + " | "
                            + nullToEmpty(log.entityId()));
        }
        if (logs.size() > limit) {
            lines.add("... " + (logs.size() - limit) + " additional entries omitted");
        }
        return SimplePdfDocument.of(lines);
    }

    static String campaignFilename(CampaignAnalyticsView analytics, ReportExportType type) {
        String base = sanitizeFilename(analytics.campaignName());
        if (base.isEmpty()) {
            base = "campaign-" + analytics.campaignId();
        }
        return base + (type == ReportExportType.PDF ? ".pdf" : ".csv");
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.trim()
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text =
                value instanceof BigDecimal decimal
                        ? decimal.toPlainString()
                        : String.valueOf(value);
        if (text.indexOf(',') >= 0
                || text.indexOf('"') >= 0
                || text.indexOf('\n') >= 0
                || text.indexOf('\r') >= 0) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }

    private static String nullToEmpty(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        return String.valueOf(value);
    }

    /**
     * Minimal PDF 1.4 writer for multi-line plain text (no third-party PDF dependency).
     */
    static final class SimplePdfDocument {

        private SimplePdfDocument() {}

        static byte[] of(List<String> lines) {
            Objects.requireNonNull(lines, "lines is required");
            StringBuilder content = new StringBuilder("BT /F1 10 Tf 14 TL 50 780 Td ");
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) {
                    content.append("T* ");
                }
                content.append('(').append(escapePdfText(lines.get(i))).append(") Tj ");
            }
            content.append("ET");
            String contentStream = content.toString();
            byte[] contentBytes = contentStream.getBytes(StandardCharsets.US_ASCII);

            String header = "%PDF-1.4\n";
            String[] objectBlocks = {
                "1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n",
                "2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n",
                "3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R"
                        + " /Resources << /Font << /F1 5 0 R >> >> >>endobj\n",
                "4 0 obj<< /Length "
                        + contentBytes.length
                        + " >>stream\n"
                        + contentStream
                        + "\nendstream\nendobj\n",
                "5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj\n"
            };

            StringBuilder body = new StringBuilder();
            int[] offsets = new int[6];
            int position = header.getBytes(StandardCharsets.US_ASCII).length;
            for (int i = 0; i < objectBlocks.length; i++) {
                offsets[i + 1] = position;
                body.append(objectBlocks[i]);
                position += objectBlocks[i].getBytes(StandardCharsets.US_ASCII).length;
            }
            int xrefOffset = position;

            StringBuilder xref = new StringBuilder("xref\n0 6\n");
            xref.append("0000000000 65535 f \n");
            for (int i = 1; i <= 5; i++) {
                xref.append(String.format("%010d 00000 n \n", offsets[i]));
            }
            String trailer =
                    "trailer<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n";
            return (header + body + xref + trailer).getBytes(StandardCharsets.US_ASCII);
        }

        private static String escapePdfText(String line) {
            if (line == null) {
                return "";
            }
            return line.replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("\r", " ")
                    .replace("\n", " ");
        }
    }
}
