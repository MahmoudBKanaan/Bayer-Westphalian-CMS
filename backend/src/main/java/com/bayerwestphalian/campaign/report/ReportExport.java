package com.bayerwestphalian.campaign.report;

import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Report export request and history row (KB entity {@code ReportExport} / table {@code
 * report_exports} / item 435).
 *
 * <p>Tracks who requested a CSV or PDF export (FR-109–FR-110), the export status, optional download
 * location, and completion time. ReportService persists each export lifecycle to this table (item
 * 439 store report export history).
 *
 * <p>Factory: {@link #request(User, String, ReportExportType)}. State transitions: {@link
 * #markCompleted(String)}, {@link #markFailed()}.
 */
@Entity
@Table(name = "report_exports")
public class ReportExport {

    private static final int REPORT_NAME_MAX_LENGTH = 255;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Optional requester; set null when the user is deleted (ON DELETE SET NULL). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @NotBlank
    @Size(max = REPORT_NAME_MAX_LENGTH)
    @Column(name = "report_name", nullable = false, length = REPORT_NAME_MAX_LENGTH)
    private String reportName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "export_type", nullable = false, columnDefinition = "report_export_type")
    private ReportExportType exportType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "report_export_status")
    private ReportExportStatus status = ReportExportStatus.REQUESTED;

    @Column(name = "file_url", columnDefinition = "text")
    private String fileUrl;

    @NotNull
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ReportExport() {}

    private ReportExport(User requestedBy, String reportName, ReportExportType exportType) {
        this.requestedBy = requestedBy;
        this.reportName = normalizeReportName(reportName);
        this.exportType = Objects.requireNonNull(exportType, "Export type is required");
        this.status = ReportExportStatus.REQUESTED;
    }

    /**
     * Creates a new export request in {@link ReportExportStatus#REQUESTED} state (KB item 435).
     *
     * @param requestedBy optional user who requested the export (may be {@code null})
     * @param reportName human-readable report label (required, max 255)
     * @param exportType {@link ReportExportType#CSV} or {@link ReportExportType#PDF}
     */
    public static ReportExport request(
            User requestedBy, String reportName, ReportExportType exportType) {
        return new ReportExport(requestedBy, reportName, exportType);
    }

    /**
     * Creates a new export request without a linked requester (system or anonymous history row).
     */
    public static ReportExport request(String reportName, ReportExportType exportType) {
        return request(null, reportName, exportType);
    }

    /**
     * Marks the export successfully completed with a storage location (item 435 / export history).
     *
     * <p>Sets status {@link ReportExportStatus#COMPLETED}, stores {@code fileUrl}, and stamps {@code
     * completedAt}.
     */
    public void markCompleted(String fileUrl) {
        String normalized = normalizeFileUrl(fileUrl);
        this.status = ReportExportStatus.COMPLETED;
        this.fileUrl = normalized;
        this.completedAt = Instant.now();
    }

    /**
     * Marks the export as failed (item 435).
     *
     * <p>Sets status {@link ReportExportStatus#FAILED} and clears completion fields so failed rows
     * do not look downloadable.
     */
    public void markFailed() {
        this.status = ReportExportStatus.FAILED;
        this.fileUrl = null;
        this.completedAt = null;
    }

    public boolean isRequested() {
        return status == ReportExportStatus.REQUESTED;
    }

    public boolean isCompleted() {
        return status == ReportExportStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == ReportExportStatus.FAILED;
    }

    public UUID getId() {
        return id;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public UUID getRequestedByUserId() {
        return requestedBy == null ? null : requestedBy.getId();
    }

    public String getReportName() {
        return reportName;
    }

    public ReportExportType getExportType() {
        return exportType;
    }

    public ReportExportStatus getStatus() {
        return status;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
        if (status == null) {
            status = ReportExportStatus.REQUESTED;
        }
    }

    private static String normalizeReportName(String reportName) {
        Objects.requireNonNull(reportName, "Report name is required");
        String trimmed = reportName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Report name is required");
        }
        if (trimmed.length() > REPORT_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Report name must not exceed " + REPORT_NAME_MAX_LENGTH + " characters");
        }
        return trimmed;
    }

    private static String normalizeFileUrl(String fileUrl) {
        Objects.requireNonNull(fileUrl, "File URL is required");
        String trimmed = fileUrl.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("File URL is required");
        }
        return trimmed;
    }
}
