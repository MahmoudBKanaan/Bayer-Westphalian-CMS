package com.bayerwestphalian.campaign.report;

/**
 * Lifecycle status of a report export request (KB {@code report_export_status}).
 *
 * <p>Maps PostgreSQL enum {@code report_export_status}: {@code REQUESTED}, {@code COMPLETED}, {@code
 * FAILED}.
 */
public enum ReportExportStatus {
    /** Export was requested and is pending generation. */
    REQUESTED,

    /** Export finished successfully; {@code file_url} and {@code completed_at} are set. */
    COMPLETED,

    /** Export generation failed. */
    FAILED
}
