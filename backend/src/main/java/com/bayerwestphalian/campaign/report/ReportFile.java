package com.bayerwestphalian.campaign.report;

import java.util.Arrays;
import java.util.Objects;

/**
 * Generated report file payload returned by {@link ReportService} export methods (KB item 436 /
 * FR-109–FR-110 / item 455 CSV).
 *
 * <p>Carries downloadable bytes plus the persisted {@link ReportExportView} history row. Campaign
 * CSV exports use {@link #CSV_CONTENT_TYPE} ({@code text/csv; charset=UTF-8}).
 */
public record ReportFile(
        String filename, String contentType, byte[] content, ReportExportView export) {

    public static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";
    public static final String PDF_CONTENT_TYPE = "application/pdf";

    public ReportFile {
        Objects.requireNonNull(filename, "filename is required");
        Objects.requireNonNull(contentType, "contentType is required");
        Objects.requireNonNull(content, "content is required");
        Objects.requireNonNull(export, "export is required");
        content = Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }

    public int contentLength() {
        return content.length;
    }
}
