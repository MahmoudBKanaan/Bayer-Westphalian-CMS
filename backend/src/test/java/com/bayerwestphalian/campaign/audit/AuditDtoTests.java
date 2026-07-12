package com.bayerwestphalian.campaign.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 517: audit DTOs for E22 audit logging (COMP-008 / NFR-008 / System Auditor).
 *
 * <p>Covers API views, list filters (actor/action/entity/date), entity-history criteria for {@code
 * getEntityHistory}, and the internal record-change command used by domain services.
 */
@DisplayName("517 Implement audit DTOs")
class AuditDtoTests {

    private static final UUID AUDIT_ID = UUID.fromString("53000000-0000-0000-0000-000000000517");
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000517");
    private static final UUID ENTITY_ID = UUID.fromString("50000000-0000-0000-0000-000000000517");
    private static final Instant CREATED_AT = Instant.parse("2026-07-11T14:30:00Z");

    @Nested
    @DisplayName("AuditLogView")
    class ViewTests {

        @Test
        void fromMapsAllKbAuditLogFields() {
            AuditLog entity =
                    AuditLog.recordChange(
                            ACTOR_ID,
                            "APPROVE",
                            "campaigns",
                            ENTITY_ID,
                            Map.of("status", "SUBMITTED"),
                            Map.of("status", "APPROVED"));
            ReflectionTestUtils.setField(entity, "id", AUDIT_ID);
            ReflectionTestUtils.setField(entity, "createdAt", CREATED_AT);
            ReflectionTestUtils.setField(entity, "ipAddress", "203.0.113.10");

            AuditLogView view = AuditLogView.from(entity);

            assertThat(view.id()).isEqualTo(AUDIT_ID);
            assertThat(view.actorUserId()).isEqualTo(ACTOR_ID);
            assertThat(view.action()).isEqualTo("APPROVE");
            assertThat(view.entityType()).isEqualTo("campaigns");
            assertThat(view.entityId()).isEqualTo(ENTITY_ID);
            assertThat(view.oldValue()).containsEntry("status", "SUBMITTED");
            assertThat(view.newValue()).containsEntry("status", "APPROVED");
            assertThat(view.ipAddress()).isEqualTo("203.0.113.10");
            assertThat(view.createdAt()).isEqualTo(CREATED_AT);
        }

        @Test
        void fromMapsCreateEventWithoutOldValue() {
            AuditLog entity =
                    AuditLog.recordCreate(
                            ACTOR_ID, "users", ENTITY_ID, Map.of("email", "auditor@test.example"));
            ReflectionTestUtils.setField(entity, "id", AUDIT_ID);
            ReflectionTestUtils.setField(entity, "createdAt", CREATED_AT);

            AuditLogView view = AuditLogView.from(entity);

            assertThat(view.action()).isEqualTo("CREATE");
            assertThat(view.oldValue()).isNull();
            assertThat(view.newValue()).containsEntry("email", "auditor@test.example");
            assertThat(view.ipAddress()).isNull();
        }

