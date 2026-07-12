package com.bayerwestphalian.campaign.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditLogRepository;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Sprint 16 critical test item <b>658</b>: Audit log is created after consent change.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code NFR-008} — Auditability: sensitive actions are logged
 *   <li>{@code FR-033} — Users can record consent status
 *   <li>{@code COMP-001} / item 524 — consent record and withdrawal write immutable audit rows
 *   <li>Entity type {@code consent_records}; actions {@code CREATE}, {@code WITHDRAW_CONSENT},
 *       and marketing {@code OPT_OUT}
 * </ul>
 *
 * <p>Companion suite (implementation detail / item 524–525): {@link
 * ConsentChangeCreatesAuditLogTests}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("658 Audit log is created after consent change")
class AuditLogIsCreatedAfterConsentChangeTests {

    private static final UUID CONSENT_ID =
            UUID.fromString("22000000-0000-0000-0000-000000000658");
    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000658");
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000658");
    private static final Instant NOW = Instant.parse("2026-07-12T16:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2027-07-12T16:00:00Z");

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ConsentRepository consentRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;

    private ConsentService consentService;

    @BeforeEach
    void setUp() {
        AuditService auditService = new AuditService(auditLogRepository);
        lenient()
                .when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        consentService =
                new ConsentService(
                        consentRepository,
                        customerRepository,
                        userRepository,
                        authorizationExpressions,
                        auditService,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("Record consent → CREATE audit on consent_records")
    class RecordConsentCreatesAudit {

        @Test
        void recordingGivenConsentWritesCreateAuditWithActorAndPayload() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(user()));
            when(consentRepository.save(any(ConsentRecord.class)))
                    .thenAnswer(
                            invocation -> {
                                ConsentRecord record = invocation.getArgument(0);
                                setConsentId(record, CONSENT_ID);
                                return record;
                            });

            ConsentRecordView view =
                    consentService.recordConsent(
                            new RecordConsentCommand(
                                    CUSTOMER_ID,
                                    ConsentType.MARKETING_EMAIL,
                                    ConsentStatus.GIVEN,
                                    "Email marketing consent",
                                    "WEB_FORM",
                                    null,
                                    EXPIRES_AT,
                                    "s3://evidence/email.pdf",
                                    ACTOR_ID));

            assertThat(view.status()).isEqualTo(ConsentStatus.GIVEN);
            assertThat(view.valid()).isTrue();

            AuditLog auditLog = captureSingleSavedAuditLog();
            assertThat(auditLog.getAction()).isEqualTo("CREATE");
            assertThat(auditLog.getEntityType()).isEqualTo(ConsentService.AUDIT_ENTITY_TYPE);
            assertThat(auditLog.getEntityType()).isEqualTo("consent_records");
            assertThat(auditLog.getEntityId()).isEqualTo(CONSENT_ID);
            assertThat(auditLog.getActorUserId()).isEqualTo(ACTOR_ID);
            assertThat(auditLog.getOldValue()).isNull();
            assertThat(auditLog.getNewValue())
                    .containsEntry("id", CONSENT_ID.toString())
                    .containsEntry("customerId", CUSTOMER_ID)
                    .containsEntry("consentType", "MARKETING_EMAIL")
                    .containsEntry("status", "GIVEN")
                    .containsEntry("purpose", "Email marketing consent")
                    .containsEntry("source", "WEB_FORM")
                    .containsEntry("grantedAt", NOW)
                    .containsEntry("expiresAt", EXPIRES_AT)
                    .containsEntry("createdBy", ACTOR_ID);
        }

        @Test
        void recordingRejectedMarketingConsentWritesCreateAndOptOutAudits() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(user()));
            when(consentRepository.save(any(ConsentRecord.class)))
                    .thenAnswer(
                            invocation -> {
                                ConsentRecord record = invocation.getArgument(0);
                                setConsentId(record, CONSENT_ID);
                                return record;
                            });

