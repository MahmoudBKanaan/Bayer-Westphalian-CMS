package com.bayerwestphalian.campaign.report;

/**
 * Report file format for export requests (KB {@code report_export_type} / FR-109–FR-110).
 *
 * <p>Maps PostgreSQL enum {@code report_export_type}: {@code CSV}, {@code PDF}.
 */
public enum ReportExportType {
    /** Tabular campaign or compliance export (FR-109). */
    CSV,

    /** Document-style campaign or management report (FR-110). */
    PDF
}
