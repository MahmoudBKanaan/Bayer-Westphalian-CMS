package com.bayerwestphalian.campaign.report;

import com.bayerwestphalian.campaign.analytics.AnalyticsService;
import com.bayerwestphalian.campaign.analytics.CampaignAnalyticsView;
import com.bayerwestphalian.campaign.audit.AuditLogView;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.campaign.Campaign;
import com.bayerwestphalian.campaign.campaign.CampaignRepository;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Report generation and export orchestration (KB {@code ReportService} / item 436).
 *
 * <p>KB constructor dependencies: AnalyticsService, CampaignRepository. Also uses {@link
 * ReportExportRepository} for export history, {@link UserRepository} for requester linkage, and
 * {@link AuditService} for audit report content and item 531 export audit trail.
 *
 * <p>KB methods: {@link #exportCampaignCsv}, {@link #generateCampaignPdf}, {@link
 * #exportAuditReport}; controller aliases {@link #campaignCsv}, {@link #campaignPdf}, {@link
 * #auditReport}. Item 439 history: {@link #listExportHistory}, {@link
 * #listExportHistoryForUser}, {@link #listExportHistoryByStatus}, {@link #getExportHistory}.
 *
 * <p>Item 466: campaign exports load aggregates through {@link AnalyticsService} (COMP-010) so
 * report bytes stay reconcilable to {@code campaign_metrics} derived from recipients and contact
 * events (BR-034), without embedding raw event/recipient dumps.
 *
 * <p>Item 531: each successful campaign CSV/PDF or audit history export also writes an {@code
 * EXPORT_REPORT} row on entity type {@code report_exports} via {@link
 * AuditService#logReportExport}.
 */
@Service
@Transactional
public class ReportService {

    /** Campaign report export roles (FR-109–FR-110 / item 458 / Sprint 16 critical item 663). */
    private static final String REPORT_READ =
            "@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER', "
                    + "'MARKETING_ANALYST', 'EXECUTIVE_VIEWER')";

    /** Audit history export roles (restricted separately from campaign reports / item 458). */
    private static final String AUDIT_REPORT_READ =
            "@authz.hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'SYSTEM_AUDITOR')";

    private final AnalyticsService analyticsService;
    private final CampaignRepository campaignRepository;
    private final ReportExportRepository reportExportRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ReportService(
            AnalyticsService analyticsService,
            CampaignRepository campaignRepository,
            ReportExportRepository reportExportRepository,
            UserRepository userRepository,
            AuditService auditService) {
        this.analyticsService = analyticsService;
        this.campaignRepository = campaignRepository;
        this.reportExportRepository = reportExportRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Campaign performance CSV export (KB {@code exportCampaignCsv} / FR-109 / item 437 / item 455).
     *
     * <p>Loads campaign analytics KPIs, builds a CSV document ({@link CampaignReportDocument}),
     * records a completed {@link ReportExport} history row, and writes item 531 {@code
     * EXPORT_REPORT} audit evidence. Exposed at {@code GET
     * /api/reports/campaigns/{campaignId}/csv} via {@link ReportController#campaignCsv}.
     *
     * @param campaignId campaign to export
     * @param requestedByUserId optional authenticated requester (may be {@code null})
     */
    @PreAuthorize(REPORT_READ)
    public ReportFile exportCampaignCsv(UUID campaignId, UUID requestedByUserId) {
        return exportCampaignReport(campaignId, requestedByUserId, ReportExportType.CSV);
    }

    /**
     * Campaign performance PDF export (KB {@code generateCampaignPdf} / FR-110 / item 438 / item
     * 456).
     *
     * <p>Loads campaign analytics KPIs, builds a minimal PDF document ({@link
     * CampaignReportDocument}), records a completed {@link ReportExport} history row, and writes
     * item 531 {@code EXPORT_REPORT} audit evidence. Exposed at {@code GET
     * /api/reports/campaigns/{campaignId}/pdf} via {@link ReportController#campaignPdf}.
     *
     * @param campaignId campaign to export
     * @param requestedByUserId optional authenticated requester (may be {@code null})
     */
    @PreAuthorize(REPORT_READ)
    public ReportFile generateCampaignPdf(UUID campaignId, UUID requestedByUserId) {
        return exportCampaignReport(campaignId, requestedByUserId, ReportExportType.PDF);
    }

    /**
     * Audit log export as CSV (KB {@code exportAuditReport} / item 436).
     *
     * <p>Restricted to Admin, Compliance Officer, and System Auditor. Stores export history (item
     * 439) the same way as campaign exports and writes item 531 {@code EXPORT_REPORT} audit
     * evidence on success.
     */
    @PreAuthorize(AUDIT_REPORT_READ)
    public ReportFile exportAuditReport(UUID requestedByUserId) {
        User requester = resolveRequester(requestedByUserId);
        ReportExport export =
                storeExportRequest(requester, "Audit history export", ReportExportType.CSV);

        try {
            List<AuditLogView> logs = auditService.listAuditLogs();
            byte[] content = CampaignReportDocument.auditCsv(logs);
            String fileUrl = "local://reports/" + export.getId() + "/audit-history.csv";
            export = storeExportCompleted(export, fileUrl);
            // Item 531: successful audit-history export is itself an auditable sensitive action.
            auditReportExportCompleted(requestedByUserId, export, null);
            return new ReportFile(
                    "audit-history.csv",
                    ReportFile.CSV_CONTENT_TYPE,
                    content,
                    ReportExportView.from(export));
        } catch (RuntimeException ex) {
            storeExportFailed(export);
            throw ex;
        }
    }

    /**
     * KB controller alias for {@link #exportCampaignCsv} (item 437 / item 455 / FR-109).
     *
     * <p>Used by {@link ReportController} for {@code GET /api/reports/campaigns/{campaignId}/csv}.
     */
    @PreAuthorize(REPORT_READ)
    public ReportFile campaignCsv(UUID campaignId, UUID requestedByUserId) {
        return exportCampaignCsv(campaignId, requestedByUserId);
    }

    /**
     * KB controller alias for {@link #generateCampaignPdf} (item 438 / item 456 / FR-110).
     *
     * <p>Used by {@link ReportController} for {@code GET /api/reports/campaigns/{campaignId}/pdf}.
     */
    @PreAuthorize(REPORT_READ)
    public ReportFile campaignPdf(UUID campaignId, UUID requestedByUserId) {
        return generateCampaignPdf(campaignId, requestedByUserId);
    }

    /** KB controller alias for {@link #exportAuditReport}. */
    @PreAuthorize(AUDIT_REPORT_READ)
    public ReportFile auditReport(UUID requestedByUserId) {
        return exportAuditReport(requestedByUserId);
    }

    /**
     * Lists all stored export history rows, newest first (KB item 439).
     *
     * <p>Each campaign CSV/PDF and audit export persists a {@link ReportExport} row in {@code
     * report_exports} with requester, type, status, file URL, and timestamps.
     */
    @PreAuthorize(REPORT_READ)
    @Transactional(readOnly = true)
    public List<ReportExportView> listExportHistory() {
        return reportExportRepository.findAllByOrderByRequestedAtDesc().stream()
                .map(ReportExportView::from)
                .toList();
    }

    /**
     * Lists export history for a specific requester (KB item 439).
     */
    @PreAuthorize(REPORT_READ)
    @Transactional(readOnly = true)
    public List<ReportExportView> listExportHistoryForUser(UUID requestedByUserId) {
        if (requestedByUserId == null) {
            throw new ValidationException(
                    "Report export history validation failed: requestedByUserId is required",
                    List.of("requestedByUserId: is required"));
        }
        return reportExportRepository.findByRequestedByUserId(requestedByUserId).stream()
                .map(ReportExportView::from)
                .toList();
    }

    /**
     * Lists export history filtered by lifecycle status (KB item 439).
     */
    @PreAuthorize(REPORT_READ)
    @Transactional(readOnly = true)
    public List<ReportExportView> listExportHistoryByStatus(ReportExportStatus status) {
        if (status == null) {
            throw new ValidationException(
                    "Report export history validation failed: status is required",
                    List.of("status: is required"));
        }
        return reportExportRepository.findByStatusOrderByRequestedAtDesc(status).stream()
                .map(ReportExportView::from)
                .toList();
    }

    /**
     * Loads a single stored export history row by id (KB item 439).
     *
     * @throws ResourceNotFoundException when no {@code report_exports} row exists for {@code
     *     exportId}
     */
    @PreAuthorize(REPORT_READ)
    @Transactional(readOnly = true)
    public ReportExportView getExportHistory(UUID exportId) {
        if (exportId == null) {
            throw new ValidationException(
                    "Report export history validation failed: exportId is required",
                    List.of("exportId: is required"));
        }
        ReportExport export =
                reportExportRepository
                        .findById(exportId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("ReportExport", exportId));
        return ReportExportView.from(export);
    }

    private ReportFile exportCampaignReport(
            UUID campaignId, UUID requestedByUserId, ReportExportType exportType) {
        if (campaignId == null) {
            throw new ValidationException(
                    "Campaign report validation failed", List.of("campaignId: is required"));
        }

        // Ensure campaign exists before creating an export history row.
        Campaign campaign =
                campaignRepository
                        .findById(campaignId)
                        .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        User requester = resolveRequester(requestedByUserId);
        String reportName =
                (exportType == ReportExportType.PDF ? "Campaign PDF: " : "Campaign CSV: ")
                        + campaign.getName();
        // KB item 439: store REQUESTED history row before generation.
        ReportExport export = storeExportRequest(requester, reportName, exportType);

        try {
            // Aggregate KPIs via AnalyticsService (KB constructor dependency).
            CampaignAnalyticsView analytics = analyticsService.getCampaignAnalytics(campaignId);
            byte[] content =
                    exportType == ReportExportType.PDF
                            ? CampaignReportDocument.campaignPdf(analytics)
                            : CampaignReportDocument.campaignCsv(analytics);
            String filename = CampaignReportDocument.campaignFilename(analytics, exportType);
            String fileUrl = "local://reports/" + export.getId() + "/" + filename;
            // KB item 439: update history to COMPLETED with file reference.
            export = storeExportCompleted(export, fileUrl);
            // Item 531: log successful campaign report export for compliance review.
            auditReportExportCompleted(requestedByUserId, export, campaignId);

            String contentType =
                    exportType == ReportExportType.PDF
                            ? ReportFile.PDF_CONTENT_TYPE
                            : ReportFile.CSV_CONTENT_TYPE;
            return new ReportFile(filename, contentType, content, ReportExportView.from(export));
        } catch (RuntimeException ex) {
            // KB item 439: retain FAILED history row for auditability.
            // Failed exports do not emit EXPORT_REPORT (item 531 is success-path only).
            storeExportFailed(export);
            throw ex;
        }
    }

    /**
     * Item 531: immutable {@code EXPORT_REPORT} audit row for a completed export history record.
     *
     * <p>Entity type {@code report_exports}; actor is the report requester when known.
     */
    private void auditReportExportCompleted(
            UUID actorUserId, ReportExport export, UUID campaignId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (export.getId() != null) {
            payload.put("id", export.getId().toString());
        }
        payload.put("reportName", export.getReportName());
        payload.put("exportType", export.getExportType().name());
        payload.put("status", export.getStatus().name());
        if (campaignId != null) {
            payload.put("campaignId", campaignId.toString());
        }
        if (export.getFileUrl() != null) {
            payload.put("fileUrl", export.getFileUrl());
        }
        if (actorUserId != null) {
            payload.put("requestedByUserId", actorUserId.toString());
        }
        auditService.logReportExport(actorUserId, export.getId(), payload);
    }

    /**
     * Persists a new {@link ReportExportStatus#REQUESTED} history row (KB item 439).
     */
    private ReportExport storeExportRequest(
            User requester, String reportName, ReportExportType exportType) {
        ReportExport export = ReportExport.request(requester, reportName, exportType);
        return reportExportRepository.save(export);
    }

    /**
     * Marks and persists a completed export history row with file location (KB item 439).
     */
    private ReportExport storeExportCompleted(ReportExport export, String fileUrl) {
        export.markCompleted(fileUrl);
        return reportExportRepository.save(export);
    }

    /**
     * Marks and persists a failed export history row (KB item 439).
     */
    private void storeExportFailed(ReportExport export) {
        export.markFailed();
        reportExportRepository.save(export);
    }

    private User resolveRequester(UUID requestedByUserId) {
        if (requestedByUserId == null) {
            return null;
        }
        return userRepository.findById(requestedByUserId).orElse(null);
    }
}
