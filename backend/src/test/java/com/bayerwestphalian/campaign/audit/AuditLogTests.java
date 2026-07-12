package com.bayerwestphalian.campaign.audit;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditLogTests {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000009901");
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000009902");

    @Test
    void mapsKbAuditLogsTableAsJpaEntity() throws Exception {
        assertThat(AuditLog.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(AuditLog.class.getAnnotation(Table.class).name()).isEqualTo("audit_logs");
        assertColumn("actorUserId", "actor_user_id", true, true, 255);
        assertColumn("action", "action", false, true, 255);
        assertColumn("entityType", "entity_type", false, true, 100);
        assertColumn("entityId", "entity_id", true, true, 255);
        assertColumn("oldValue", "old_value", true, true, 255);
        assertColumn("newValue", "new_value", true, true, 255);
        assertColumn("ipAddress", "ip_address", true, true, 100);
        assertColumn("createdAt", "created_at", false, false, 255);
    }

    @Test
    @DisplayName("515 Implement AuditLog entity")
    void implementsKbAuditLogEntityContract() throws Exception {
        AuditLog auditLog =
                AuditLog.recordChange(
                        ACTOR_ID,
                        "UPDATE_CONSENT",
                        "consent_records",
                        USER_ID,
                        Map.of("status", "GRANTED"),
                        Map.of("status", "WITHDRAWN"));

        assertThat(AuditLog.class.getDeclaredField("id").isAnnotationPresent(Id.class)).isTrue();
        assertThat(
                        AuditLog.class
                                .getDeclaredMethod("onCreate")
                                .isAnnotationPresent(PrePersist.class))
                .isTrue();
        assertJsonbColumn("oldValue");
        assertJsonbColumn("newValue");
        assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(auditLog.getAction()).isEqualTo("UPDATE_CONSENT");
        assertThat(auditLog.getEntityType()).isEqualTo("consent_records");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getOldValue()).containsEntry("status", "GRANTED");
        assertThat(auditLog.getNewValue()).containsEntry("status", "WITHDRAWN");
        List<String> publicMutators =
                Arrays.stream(AuditLog.class.getMethods())
                        .filter(method -> method.getDeclaringClass().equals(AuditLog.class))
                        .filter(method -> Modifier.isPublic(method.getModifiers()))
                        .map(Method::getName)
                        .filter(name -> name.startsWith("set"))
                        .toList();
        assertThat(publicMutators).isEmpty();
    }

    @Test
    void recordsCreateAuditEventWithoutOldValue() {
        AuditLog auditLog =
                AuditLog.recordCreate(ACTOR_ID, "users", USER_ID, Map.of("email", "user@test"));

        assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(auditLog.getAction()).isEqualTo("CREATE");
        assertThat(auditLog.getEntityType()).isEqualTo("users");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getOldValue()).isNull();
        assertThat(auditLog.getNewValue()).containsEntry("email", "user@test");
    }

    @Test
    void recordsRoleAssignmentAuditEvent() {
        AuditLog auditLog =
                AuditLog.recordAction(
                        ACTOR_ID,
                        "ASSIGN_ROLE",
                        "users",
                        USER_ID,
                        null,
                        Map.of("roleName", "ADMIN"));

        assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(auditLog.getAction()).isEqualTo("ASSIGN_ROLE");
        assertThat(auditLog.getEntityType()).isEqualTo("users");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getOldValue()).isNull();
        assertThat(auditLog.getNewValue()).containsEntry("roleName", "ADMIN");
    }

    @Test
    void recordsUserDisableAuditEventWithOldAndNewStatus() {
        AuditLog auditLog =
                AuditLog.recordAction(
                        ACTOR_ID,
                        "DISABLE_USER",
                        "users",
                        USER_ID,
                        Map.of("status", "ACTIVE"),
                        Map.of("status", "DISABLED"));

        assertThat(auditLog.getAction()).isEqualTo("DISABLE_USER");
        assertThat(auditLog.getEntityType()).isEqualTo("users");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getOldValue()).containsEntry("status", "ACTIVE");
        assertThat(auditLog.getNewValue()).containsEntry("status", "DISABLED");
    }

    @Test
    void initializesIdAndCreatedAtBeforePersist() throws Exception {
        AuditLog auditLog = AuditLog.recordCreate(null, "users", USER_ID, Map.of());

        Method onCreate = AuditLog.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(auditLog);

        assertThat(auditLog.getId()).isNotNull();
        assertThat(auditLog.getCreatedAt()).isNotNull();
    }

    private static void assertColumn(
            String fieldName, String columnName, boolean nullable, boolean updatable, int length)
            throws Exception {
        Column column = AuditLog.class.getDeclaredField(fieldName).getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(column.updatable()).isEqualTo(updatable);
        assertThat(column.length()).isEqualTo(length);
    }

    private static void assertJsonbColumn(String fieldName) throws Exception {
        JdbcTypeCode jdbcTypeCode =
                AuditLog.class.getDeclaredField(fieldName).getAnnotation(JdbcTypeCode.class);
        Column column = AuditLog.class.getDeclaredField(fieldName).getAnnotation(Column.class);

        assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.JSON);
        assertThat(column.columnDefinition()).isEqualTo("jsonb");
    }
}
