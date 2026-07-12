package com.bayerwestphalian.campaign.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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

    public static AuditLogView from(AuditLog auditLog) {
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
}
