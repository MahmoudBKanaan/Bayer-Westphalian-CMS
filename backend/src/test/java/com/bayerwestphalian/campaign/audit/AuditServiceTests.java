package com.bayerwestphalian.campaign.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 518: {@link AuditService} unit coverage for E22 audit logging (COMP-008 / NFR-008).
 *
 * <p>Covers list/history reads, search criteria, generic {@code recordChange}, and domain write
 * helpers used by user/consent/campaign/product/report flows (items 520–531).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("518 Implement AuditService")
class AuditServiceTests {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000009901");
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000009902");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000518");
    private static final UUID EXPORT_ID =
            UUID.fromString("43000000-0000-0000-0000-000000000518");
    private static final Instant T1 = Instant.parse("2026-07-11T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-07-11T12:00:00Z");
    private static final Instant T3 = Instant.parse("2026-07-11T14:00:00Z");

    @Mock private AuditLogRepository auditLogRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository);
    }

    @Nested
    @DisplayName("Reads — listAuditLogs / getEntityHistory")
    class ReadOperations {

        @Test
        void listAuditLogsUsesRecentRepositoryQuery() {
            AuditLog auditLog =
                    AuditLog.recordCreate(
                            ACTOR_ID, "campaigns", USER_ID, Map.of("name", "Summer Campaign"));
            when(auditLogRepository.findRecent()).thenReturn(List.of(auditLog));

            List<AuditLogView> views = auditService.listAuditLogs();

            assertThat(views).hasSize(1);
            assertThat(views.getFirst().action()).isEqualTo("CREATE");
            assertThat(views.getFirst().entityType()).isEqualTo("campaigns");
            verify(auditLogRepository).findRecent();
        }

        @Test
        void listAuditLogsWithNullOrEmptyCriteriaDelegatesToRecent() {
            when(auditLogRepository.findRecent()).thenReturn(List.of());

            assertThat(auditService.listAuditLogs(null)).isEmpty();
            assertThat(
                            auditService.listAuditLogs(
                                    new AuditLogSearchCriteria(
                                            null, null, null, null, null, null)))
                    .isEmpty();

            verify(auditLogRepository, org.mockito.Mockito.times(2)).findRecent();
        }

        @Test
        void listAuditLogsFiltersByEntityViaRepository() {
            AuditLog match =
                    withCreatedAt(
                            AuditLog.recordAction(
                                    ACTOR_ID,
                                    "APPROVE",
                                    "campaigns",
                                    CAMPAIGN_ID,
                                    Map.of("status", "SUBMITTED"),
                                    Map.of("status", "APPROVED")),
                            T2);
            when(auditLogRepository.findByEntityTypeAndEntityId("campaigns", CAMPAIGN_ID))
                    .thenReturn(List.of(match));

            List<AuditLogView> views =
                    auditService.listAuditLogs(
                            new AuditLogSearchCriteria(
                                    null, "APPROVE", "campaigns", CAMPAIGN_ID, null, null));

            assertThat(views).hasSize(1);
            assertThat(views.getFirst().action()).isEqualTo("APPROVE");
            assertThat(views.getFirst().entityId()).isEqualTo(CAMPAIGN_ID);
            verify(auditLogRepository).findByEntityTypeAndEntityId("campaigns", CAMPAIGN_ID);
            verify(auditLogRepository, never()).findRecent();
        }

        @Test
        void listAuditLogsFiltersByActorActionAndDateRange() {
            AuditLog inRange =
                    withCreatedAt(
                            AuditLog.recordAction(
                                    ACTOR_ID, "UPDATE", "customers", USER_ID, null, Map.of("x", 1)),
                            T2);
            AuditLog wrongAction =
                    withCreatedAt(
                            AuditLog.recordAction(
                                    ACTOR_ID, "DELETE", "customers", USER_ID, null, Map.of("x", 2)),
                            T2);
            AuditLog tooEarly =
                    withCreatedAt(
                            AuditLog.recordAction(
                                    ACTOR_ID, "UPDATE", "customers", USER_ID, null, Map.of("x", 3)),
                            T1);
            when(auditLogRepository.findByActorUserId(ACTOR_ID))
                    .thenReturn(List.of(inRange, wrongAction, tooEarly));

            List<AuditLogView> views =
                    auditService.listAuditLogs(
                            new AuditLogSearchCriteria(
                                    ACTOR_ID,
                                    "UPDATE",
                                    null,
                                    null,
                                    Instant.parse("2026-07-11T11:00:00Z"),
                                    Instant.parse("2026-07-11T13:00:00Z")));

            assertThat(views).hasSize(1);
            assertThat(views.getFirst().newValue()).containsEntry("x", 1);
            verify(auditLogRepository).findByActorUserId(ACTOR_ID);
        }

        @Test
        void getEntityHistoryMapsRepositoryRows() {
            AuditLog first =
                    withCreatedAt(
                            AuditLog.recordAction(
                                    ACTOR_ID,
                                    "SUBMIT",
                                    "campaigns",
                                    CAMPAIGN_ID,
                                    Map.of("status", "DRAFT"),
                                    Map.of("status", "SUBMITTED")),
                            T3);
            AuditLog second =
                    withCreatedAt(
                            AuditLog.recordCreate(
                                    ACTOR_ID, "campaigns", CAMPAIGN_ID, Map.of("name", "Life")),
                            T1);
            when(auditLogRepository.findByEntityTypeAndEntityId("campaigns", CAMPAIGN_ID))
                    .thenReturn(List.of(first, second));

            List<AuditLogView> history = auditService.getEntityHistory("campaigns", CAMPAIGN_ID);

            assertThat(history).hasSize(2);
            assertThat(history.get(0).action()).isEqualTo("SUBMIT");
            assertThat(history.get(1).action()).isEqualTo("CREATE");
            verify(auditLogRepository).findByEntityTypeAndEntityId("campaigns", CAMPAIGN_ID);
        }

        @Test
        void getEntityHistoryViaCriteriaRequiresCriteria() {
            assertThatThrownBy(() -> auditService.getEntityHistory((EntityHistoryCriteria) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("criteria is required");
        }

        @Test
        void getEntityHistoryRejectsBlankEntityTypeAndNullEntityId() {
            assertThatThrownBy(() -> auditService.getEntityHistory(" ", CAMPAIGN_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Audit validation failed");
            assertThatThrownBy(() -> auditService.getEntityHistory("campaigns", null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Audit validation failed");
            verify(auditLogRepository, never())
                    .findByEntityTypeAndEntityId(any(), any());
        }
    }

    @Nested
    @DisplayName("Writes — recordChange and domain helpers")
    class WriteOperations {

        @BeforeEach
        void stubSave() {
            lenient()
                    .when(auditLogRepository.save(any(AuditLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void recordChangePersistsCommandIncludingIp() {
            RecordAuditChangeCommand command =
                    new RecordAuditChangeCommand(
                            ACTOR_ID,
                            "APPROVE",
                            "campaigns",
                            CAMPAIGN_ID,
                            Map.of("status", "SUBMITTED"),
                            Map.of("status", "APPROVED"),
                            "203.0.113.44");

            AuditLog auditLog = auditService.recordChange(command);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());
            assertThat(auditLog).isSameAs(captor.getValue());
            assertThat(auditLog.getAction()).isEqualTo("APPROVE");
            assertThat(auditLog.getEntityType()).isEqualTo("campaigns");
            assertThat(auditLog.getEntityId()).isEqualTo(CAMPAIGN_ID);
            assertThat(auditLog.getIpAddress()).isEqualTo("203.0.113.44");
            assertThat(auditLog.getOldValue()).containsEntry("status", "SUBMITTED");
            assertThat(auditLog.getNewValue()).containsEntry("status", "APPROVED");
        }

        @Test
        void recordChangeRequiresCommand() {
            assertThatThrownBy(() -> auditService.recordChange(null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Audit validation failed");
        }

        @Test
        void logsCreateEventWithStructuredPayload() {
            AuditLog auditLog =
                    auditService.logCreate(
                            ACTOR_ID,
                            "users",
                            USER_ID,
                            Map.of(
                                    "email",
                                    "advisor@bayer-westphalian.test",
                                    "status",
                                    "ACTIVE"));

            ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditLogCaptor.capture());
            assertThat(auditLog).isSameAs(auditLogCaptor.getValue());
            assertThat(auditLog.getAction()).isEqualTo("CREATE");
            assertThat(auditLog.getEntityType()).isEqualTo("users");
            assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
            assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
            assertThat(auditLog.getNewValue())
                    .containsEntry("email", "advisor@bayer-westphalian.test")
                    .containsEntry("status", "ACTIVE")
                    .doesNotContainKey("password");
        }

        @Test
        void logsConsentCreationEventWithStructuredPayload() {
            AuditLog auditLog =
                    auditService.logConsentCreation(
                            ACTOR_ID,
                            USER_ID,
                            Map.of(
                                    "customerId",
                                    USER_ID,
                                    "consentType",
                                    "MARKETING_EMAIL",
                                    "status",
                                    "GIVEN"));

            ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditLogCaptor.capture());
            assertThat(auditLog).isSameAs(auditLogCaptor.getValue());
            assertThat(auditLog.getAction()).isEqualTo("CREATE");
            assertThat(auditLog.getEntityType()).isEqualTo("consent_records");
            assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
            assertThat(auditLog.getNewValue())
                    .containsEntry("consentType", "MARKETING_EMAIL")
                    .containsEntry("status", "GIVEN");
        }

        @Test
        void logsConsentChangeEventWithOldAndNewValues() {
            AuditLog auditLog =
                    auditService.logConsentChange(
                            ACTOR_ID,
                            "WITHDRAW_CONSENT",
                            USER_ID,
                            Map.of("status", "GIVEN"),
                            Map.of("status", "WITHDRAWN"));

            assertThat(auditLog.getAction()).isEqualTo("WITHDRAW_CONSENT");
            assertThat(auditLog.getEntityType()).isEqualTo("consent_records");
            assertThat(auditLog.getOldValue()).containsEntry("status", "GIVEN");
            assertThat(auditLog.getNewValue()).containsEntry("status", "WITHDRAWN");
        }

        @Test
        void logsConsentWithdrawalEventWithOldAndNewValues() {
            AuditLog auditLog =
                    auditService.logConsentWithdrawal(
                            ACTOR_ID,
                            USER_ID,
                            Map.of("status", "GIVEN"),
                            Map.of("status", "WITHDRAWN"));

            assertThat(auditLog.getAction()).isEqualTo("WITHDRAW_CONSENT");
            assertThat(auditLog.getEntityType()).isEqualTo("consent_records");
        }

        @Test
        void logsOptOutChangeEvent() {
            AuditLog auditLog =
                    auditService.logOptOutChange(
                            ACTOR_ID,
                            USER_ID,
                            Map.of("status", "GIVEN"),
                            Map.of("status", "WITHDRAWN", "purpose", "MARKETING"));

            assertThat(auditLog.getAction()).isEqualTo("OPT_OUT");
            assertThat(auditLog.getEntityType()).isEqualTo("consent_records");
            assertThat(auditLog.getNewValue()).containsEntry("purpose", "MARKETING");
        }

        @Test
        void logsDoNotContactUpdateEventWithOldAndNewValues() {
            AuditLog auditLog =
                    auditService.logDoNotContactUpdate(
                            ACTOR_ID,
                            USER_ID,
                            Map.of("doNotContact", false),
                            Map.of("doNotContact", true));

            assertThat(auditLog.getAction()).isEqualTo("UPDATE_DO_NOT_CONTACT");
            assertThat(auditLog.getEntityType()).isEqualTo("customers");
            assertThat(auditLog.getOldValue()).containsEntry("doNotContact", false);
            assertThat(auditLog.getNewValue()).containsEntry("doNotContact", true);
        }

        @Test
        void logRoleChangeIsKbMethodAndLogRoleAssignmentDelegates() {
            AuditLog changed =
                    auditService.logRoleChange(
                            ACTOR_ID,
                            USER_ID,
                            Map.of("roles", List.of("BI_ANALYST")),
                            Map.of("roles", List.of("BI_ANALYST", "ADMIN")));

            assertThat(changed.getAction()).isEqualTo("ASSIGN_ROLE");
            assertThat(changed.getEntityType()).isEqualTo("users");
            assertThat(changed.getOldValue()).containsKey("roles");
            assertThat(changed.getNewValue()).containsKey("roles");

            AuditLog assigned =
                    auditService.logRoleAssignment(
                            ACTOR_ID,
                            USER_ID,
                            Map.of("email", "advisor@bayer-westphalian.test", "roleName", "ADMIN"));

            assertThat(assigned.getAction()).isEqualTo("ASSIGN_ROLE");
            assertThat(assigned.getOldValue()).isNull();
            assertThat(assigned.getNewValue()).containsEntry("roleName", "ADMIN");
        }

        @Test
        void logsUpdateEventWithOldAndNewValues() {
            AuditLog auditLog =
                    auditService.logUpdate(
                            ACTOR_ID,
                            "customers",
                            USER_ID,
                            Map.of("status", "ACTIVE"),
                            Map.of("status", "CONVERTED"));

            assertThat(auditLog.getAction()).isEqualTo("UPDATE");
            assertThat(auditLog.getEntityType()).isEqualTo("customers");
        }

        @Test
        void logsCampaignWorkflowEvents() {
            assertThat(
                            auditService
                                    .logSubmission(
                                            ACTOR_ID,
                                            "campaigns",
                                            CAMPAIGN_ID,
                                            Map.of("status", "DRAFT"),
                                            Map.of("status", "SUBMITTED"))
                                    .getAction())
                    .isEqualTo("SUBMIT");
            assertThat(
                            auditService
                                    .logApproval(
                                            ACTOR_ID,
                                            "campaigns",
                                            CAMPAIGN_ID,
                                            Map.of("status", "SUBMITTED"),
                                            Map.of("status", "APPROVED"))
                                    .getAction())
                    .isEqualTo("APPROVE");
            assertThat(
                            auditService
                                    .logRejection(
                                            ACTOR_ID,
                                            "campaigns",
                                            CAMPAIGN_ID,
                                            Map.of("status", "SUBMITTED"),
                                            Map.of("status", "REJECTED"))
                                    .getAction())
                    .isEqualTo("REJECT");
            assertThat(
                            auditService
                                    .logLaunch(
                                            ACTOR_ID,
                                            "campaigns",
                                            CAMPAIGN_ID,
                                            Map.of("status", "APPROVED"),
                                            Map.of("status", "ACTIVE"))
                                    .getAction())
                    .isEqualTo("LAUNCH");
        }

        @Test
        void logsDeleteEventWithOldAndNewValues() {
            AuditLog auditLog =
                    auditService.logDelete(
                            ACTOR_ID,
                            "customers",
                            USER_ID,
                            Map.of("deleted", false),
                            Map.of("deleted", true));

            assertThat(auditLog.getAction()).isEqualTo("DELETE");
            assertThat(auditLog.getEntityType()).isEqualTo("customers");
        }

        @Test
        void logsUserDisableEventWithStatusTransition() {
            AuditLog auditLog =
                    auditService.logUserDisable(
                            ACTOR_ID,
                            USER_ID,
                            Map.of("status", "ACTIVE"),
                            Map.of("status", "DISABLED"));

            assertThat(auditLog.getAction()).isEqualTo("DISABLE_USER");
            assertThat(auditLog.getEntityType()).isEqualTo("users");
        }

        @Test
        void logsReportExportEvent() {
            AuditLog auditLog =
                    auditService.logReportExport(
                            ACTOR_ID,
                            EXPORT_ID,
                            Map.of(
                                    "reportName",
                                    "Campaign CSV: Spring Life",
                                    "exportType",
                                    "CSV"));

            assertThat(auditLog.getAction()).isEqualTo("EXPORT_REPORT");
            assertThat(auditLog.getEntityType()).isEqualTo("report_exports");
            assertThat(auditLog.getEntityId()).isEqualTo(EXPORT_ID);
            assertThat(auditLog.getNewValue())
                    .containsEntry("reportName", "Campaign CSV: Spring Life")
                    .containsEntry("exportType", "CSV");
            assertThat(auditLog.getOldValue()).isNull();
        }

        @Test
        void logsProductChangeRequestApprovalAndRejection() {
            assertThat(
                            auditService
                                    .logApproval(
                                            ACTOR_ID,
                                            "product_change_requests",
                                            USER_ID,
                                            Map.of("status", "OPEN"),
                                            Map.of("status", "APPROVED"))
                                    .getEntityType())
                    .isEqualTo("product_change_requests");
            assertThat(
                            auditService
                                    .logRejection(
                                            ACTOR_ID,
                                            "product_change_requests",
                                            USER_ID,
                                            Map.of("status", "OPEN"),
                                            Map.of("status", "REJECTED"))
                                    .getAction())
                    .isEqualTo("REJECT");
        }

        @Test
        void rejectsBlankEntityType() {
            assertThatThrownBy(() -> auditService.logCreate(null, " ", USER_ID, Map.of()))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Audit validation failed");
            verify(auditLogRepository, never()).save(any());
        }

        @Test
        void rejectsBlankConsentAuditAction() {
            assertThatThrownBy(
                            () ->
                                    auditService.logConsentChange(
                                            ACTOR_ID, " ", USER_ID, null, Map.of("status", "GIVEN")))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Audit validation failed");
            verify(auditLogRepository, never()).save(any());
        }
    }

    private static AuditLog withCreatedAt(AuditLog auditLog, Instant createdAt) {
        ReflectionTestUtils.setField(auditLog, "createdAt", createdAt);
        ReflectionTestUtils.setField(auditLog, "id", UUID.randomUUID());
        return auditLog;
    }
}
