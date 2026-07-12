package com.bayerwestphalian.campaign.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.common.exception.ValidationException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceTests {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000009901");
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000009902");

    @Mock private AuditLogRepository auditLogRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository);
    }

    @Test
    void logsCreateEventWithStructuredPayload() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog auditLog =
                auditService.logCreate(
                        ACTOR_ID,
                        "users",
                        USER_ID,
                        Map.of("email", "advisor@bayer-westphalian.test", "status", "ACTIVE"));

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
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog auditLog =
                auditService.logConsentCreation(
                        ACTOR_ID,
                        USER_ID,
                        Map.of(
                                "customerId", USER_ID,
                                "consentType", "MARKETING_EMAIL",
                                "status", "GIVEN"));

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLog).isSameAs(auditLogCaptor.getValue());
        assertThat(auditLog.getAction()).isEqualTo("CREATE");
        assertThat(auditLog.getEntityType()).isEqualTo("consent_records");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(auditLog.getNewValue())
                .containsEntry("customerId", USER_ID)
                .containsEntry("consentType", "MARKETING_EMAIL")
                .containsEntry("status", "GIVEN");
    }

    @Test
    void logsConsentChangeEventWithOldAndNewValues() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog auditLog =
                auditService.logConsentChange(
                        ACTOR_ID,
                        "WITHDRAW_CONSENT",
                        USER_ID,
                        Map.of("status", "GIVEN"),
                        Map.of("status", "WITHDRAWN"));

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLog).isSameAs(auditLogCaptor.getValue());
        assertThat(auditLog.getAction()).isEqualTo("WITHDRAW_CONSENT");
        assertThat(auditLog.getEntityType()).isEqualTo("consent_records");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(auditLog.getOldValue()).containsEntry("status", "GIVEN");
        assertThat(auditLog.getNewValue()).containsEntry("status", "WITHDRAWN");
    }

    @Test
    void logsConsentWithdrawalEventWithOldAndNewValues() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog auditLog =
                auditService.logConsentWithdrawal(
                        ACTOR_ID,
                        USER_ID,
                        Map.of("status", "GIVEN"),
                        Map.of("status", "WITHDRAWN"));

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLog).isSameAs(auditLogCaptor.getValue());
        assertThat(auditLog.getAction()).isEqualTo("WITHDRAW_CONSENT");
        assertThat(auditLog.getEntityType()).isEqualTo("consent_records");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(auditLog.getOldValue()).containsEntry("status", "GIVEN");
        assertThat(auditLog.getNewValue()).containsEntry("status", "WITHDRAWN");
    }

    @Test
    void logsDoNotContactUpdateEventWithOldAndNewValues() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog auditLog =
                auditService.logDoNotContactUpdate(
                        ACTOR_ID,
                        USER_ID,
                        Map.of("doNotContact", false),
                        Map.of("doNotContact", true));

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLog).isSameAs(auditLogCaptor.getValue());
        assertThat(auditLog.getAction()).isEqualTo("UPDATE_DO_NOT_CONTACT");
        assertThat(auditLog.getEntityType()).isEqualTo("customers");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(auditLog.getOldValue()).containsEntry("doNotContact", false);
        assertThat(auditLog.getNewValue()).containsEntry("doNotContact", true);
    }

    @Test
    void logsRoleAssignmentEventWithStructuredPayload() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog auditLog =
                auditService.logRoleAssignment(
                        ACTOR_ID,
                        USER_ID,
                        Map.of("email", "advisor@bayer-westphalian.test", "roleName", "ADMIN"));

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLog).isSameAs(auditLogCaptor.getValue());
        assertThat(auditLog.getAction()).isEqualTo("ASSIGN_ROLE");
        assertThat(auditLog.getEntityType()).isEqualTo("users");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(auditLog.getNewValue())
                .containsEntry("email", "advisor@bayer-westphalian.test")
                .containsEntry("roleName", "ADMIN");
    }

    @Test
    void logsUpdateEventWithOldAndNewValues() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog auditLog =
                auditService.logUpdate(
                        ACTOR_ID,
                        "customers",
                        USER_ID,
                        Map.of("status", "ACTIVE"),
                        Map.of("status", "CONVERTED"));

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLog).isSameAs(auditLogCaptor.getValue());
        assertThat(auditLog.getAction()).isEqualTo("UPDATE");
        assertThat(auditLog.getEntityType()).isEqualTo("customers");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getOldValue()).containsEntry("status", "ACTIVE");
        assertThat(auditLog.getNewValue()).containsEntry("status", "CONVERTED");
    }

    @Test
    void logsCampaignLaunchEventWithOldAndNewValues() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog auditLog =
                auditService.logLaunch(
                        ACTOR_ID,
                        "campaigns",
                        USER_ID,
                        Map.of("status", "APPROVED"),
                        Map.of("status", "ACTIVE"));

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLog).isSameAs(auditLogCaptor.getValue());
        assertThat(auditLog.getAction()).isEqualTo("LAUNCH");
        assertThat(auditLog.getEntityType()).isEqualTo("campaigns");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(auditLog.getOldValue()).containsEntry("status", "APPROVED");
        assertThat(auditLog.getNewValue()).containsEntry("status", "ACTIVE");
    }

    @Test
    void logsDeleteEventWithOldAndNewValues() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog auditLog =
                auditService.logDelete(
                        ACTOR_ID,
                        "customers",
                        USER_ID,
                        Map.of("deleted", false),
                        Map.of("deleted", true));

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLog).isSameAs(auditLogCaptor.getValue());
        assertThat(auditLog.getAction()).isEqualTo("DELETE");
        assertThat(auditLog.getEntityType()).isEqualTo("customers");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getOldValue()).containsEntry("deleted", false);
        assertThat(auditLog.getNewValue()).containsEntry("deleted", true);
    }

    @Test
    void logsUserDisableEventWithStatusTransition() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog auditLog =
                auditService.logUserDisable(
                        ACTOR_ID,
                        USER_ID,
                        Map.of("status", "ACTIVE"),
                        Map.of("status", "DISABLED"));

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLog).isSameAs(auditLogCaptor.getValue());
        assertThat(auditLog.getAction()).isEqualTo("DISABLE_USER");
        assertThat(auditLog.getEntityType()).isEqualTo("users");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(auditLog.getOldValue()).containsEntry("status", "ACTIVE");
        assertThat(auditLog.getNewValue()).containsEntry("status", "DISABLED");
    }

    @Test
    void rejectsBlankEntityType() {
        assertThatThrownBy(() -> auditService.logCreate(null, " ", USER_ID, Map.of()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Audit validation failed");
    }

    @Test
    void rejectsBlankConsentAuditAction() {
        assertThatThrownBy(
                        () ->
                                auditService.logConsentChange(
                                        ACTOR_ID, " ", USER_ID, null, Map.of("status", "GIVEN")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Audit validation failed");
    }

    @Test
    void logsApprovalEventWithOldAndNewValues() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog auditLog =
                auditService.logApproval(
                        ACTOR_ID,
                        "product_change_requests",
                        USER_ID,
                        Map.of("status", "OPEN"),
                        Map.of("status", "APPROVED"));

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLog).isSameAs(auditLogCaptor.getValue());
        assertThat(auditLog.getAction()).isEqualTo("APPROVE");
        assertThat(auditLog.getEntityType()).isEqualTo("product_change_requests");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getOldValue()).containsEntry("status", "OPEN");
        assertThat(auditLog.getNewValue()).containsEntry("status", "APPROVED");
    }

    @Test
    void logsRejectionEventWithOldAndNewValues() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog auditLog =
                auditService.logRejection(
                        ACTOR_ID,
                        "product_change_requests",
                        USER_ID,
                        Map.of("status", "OPEN"),
                        Map.of("status", "REJECTED"));

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertThat(auditLog).isSameAs(auditLogCaptor.getValue());
        assertThat(auditLog.getAction()).isEqualTo("REJECT");
        assertThat(auditLog.getEntityType()).isEqualTo("product_change_requests");
        assertThat(auditLog.getEntityId()).isEqualTo(USER_ID);
        assertThat(auditLog.getOldValue()).containsEntry("status", "OPEN");
        assertThat(auditLog.getNewValue()).containsEntry("status", "REJECTED");
    }
}
