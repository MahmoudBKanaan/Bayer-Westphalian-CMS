/**
 * Report export package for CSV/PDF generation requests, export state, file references, and export
 * history (KB Reports epic / FR-109–FR-110).
 *
 * <p>Module documentation (item 460): {@code docs/modules/report-export.md}.
 *
 * <p>Entity (item 435): {@link com.bayerwestphalian.campaign.report.ReportExport} maps table {@code
 * report_exports} with {@link com.bayerwestphalian.campaign.report.ReportExportType} and {@link
 * com.bayerwestphalian.campaign.report.ReportExportStatus}.
 *
 * <p>Service (item 436): {@link com.bayerwestphalian.campaign.report.ReportService} —
 * {@code exportCampaignCsv} / {@code generateCampaignPdf} / {@code exportAuditReport} (and
 * controller aliases {@code campaignCsv}, {@code campaignPdf}, {@code auditReport}). Persists
 * history via {@link com.bayerwestphalian.campaign.report.ReportExportRepository} and returns
 * {@link com.bayerwestphalian.campaign.report.ReportFile} payloads.
 *
 * <p>Controller (item 437–439): {@link com.bayerwestphalian.campaign.report.ReportController}
 * under {@code /api/reports}.
 *
 * <ul>
 *   <li>Campaign CSV export (item 437 / acceptance item 455 / FR-109): {@code GET
 *       /api/reports/campaigns/{campaignId}/csv}
 *   <li>Campaign PDF export (item 438 / acceptance item 456 / FR-110): {@code GET
 *       /api/reports/campaigns/{campaignId}/pdf}
 *   <li>Export history list (item 439): {@code GET /api/reports/exports}
 *   <li>Export history detail (item 439): {@code GET /api/reports/exports/{exportId}}
 * </ul>
 *
 * <p>CSV/PDF document bytes are produced by package-private {@code CampaignReportDocument}. Every
 * export stores a {@link com.bayerwestphalian.campaign.report.ReportExport} history row (item 439)
 * with REQUESTED → COMPLETED/FAILED lifecycle. Acceptance item 455 formalizes that campaign CSV
 * export works end-to-end (document content, service completion, HTTP download). Acceptance item
 * 456 formalizes that campaign PDF export works end-to-end (PDF 1.4 content, service completion,
 * {@code application/pdf} download).
 *
 * <p>Access control (item 458): only Admin, BI Analyst, Campaign Manager, Marketing Analyst, and
 * Executive Viewer may export campaign CSV/PDF or read export history. Unauthenticated callers
 * receive 401; other roles receive 403 without invoking export generation.
 *
 * <p>Aggregation and traceability (item 466 / COMP-010 / BR-034): campaign CSV/PDF exports
 * serialize aggregated {@code campaign_metrics} KPIs via analytics (not raw recipient or
 * contact-event rows). Eligible/excluded/sent remain reconcilable to campaign recipients at
 * launch; opened/clicked/replied/converted remain reconcilable to contact events.
 *
 * <p>Item 531: successful campaign CSV/PDF and audit-history exports write {@code EXPORT_REPORT}
 * audit rows on entity type {@code report_exports} via {@link
 * com.bayerwestphalian.campaign.audit.AuditService#logReportExport}.
 */
package com.bayerwestphalian.campaign.report;
