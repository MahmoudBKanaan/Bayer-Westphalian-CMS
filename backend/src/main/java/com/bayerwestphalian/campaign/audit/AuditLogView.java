package com.bayerwestphalian.campaign.audit;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * API / service view of a stored {@link AuditLog} row (KB item 517 / E22 / table {@code
 * audit_logs}).
 *
 * <p>Returned by {@code GET /api/audit-logs}, entity-history queries, and audit CSV/PDF report
 * generation. Payload maps are defensive copies so callers cannot mutate persisted JSON after the
 * view is built (COMP-008 application-level immutability of audit presentation).
 *
 * <p>Fields match the KB entity contract: actor, action, entity type/id, optional old/new JSON
 * values, optional IP, and created timestamp.
 */
public record AuditLogView(
        UUID id,
        UUID actorUserId,
        String action,
        String entityType,
        UUID entityId,
        Map<String, Object> oldValue,
        Map<String, Object> newValue,
        String ipAddress,
        Instant createdAt) {

    public AuditLogView {
        oldValue = copyPayload(oldValue);
        newValue = copyPayload(newValue);
    }

    /**
     * Maps a persisted audit row to the API view.
     *
     * @throws NullPointerException when {@code auditLog} is null
     */
    public static AuditLogView from(AuditLog auditLog) {
        Objects.requireNonNull(auditLog, "auditLog is required");
        return new AuditLogView(
                auditLog.getId(),
                auditLog.getActorUserId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getOldValue(),
                auditLog.getNewValue(),
                auditLog.getIpAddress(),
                auditLog.getCreatedAt());
    }

    private static Map<String, Object> copyPayload(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return value == null ? null : Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }
}