            consentService.recordConsent(
                    new RecordConsentCommand(
                            CUSTOMER_ID,
                            ConsentType.MARKETING_SMS,
                            ConsentStatus.REJECTED,
                            "SMS marketing opt-out",
                            "CUSTOMER_REQUEST",
                            null,
                            null,
                            null,
                            ACTOR_ID));

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository, org.mockito.Mockito.times(2)).save(captor.capture());
            List<AuditLog> logs = captor.getAllValues();
            assertThat(logs).extracting(AuditLog::getAction).containsExactlyInAnyOrder("CREATE", "OPT_OUT");
            assertThat(logs)
                    .allSatisfy(
                            log -> {
                                assertThat(log.getEntityType()).isEqualTo("consent_records");
                                assertThat(log.getEntityId()).isEqualTo(CONSENT_ID);
                                assertThat(log.getActorUserId()).isEqualTo(ACTOR_ID);
                            });
        }
    }

    @Nested
    @DisplayName("Withdraw consent → WITHDRAW_CONSENT audit on consent_records")
    class WithdrawConsentCreatesAudit {

        @Test
        void withdrawingConsentWritesWithdrawAuditWithBeforeAndAfterPayloads() throws Exception {
            ConsentRecord consentRecord = givenGrantedSmsConsent();
            setConsentId(consentRecord, CONSENT_ID);
            when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);
            when(consentRepository.findById(CONSENT_ID)).thenReturn(Optional.of(consentRecord));
            when(consentRepository.save(consentRecord)).thenReturn(consentRecord);

            ConsentRecordView view =
                    consentService.withdrawConsent(new WithdrawConsentCommand(CONSENT_ID, null));

            assertThat(view.status()).isEqualTo(ConsentStatus.WITHDRAWN);
            assertThat(view.valid()).isFalse();

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository, org.mockito.Mockito.times(2)).save(captor.capture());
            AuditLog withdrawLog =
                    captor.getAllValues().stream()
                            .filter(log -> "WITHDRAW_CONSENT".equals(log.getAction()))
                            .findFirst()
                            .orElseThrow();
            assertThat(withdrawLog.getEntityType()).isEqualTo("consent_records");
            assertThat(withdrawLog.getEntityId()).isEqualTo(CONSENT_ID);
            assertThat(withdrawLog.getActorUserId()).isEqualTo(ACTOR_ID);
            assertThat(withdrawLog.getOldValue())
                    .containsEntry("status", "GIVEN")
                    .containsEntry("consentType", "MARKETING_SMS")
                    .containsEntry("customerId", CUSTOMER_ID);
            assertThat(withdrawLog.getNewValue())
                    .containsEntry("status", "WITHDRAWN")
                    .containsEntry("withdrawnAt", NOW);
            assertThat(captor.getAllValues())
                    .extracting(AuditLog::getAction)
                    .contains("OPT_OUT");
        }
    }

    @Nested
    @DisplayName("Failed consent changes do not write audit rows")
    class FailuresDoNotAudit {

        @Test
        void invalidRecordCommandDoesNotWriteAudit() {
            assertThatThrownBy(() -> consentService.recordConsent(null))
                    .isInstanceOf(ValidationException.class);

            verify(consentRepository, never()).save(any(ConsentRecord.class));
            verify(auditLogRepository, never()).save(any(AuditLog.class));
        }

        @Test
        void missingConsentOnWithdrawDoesNotWriteAudit() {
            when(consentRepository.findById(CONSENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                            () ->
                                    consentService.withdrawConsent(
                                            new WithdrawConsentCommand(CONSENT_ID, null)))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(consentRepository, never()).save(any(ConsentRecord.class));
            verify(auditLogRepository, never()).save(any(AuditLog.class));
        }
    }

    private AuditLog captureSingleSavedAuditLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    private ConsentRecord givenGrantedSmsConsent() throws Exception {
        Customer customer = customer();
        User createdBy = user();
        ConsentRecord consentRecord =
                ConsentRecord.create(
                        customer,
                        ConsentType.MARKETING_SMS,
                        ConsentStatus.REQUIRED,
                        "SMS marketing consent",
                        "WEB_FORM");
        consentRecord.grant(NOW, EXPIRES_AT, "s3://evidence/sms.pdf", createdBy);
        return consentRecord;
    }

    private Customer customer() throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Policyholder");
        setId(customer, CUSTOMER_ID);
        return customer;
    }

    private User user() throws Exception {
        User user = User.create("agent@bayer-westphalian.test", "$2a$10$hash", "Service Agent");
        setId(user, ACTOR_ID);
        return user;
    }

    private static void setConsentId(ConsentRecord consentRecord, UUID id) throws Exception {
        Field idField = ConsentRecord.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(consentRecord, id);
    }

    private static void setId(BaseEntity entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
