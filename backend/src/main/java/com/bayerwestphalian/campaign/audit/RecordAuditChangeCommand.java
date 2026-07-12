package com.bayerwestphalian.campaign.audit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.StringUtils;

/**
 * Internal command for recording a sensitive-action audit entry (KB item 517 / {@code
 * AuditLog.recordChange} / COMP-008).
 *
 * <p>Audit rows are written by domain services (user, consent, campaign, product, report export),
 * not by public create APIs. This command carries the KB payload shape before persistence:
 * actor, action, entity type/id, optional old/new JSON values, optional client IP.
 */
public record RecordAuditChangeCommand(
        UUID actorUserId,
        String action,
        String entityType,
        UUID entityId,
        Map<String, Object> oldValue,
        Map<String, Object> newValue,
        String ipAddress) {

    public RecordAuditChangeCommand {
        if (!StringUtils.hasText(action)) {
            throw new IllegalArgumentException("action is required");
        }
        if (!StringUtils.hasText(entityType)) {
            throw new IllegalArgumentException("entityType is required");
        }
        action = action.trim();
        entityType = entityType.trim();
        oldValue = copyPayload(oldValue);
        newValue = copyPayload(newValue);
        if (StringUtils.hasText(ipAddress)) {
            ipAddress = ipAddress.trim();
        } else {
            ipAddress = null;
        }
    }

    /**
     * Convenience factory matching KB {@code AuditLog(actor, action, entityType, entityId)} plus
     * optional before/after payloads.
     */
    public static RecordAuditChangeCommand of(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        return new RecordAuditChangeCommand(
                actorUserId,
                action,
                entityType,
                entityId,
                castPayload(oldValue),
                castPayload(newValue),
                null);
    }

    /** Builds an {@link AuditLog} entity via {@link AuditLog#recordAction}, including client IP. */
    public AuditLog toEntity() {
        return AuditLog.recordAction(
                actorUserId, action, entityType, entityId, oldValue, newValue, ipAddress);
    }

    private static Map<String, Object> castPayload(Map<String, ?> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return new LinkedHashMap<>(value);
    }

    private static Map<String, Object> copyPayload(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }
}
