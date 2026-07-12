package com.bayerwestphalian.campaign.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "action", nullable = false, length = 255)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private Map<String, Object> oldValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    private Map<String, Object> newValue;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {}

    private AuditLog(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            Map<String, Object> oldValue,
            Map<String, Object> newValue,
            String ipAddress) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.ipAddress = ipAddress;
    }

    public static AuditLog recordCreate(
            UUID actorUserId, String entityType, UUID entityId, Map<String, ?> newValue) {
        return recordAction(actorUserId, "CREATE", entityType, entityId, null, newValue);
    }

    public static AuditLog recordAction(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        return recordAction(actorUserId, action, entityType, entityId, oldValue, newValue, null);
    }

    public static AuditLog recordAction(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue,
            String ipAddress) {
        return new AuditLog(
                actorUserId,
                action,
                entityType,
                entityId,
                copy(oldValue),
                copy(newValue),
                copyIp(ipAddress));
    }

    public static AuditLog recordChange(
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            Map<String, ?> oldValue,
            Map<String, ?> newValue) {
        return recordAction(actorUserId, action, entityType, entityId, oldValue, newValue);
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public Map<String, Object> getOldValue() {
        return oldValue;
    }

    public Map<String, Object> getNewValue() {
        return newValue;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    private static Map<String, Object> copy(Map<String, ?> value) {
        return value == null || value.isEmpty() ? null : new LinkedHashMap<>(value);
    }

    private static String copyIp(String ipAddress) {
        if (ipAddress == null) {
            return null;
        }
        String trimmed = ipAddress.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
