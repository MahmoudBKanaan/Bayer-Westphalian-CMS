package com.bayerwestphalian.campaign.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditLog;
import com.bayerwestphalian.campaign.audit.AuditLogRepository;
import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KB item 525: marketing opt-out changes write an immutable {@code OPT_OUT} audit row on entity
 * type {@code consent_records} (COMP-002 / BR-002).
 *
 * <p>Opt-outs are marketing-channel records with status {@code REJECTED} or {@code WITHDRAWN}
 * (recorded directly or via withdraw of a prior grant).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("525 Log opt-out changes")
class OptOutChangeCreatesAuditLogTests {

    private static final UUID CONSENT_ID =
            UUID.fromString("22000000-0000-0000-0000-000000000525");
    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000525");
    private static final UUID ACTOR_ID = UUID.fromString("10000000-0000-0000-0000-000000000525");
    private static final Instant NOW = Instant.parse("2026-07-11T16:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2027-07-11T16:00:00Z");

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

    @Test
    void recordRejectedMarketingConsentWritesCreateAndOptOutAudits() throws Exception {
        Customer customer = customer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(user()));
        stubConsentSave();

        consentService.recordConsent(
                new RecordConsentCommand(
                        CUSTOMER_ID,
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.REJECTED,
                        "Marketing opt-out",
                        "CUSTOMER_REQUEST",
                        null,
                        null,
                        null,
                        ACTOR_ID));

        List<AuditLog> auditLogs = captureSavedAuditLogs(2);
        AuditLog createLog =
                auditLogs.stream()
                        .filter(log -> "CREATE".equals(log.getAction()))
                        .findFirst()
                        .orElseThrow();
        AuditLog optOutLog =
                auditLogs.stream()
                        .filter(log -> "OPT_OUT".equals(log.getAction()))
                        .findFirst()
                        .orElseThrow();

        assertThat(createLog.getEntityType()).isEqualTo(ConsentService.AUDIT_ENTITY_TYPE);
        assertThat(createLog.getEntityId()).isEqualTo(CONSENT_ID);
        assertThat(createLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(createLog.getNewValue()).containsEntry("status", "REJECTED");

        assertThat(optOutLog.getEntityType()).isEqualTo("consent_records");
        assertThat(optOutLog.getEntityId()).isEqualTo(CONSENT_ID);
        assertThat(optOutLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(optOutLog.getOldValue()).isNull();
        assertThat(optOutLog.getNewValue())
                .containsEntry("status", "REJECTED")
                .containsEntry("consentType", "MARKETING_EMAIL")
                .containsEntry("customerId", CUSTOMER_ID)
                .containsEntry("optOut", true)
                .containsEntry("marketingConsent", true)
                .containsEntry("purpose", "Marketing opt-out");
    }

    @Test
    void recordWithdrawnMarketingConsentWritesOptOutAudit() throws Exception {
        Customer customer = customer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(user()));
        stubConsentSave();

        consentService.recordConsent(
                new RecordConsentCommand(
                        CUSTOMER_ID,
                        ConsentType.MARKETING_PHONE,
                        ConsentStatus.WITHDRAWN,
                        "Phone opt-out at intake",
                        "CALL_CENTER",
                        null,
                        null,
                        null,
                        ACTOR_ID));

        List<AuditLog> auditLogs = captureSavedAuditLogs(2);
        assertThat(auditLogs).anyMatch(log -> "OPT_OUT".equals(log.getAction()));
        AuditLog optOutLog =
                auditLogs.stream()
                        .filter(log -> "OPT_OUT".equals(log.getAction()))
                        .findFirst()
                        .orElseThrow();
        assertThat(optOutLog.getNewValue())
                .containsEntry("consentType", "MARKETING_PHONE")
                .containsEntry("status", "WITHDRAWN")
                .containsEntry("optOut", true);
    }

