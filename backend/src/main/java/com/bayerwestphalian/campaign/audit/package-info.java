/**
 * Audit logging package for immutable sensitive-action history (KB epic E22 / COMP-008 / NFR-008).
 *
 * <p>Entity (item 515): {@link com.bayerwestphalian.campaign.audit.AuditLog} maps table {@code
 * audit_logs} (actor, action, entity type/id, old/new JSON, IP, created_at).
 *
 * <p>Repository (item 516): {@link com.bayerwestphalian.campaign.audit.AuditLogRepository} —
 * {@code findRecent}, {@code findByEntityTypeAndEntityId}, {@code findByActorUserId}.
 *
 * <p>DTOs (item 517):
 *
 * <ul>
 *   <li>{@link com.bayerwestphalian.campaign.audit.AuditLogView} — API/report projection
 *   <li>{@link com.bayerwestphalian.campaign.audit.AuditLogSearchCriteria} — list filters (actor,
 *       action, entity, date range)
 *   <li>{@link com.bayerwestphalian.campaign.audit.EntityHistoryCriteria} — {@code
 *       getEntityHistory} input
 *   <li>{@link com.bayerwestphalian.campaign.audit.RecordAuditChangeCommand} — internal write
 *       command for {@code recordChange}-style logging
 * </ul>
 *
 * <p>Service (item 518): {@link com.bayerwestphalian.campaign.audit.AuditService} —
 *
 * <ul>
 *   <li>Reads: {@code listAuditLogs()}, {@code listAuditLogs(AuditLogSearchCriteria)}, {@code
 *       getEntityHistory}
 *   <li>Writes (MANDATORY TX): {@code recordChange}, {@code logCreate}, {@code logUpdate}, {@code
 *       logDelete}, {@code logApproval}, {@code logRejection}, {@code logSubmission}, {@code
 *       logLaunch}, {@code logConsentChange}, {@code logRoleChange}, {@code logUserDisable},
 *       {@code logReportExport}, and related helpers
 * </ul>
 *
 * <p>Controller (item 519): {@link com.bayerwestphalian.campaign.audit.AuditController} under
 * {@code /api/audit-logs}:
 *
 * <ul>
 *   <li>{@code GET /api/audit-logs} — {@code listAuditLogs} with optional actor/action/entity/date
 *       filters
 *   <li>{@code GET /api/audit-logs/entity-history} — {@code getEntityHistory} (query params)
 *   <li>{@code GET /api/audit-logs/entities/{entityType}/{entityId}} — entity history path alias
 * </ul>
 *
 * <p>Roles: Admin, Compliance Officer, System Auditor (filter chain + method security). Audit
 * writes are not exposed on this controller.
 *
 * <p>UI (items 532–533): frontend {@code AuditPage} at {@code /audit} lists sensitive actions,
 * shows a selected-entry detail panel, loads entity history, and filters the list by actor,
 * action, entity type/id, and created-at range via {@code GET /api/audit-logs} query params.
 */
package com.bayerwestphalian.campaign.audit;
