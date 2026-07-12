package com.bayerwestphalian.campaign.audit;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST boundary for audit log reads (KB item 519 / E22 / {@code GET /api/audit-logs}).
 *
 * <p>Authorized roles: Admin, Compliance Officer, System Auditor. Writes are not exposed — domain
 * services record immutable audit rows via {@link AuditService} inside their transactions
 * (COMP-008).
 *
 * <p>KB controller methods: {@code listAuditLogs()}, {@code getEntityHistory()}. Optional list
 * filters support item 533 UI filters (actor, action, entity, date).
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private static final String AUDIT_READ =
            "@authz.hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'SYSTEM_AUDITOR')";

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Lists audit logs newest first.
     *
     * <p>{@code GET /api/audit-logs} with optional query filters: {@code actorUserId}, {@code
     * action}, {@code entityType}, {@code entityId}, {@code createdFrom}, {@code createdTo}.
     */
    @GetMapping
    @PreAuthorize(AUDIT_READ)
    public ResponseEntity<ApiResponse<List<AuditLogView>>> listAuditLogs(
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant createdTo) {
        AuditLogSearchCriteria criteria =
                new AuditLogSearchCriteria(
                        actorUserId, action, entityType, entityId, createdFrom, createdTo);

        List<AuditLogView> logs =
                criteria.isEmpty()
                        ? auditService.listAuditLogs()
                        : auditService.listAuditLogs(criteria);

        return ResponseEntity.ok(ApiResponse.success("Audit logs loaded", logs));
    }

    /**
     * Entity-scoped history (KB {@code getEntityHistory}).
     *
     * <p>{@code GET /api/audit-logs/entity-history?entityType=...&entityId=...}
     */
    @GetMapping("/entity-history")
    @PreAuthorize(AUDIT_READ)
    public ResponseEntity<ApiResponse<List<AuditLogView>>> getEntityHistory(
            @RequestParam String entityType, @RequestParam UUID entityId) {
        List<AuditLogView> history = auditService.getEntityHistory(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success("Entity audit history loaded", history));
    }

    /**
     * Path-style entity history alias for convenient client routing.
     *
     * <p>{@code GET /api/audit-logs/entities/{entityType}/{entityId}}
     */
    @GetMapping("/entities/{entityType}/{entityId}")
    @PreAuthorize(AUDIT_READ)
    public ResponseEntity<ApiResponse<List<AuditLogView>>> getEntityHistoryByPath(
            @PathVariable String entityType, @PathVariable UUID entityId) {
        List<AuditLogView> history = auditService.getEntityHistory(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success("Entity audit history loaded", history));
    }
}
