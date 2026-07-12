package com.bayerwestphalian.campaign.report;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * API / service view of a stored {@link ReportExport} history row (KB item 439 / table {@code
 * report_exports}).
 *
 * <p>Returned by {@code GET /api/reports/exports} and {@code GET /api/reports/exports/{exportId}},
 * and embedded on {@link ReportFile} after each CSV/PDF/audit export.
 */
public record ReportExportView(
        UUID id,
        UUID requestedByUserId,
        String reportName,
        ReportExportType exportType,
        ReportExportStatus status,
        String fileUrl,
        Instant requestedAt,
        Instant completedAt) {

    public static ReportExportView from(ReportExport export) {
        Objects.requireNonNull(export, "export is required");
        return new ReportExportView(
                export.getId(),
                export.getRequestedByUserId(),
                export.getReportName(),
                export.getExportType(),
                export.getStatus(),
                export.getFileUrl(),
                export.getRequestedAt(),
                export.getCompletedAt());
    }
}
