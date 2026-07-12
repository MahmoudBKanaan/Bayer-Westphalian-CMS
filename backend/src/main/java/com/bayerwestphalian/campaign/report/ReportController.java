package com.bayerwestphalian.campaign.report;

import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.api.ApiResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Report export REST API (KB ReportController / items 437–439).
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>{@code GET /api/reports/campaigns/{campaignId}/csv} — campaign CSV export (item 437 /
 *       item 455 / FR-109)
 *   <li>{@code GET /api/reports/campaigns/{campaignId}/pdf} — campaign PDF export (item 438 /
 *       FR-110)
 *   <li>{@code GET /api/reports/exports} — stored export history (item 439)
 *   <li>{@code GET /api/reports/exports/{exportId}} — single export history row (item 439)
 * </ul>
 *
 * <p>Access matches {@code SecurityConfiguration} {@code /api/reports/**}: Admin, BI Analyst,
 * Campaign Manager, Marketing Analyst, Executive Viewer. Acceptance item 458 / Sprint 16 critical
 * item 663: unauthorized users cannot export restricted reports (401 unauthenticated / 403 wrong
 * role).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final String REPORT_READ =
            "@authz.hasAnyRole('ADMIN', 'BI_ANALYST', 'CAMPAIGN_MANAGER', "
                    + "'MARKETING_ANALYST', 'EXECUTIVE_VIEWER')";

    private final ReportService reportService;
    private final AuthorizationExpressions authorizationExpressions;

    public ReportController(
            ReportService reportService, AuthorizationExpressions authorizationExpressions) {
        this.reportService = reportService;
        this.authorizationExpressions = authorizationExpressions;
    }

    /**
     * Campaign performance CSV download (KB item 437 / item 455 / FR-109 / {@code GET
     * /api/reports/campaigns/{campaignId}/csv}).
     *
     * <p>Returns a {@code text/csv} attachment built from campaign identity and KPI metrics
     * (audience, sent, open/click/conversion rates, cost/revenue/ROI). Persists a {@link
     * ReportExport} history row via {@link ReportService#campaignCsv}. Responds {@code 404} when
     * the campaign does not exist. Acceptance item 455: CSV export works end-to-end.
     */
    @GetMapping(value = "/campaigns/{campaignId}/csv", produces = "text/csv")
    @PreAuthorize(REPORT_READ)
    public ResponseEntity<byte[]> campaignCsv(@PathVariable UUID campaignId) {
        UUID requesterId = authorizationExpressions.currentUserId();
        ReportFile file = reportService.campaignCsv(campaignId, requesterId);
        return toAttachmentResponse(file);
    }

    /**
     * Campaign performance PDF download (KB item 438 / FR-110 / {@code GET
     * /api/reports/campaigns/{campaignId}/pdf}).
     *
     * <p>Returns an {@code application/pdf} attachment with campaign identity and KPI metrics
     * (audience, sent, open/click/conversion rates, cost/revenue/ROI). Persists a {@link
     * ReportExport} history row via {@link ReportService#campaignPdf}. Responds {@code 404} when
     * the campaign does not exist.
     */
    @GetMapping(value = "/campaigns/{campaignId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize(REPORT_READ)
    public ResponseEntity<byte[]> campaignPdf(@PathVariable UUID campaignId) {
        UUID requesterId = authorizationExpressions.currentUserId();
        ReportFile file = reportService.campaignPdf(campaignId, requesterId);
        return toAttachmentResponse(file);
    }

    /**
     * Stored report export history (KB item 439 / {@code GET /api/reports/exports}).
     *
     * <p>Returns {@link ReportExportView} rows from {@code report_exports}, newest first. Pass
     * {@code mine=true} to limit results to the authenticated requester. Optional {@code status}
     * filters by {@link ReportExportStatus} (ignored when {@code mine=true}).
     */
    @GetMapping("/exports")
    @PreAuthorize(REPORT_READ)
    public ResponseEntity<ApiResponse<List<ReportExportView>>> listExportHistory(
            @RequestParam(name = "mine", defaultValue = "false") boolean mine,
            @RequestParam(name = "status", required = false) ReportExportStatus status) {
        List<ReportExportView> history;
        if (mine) {
            history =
                    reportService.listExportHistoryForUser(
                            authorizationExpressions.currentUserId());
        } else if (status != null) {
            history = reportService.listExportHistoryByStatus(status);
        } else {
            history = reportService.listExportHistory();
        }
        return ResponseEntity.ok(ApiResponse.success("Report export history loaded", history));
    }

    /**
     * Single stored export history row (KB item 439 / {@code GET
     * /api/reports/exports/{exportId}}).
     *
     * <p>Responds {@code 404} when the export id is unknown.
     */
    @GetMapping("/exports/{exportId}")
    @PreAuthorize(REPORT_READ)
    public ResponseEntity<ApiResponse<ReportExportView>> getExportHistory(
            @PathVariable UUID exportId) {
        ReportExportView export = reportService.getExportHistory(exportId);
        return ResponseEntity.ok(ApiResponse.success("Report export loaded", export));
    }

    private static ResponseEntity<byte[]> toAttachmentResponse(ReportFile file) {
        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(file.filename(), StandardCharsets.UTF_8)
                        .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.contentLength())
                .body(file.content());
    }
}