        @Test
        void fromRequiresAuditLog() {
            assertThatThrownBy(() -> AuditLogView.from(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("auditLog is required");
        }

        @Test
        void viewPayloadMapsAreDefensiveCopies() {
            Map<String, Object> oldPayload = new HashMap<>();
            oldPayload.put("status", "ACTIVE");
            Map<String, Object> newPayload = new HashMap<>();
            newPayload.put("status", "DISABLED");

            AuditLogView view =
                    new AuditLogView(
                            AUDIT_ID,
                            ACTOR_ID,
                            "DISABLE_USER",
                            "users",
                            ENTITY_ID,
                            oldPayload,
                            newPayload,
                            null,
                            CREATED_AT);

            oldPayload.put("status", "MUTATED");
            newPayload.clear();

            assertThat(view.oldValue()).containsEntry("status", "ACTIVE");
            assertThat(view.newValue()).containsEntry("status", "DISABLED");
            assertThatThrownBy(() -> view.oldValue().put("extra", "x"))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> view.newValue().put("extra", "x"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("AuditLogSearchCriteria")
    class SearchCriteriaTests {

        @Test
        void holdsActorActionEntityAndDateFilters() {
            Instant from = Instant.parse("2026-07-01T00:00:00Z");
            Instant to = Instant.parse("2026-07-11T23:59:59Z");

            AuditLogSearchCriteria criteria =
                    new AuditLogSearchCriteria(
                            ACTOR_ID, "APPROVE", "campaigns", ENTITY_ID, from, to);

            assertThat(criteria.actorUserId()).isEqualTo(ACTOR_ID);
            assertThat(criteria.action()).isEqualTo("APPROVE");
            assertThat(criteria.entityType()).isEqualTo("campaigns");
            assertThat(criteria.entityId()).isEqualTo(ENTITY_ID);
            assertThat(criteria.createdFrom()).isEqualTo(from);
            assertThat(criteria.createdTo()).isEqualTo(to);
            assertThat(criteria.isEmpty()).isFalse();
            assertThat(criteria.hasEntityFilter()).isTrue();
            assertThat(criteria.hasDateFilter()).isTrue();
        }

        @Test
        void emptyCriteriaMeansUnfilteredRecentList() {
            AuditLogSearchCriteria criteria =
                    new AuditLogSearchCriteria(null, null, null, null, null, null);

            assertThat(criteria.isEmpty()).isTrue();
            assertThat(criteria.hasEntityFilter()).isFalse();
            assertThat(criteria.hasDateFilter()).isFalse();
        }

        @Test
        void blankActionOrEntityTypeDoesNotCountAsFilter() {
            AuditLogSearchCriteria criteria =
                    new AuditLogSearchCriteria(null, "  ", "  ", null, null, null);

            assertThat(criteria.isEmpty()).isTrue();
            assertThat(criteria.hasEntityFilter()).isFalse();
        }

        @Test
        void toEntityHistoryCriteriaRequiresCompleteEntityFilter() {
            AuditLogSearchCriteria incomplete =
                    new AuditLogSearchCriteria(null, null, "campaigns", null, null, null);

            assertThatThrownBy(incomplete::toEntityHistoryCriteria)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("entityType and entityId");

            EntityHistoryCriteria history =
                    new AuditLogSearchCriteria(null, null, " campaigns ", ENTITY_ID, null, null)
                            .toEntityHistoryCriteria();

            assertThat(history.entityType()).isEqualTo("campaigns");
            assertThat(history.entityId()).isEqualTo(ENTITY_ID);
        }
    }

    @Nested
    @DisplayName("EntityHistoryCriteria")
    class EntityHistoryCriteriaTests {

        @Test
        void requiresEntityTypeAndEntityId() {
            EntityHistoryCriteria criteria = new EntityHistoryCriteria("consent_records", ENTITY_ID);

            assertThat(criteria.entityType()).isEqualTo("consent_records");
            assertThat(criteria.entityId()).isEqualTo(ENTITY_ID);
        }

        @Test
        void trimsEntityType() {
            EntityHistoryCriteria criteria =
                    new EntityHistoryCriteria("  product_change_requests  ", ENTITY_ID);

            assertThat(criteria.entityType()).isEqualTo("product_change_requests");
        }

        @Test
        void rejectsBlankEntityType() {
            assertThatThrownBy(() -> new EntityHistoryCriteria("  ", ENTITY_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("entityType is required");
            assertThatThrownBy(() -> new EntityHistoryCriteria(null, ENTITY_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("entityType is required");
        }

        @Test
        void rejectsNullEntityId() {
            assertThatThrownBy(() -> new EntityHistoryCriteria("campaigns", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("entityId is required");
        }
    }

    @Nested
    @DisplayName("RecordAuditChangeCommand")
    class RecordCommandTests {

        @Test
        void ofBuildsCommandAndToEntityUsesRecordAction() {
            RecordAuditChangeCommand command =
                    RecordAuditChangeCommand.of(
                            ACTOR_ID,
                            "WITHDRAW_CONSENT",
                            "consent_records",
                            ENTITY_ID,
                            Map.of("status", "GIVEN"),
                            Map.of("status", "WITHDRAWN"));

            assertThat(command.actorUserId()).isEqualTo(ACTOR_ID);
            assertThat(command.action()).isEqualTo("WITHDRAW_CONSENT");
            assertThat(command.entityType()).isEqualTo("consent_records");
            assertThat(command.entityId()).isEqualTo(ENTITY_ID);
            assertThat(command.oldValue()).containsEntry("status", "GIVEN");
            assertThat(command.newValue()).containsEntry("status", "WITHDRAWN");
            assertThat(command.ipAddress()).isNull();

            AuditLog entity = command.toEntity();
            assertThat(entity.getActorUserId()).isEqualTo(ACTOR_ID);
            assertThat(entity.getAction()).isEqualTo("WITHDRAW_CONSENT");
            assertThat(entity.getEntityType()).isEqualTo("consent_records");
            assertThat(entity.getEntityId()).isEqualTo(ENTITY_ID);
            assertThat(entity.getOldValue()).containsEntry("status", "GIVEN");
            assertThat(entity.getNewValue()).containsEntry("status", "WITHDRAWN");
        }

        @Test
        void trimsActionEntityTypeAndIpAndDefendsPayloads() {
            Map<String, Object> oldPayload = new HashMap<>();
            oldPayload.put("role", "BI_ANALYST");
            Map<String, Object> newPayload = new HashMap<>();
            newPayload.put("role", "ADMIN");

            RecordAuditChangeCommand command =
                    new RecordAuditChangeCommand(
                            ACTOR_ID,
                            "  ASSIGN_ROLE  ",
                            "  users  ",
                            ENTITY_ID,
                            oldPayload,
                            newPayload,
                            "  198.51.100.20  ");

            assertThat(command.action()).isEqualTo("ASSIGN_ROLE");
            assertThat(command.entityType()).isEqualTo("users");
            assertThat(command.ipAddress()).isEqualTo("198.51.100.20");

            oldPayload.put("role", "MUTATED");
            assertThat(command.oldValue()).containsEntry("role", "BI_ANALYST");
            assertThatThrownBy(() -> command.newValue().put("extra", true))
                    .isInstanceOf(UnsupportedOperationException.class);

            AuditLog entity = command.toEntity();
            assertThat(entity.getAction()).isEqualTo("ASSIGN_ROLE");
            assertThat(entity.getEntityType()).isEqualTo("users");
            assertThat(entity.getIpAddress()).isEqualTo("198.51.100.20");
            assertThat(entity.getOldValue()).containsEntry("role", "BI_ANALYST");
        }

        @Test
        void rejectsBlankAction() {
            assertThatThrownBy(
                            () ->
                                    RecordAuditChangeCommand.of(
                                            ACTOR_ID, " ", "users", ENTITY_ID, null, Map.of("x", 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("action is required");
        }

        @Test
        void rejectsBlankEntityType() {
            assertThatThrownBy(
                            () ->
                                    new RecordAuditChangeCommand(
                                            ACTOR_ID, "CREATE", null, ENTITY_ID, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("entityType is required");
        }

        @Test
        void blankIpBecomesNull() {
            RecordAuditChangeCommand command =
                    new RecordAuditChangeCommand(
                            ACTOR_ID, "CREATE", "users", ENTITY_ID, null, Map.of("a", 1), "   ");

            assertThat(command.ipAddress()).isNull();
            assertThat(command.oldValue()).isNull();
            assertThat(command.newValue()).containsEntry("a", 1);
        }
    }
}