    @Test
    void withdrawMarketingConsentWritesWithdrawAndOptOutAudits() throws Exception {
        ConsentRecord consentRecord = givenMarketingConsent(ConsentType.MARKETING_SMS);
        setConsentId(consentRecord, CONSENT_ID);
        when(authorizationExpressions.currentUserId()).thenReturn(ACTOR_ID);
        when(consentRepository.findById(CONSENT_ID)).thenReturn(Optional.of(consentRecord));
        when(consentRepository.save(consentRecord)).thenReturn(consentRecord);

        consentService.withdrawConsent(new WithdrawConsentCommand(CONSENT_ID, null));

        List<AuditLog> auditLogs = captureSavedAuditLogs(2);
        AuditLog withdrawLog =
                auditLogs.stream()
                        .filter(log -> "WITHDRAW_CONSENT".equals(log.getAction()))
                        .findFirst()
                        .orElseThrow();
        AuditLog optOutLog =
                auditLogs.stream()
                        .filter(log -> "OPT_OUT".equals(log.getAction()))
                        .findFirst()
                        .orElseThrow();

        assertThat(withdrawLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(withdrawLog.getOldValue()).containsEntry("status", "GIVEN");
        assertThat(withdrawLog.getNewValue()).containsEntry("status", "WITHDRAWN");

        assertThat(optOutLog.getAction()).isEqualTo("OPT_OUT");
        assertThat(optOutLog.getEntityId()).isEqualTo(CONSENT_ID);
        assertThat(optOutLog.getActorUserId()).isEqualTo(ACTOR_ID);
        assertThat(optOutLog.getOldValue())
                .containsEntry("status", "GIVEN")
                .containsEntry("optOut", false)
                .containsEntry("marketingConsent", true);
        assertThat(optOutLog.getNewValue())
                .containsEntry("status", "WITHDRAWN")
                .containsEntry("optOut", true)
                .containsEntry("marketingConsent", true)
                .containsEntry("withdrawnAt", NOW);
    }

    @Test
    void recordGivenMarketingConsentDoesNotWriteOptOutAudit() throws Exception {
        Customer customer = customer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(user()));
        stubConsentSave();

        consentService.recordConsent(
                new RecordConsentCommand(
                        CUSTOMER_ID,
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.GIVEN,
                        "Email marketing consent",
                        "WEB_FORM",
                        null,
                        EXPIRES_AT,
                        null,
                        ACTOR_ID));

        List<AuditLog> auditLogs = captureSavedAuditLogs(1);
        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.getFirst().getAction()).isEqualTo("CREATE");
        assertThat(auditLogs).noneMatch(log -> "OPT_OUT".equals(log.getAction()));
    }

    @Test
    void recordRejectedGuardianConsentDoesNotWriteOptOutAudit() throws Exception {
        Customer customer = customer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(user()));
        stubConsentSave();

        consentService.recordConsent(
                new RecordConsentCommand(
                        CUSTOMER_ID,
                        ConsentType.GUARDIAN,
                        ConsentStatus.REJECTED,
                        "Guardian declined",
                        "PHONE",
                        null,
                        null,
                        null,
                        ACTOR_ID));

        List<AuditLog> auditLogs = captureSavedAuditLogs(1);
        assertThat(auditLogs.getFirst().getAction()).isEqualTo("CREATE");
        assertThat(auditLogs).noneMatch(log -> "OPT_OUT".equals(log.getAction()));
    }

    @Test
    void validationFailureDoesNotWriteOptOutAudit() {
        assertThatThrownBy(() -> consentService.recordConsent(null))
                .isInstanceOf(ValidationException.class);

        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    private void stubConsentSave() {
        when(consentRepository.save(any(ConsentRecord.class)))
                .thenAnswer(
                        invocation -> {
                            ConsentRecord record = invocation.getArgument(0);
                            setConsentId(record, CONSENT_ID);
                            return record;
                        });
    }

    private List<AuditLog> captureSavedAuditLogs(int expectedCount) {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(expectedCount)).save(captor.capture());
        return captor.getAllValues();
    }

    private ConsentRecord givenMarketingConsent(ConsentType type) throws Exception {
        Customer customer = customer();
        User createdBy = user();
        ConsentRecord consentRecord =
                ConsentRecord.create(
                        customer, type, ConsentStatus.REQUIRED, "Marketing consent", "WEB_FORM");
        consentRecord.grant(NOW, EXPIRES_AT, "s3://evidence/mkt.pdf", createdBy);
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
