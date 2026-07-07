package com.bayerwestphalian.campaign.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.user.User;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTests {

    private static final UUID CONSENT_ID =
            UUID.fromString("22000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000101");
    private static final UUID USER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000101");
    private static final Instant NOW = Instant.parse("2026-07-06T12:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2027-07-06T12:00:00Z");

    @Mock private ConsentRepository consentRepository;

    @Mock private CustomerRepository customerRepository;

    @Mock private UserRepository userRepository;

    @Mock private AuditService auditService;

    private ConsentService consentService;

    @BeforeEach
    void setUp() {
        consentService =
                new ConsentService(
                        consentRepository,
                        customerRepository,
                        userRepository,
                        auditService,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void serviceMethodsDeclareMethodLevelAuthorization() throws Exception {
        assertPreAuthorize("recordConsent", RecordConsentCommand.class);
        assertPreAuthorize("withdrawConsent", WithdrawConsentCommand.class);
        assertPreAuthorize("listConsents", ConsentSearchCriteria.class);
        assertPreAuthorize("getConsentStatus", UUID.class, ConsentType.class);
        assertPreAuthorize("hasValidMarketingConsent", UUID.class);
        assertPreAuthorize("hasValidMarketingConsent", UUID.class, ConsentType.class);
        assertPreAuthorize("hasValidGuardianConsent", UUID.class);
        assertPreAuthorize("hasMarketingOptOut", UUID.class);
        assertPreAuthorize("isGuardianConsentSatisfied", UUID.class, boolean.class);
        assertPreAuthorize("validateGuardianConsent", UUID.class, boolean.class);
        assertPreAuthorize("isCommunicationEligible", UUID.class, ConsentType.class);
        assertPreAuthorize(
                "isCommunicationEligible", UUID.class, ConsentType.class, boolean.class);
    }

    @Test
    void recordsGivenConsentAndAuditsCreation() throws Exception {
        Customer customer = customer();
        User createdBy = user();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createdBy));
        when(consentRepository.save(any(ConsentRecord.class)))
                .thenAnswer(
                        invocation -> {
                            ConsentRecord consentRecord = invocation.getArgument(0);
                            setConsentId(consentRecord, CONSENT_ID);
                            return consentRecord;
                        });

        ConsentRecordView view =
                consentService.recordConsent(
                        new RecordConsentCommand(
                                CUSTOMER_ID,
                                ConsentType.MARKETING_EMAIL,
                                ConsentStatus.GIVEN,
                                " Email marketing consent ",
                                " WEB_FORM ",
                                null,
                                EXPIRES_AT,
                                " s3://evidence/email.pdf ",
                                USER_ID));

        ArgumentCaptor<ConsentRecord> consentCaptor =
                ArgumentCaptor.forClass(ConsentRecord.class);
        verify(consentRepository).save(consentCaptor.capture());
        ConsentRecord savedConsent = consentCaptor.getValue();
        assertThat(savedConsent.getCustomer()).isSameAs(customer);
        assertThat(savedConsent.getConsentType()).isEqualTo(ConsentType.MARKETING_EMAIL);
        assertThat(savedConsent.getStatus()).isEqualTo(ConsentStatus.GIVEN);
        assertThat(savedConsent.getPurpose()).isEqualTo("Email marketing consent");
        assertThat(savedConsent.getSource()).isEqualTo("WEB_FORM");
        assertThat(savedConsent.getGrantedAt()).isEqualTo(NOW);
        assertThat(savedConsent.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(savedConsent.getEvidenceFileUrl()).isEqualTo("s3://evidence/email.pdf");
        assertThat(savedConsent.getCreatedBy()).isSameAs(createdBy);
        assertThat(view.valid()).isTrue();
        assertThat(view.requiresAction()).isFalse();
        verify(auditService)
                .logConsentCreation(
                        eq(USER_ID),
                        eq(CONSENT_ID),
                        eq(
                                Map.ofEntries(
                                        Map.entry("customerId", CUSTOMER_ID),
                                        Map.entry("consentType", "MARKETING_EMAIL"),
                                        Map.entry("status", "GIVEN"),
                                        Map.entry("purpose", "Email marketing consent"),
                                        Map.entry("source", "WEB_FORM"),
                                        Map.entry("grantedAt", NOW),
                                        Map.entry("expiresAt", EXPIRES_AT),
                                        Map.entry("evidenceFileUrl", "s3://evidence/email.pdf"),
                                        Map.entry("createdBy", USER_ID))));
    }

    @Test
    void recordsRequiredConsentAndAuditsCreation() throws Exception {
        Customer customer = customer();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(consentRepository.save(any(ConsentRecord.class)))
                .thenAnswer(
                        invocation -> {
                            ConsentRecord consentRecord = invocation.getArgument(0);
                            setConsentId(consentRecord, CONSENT_ID);
                            return consentRecord;
                        });

        ConsentRecordView view =
                consentService.recordConsent(
                        new RecordConsentCommand(
                                CUSTOMER_ID,
                                ConsentType.GUARDIAN,
                                ConsentStatus.REQUIRED,
                                " Guardian consent required ",
                                " PHONE ",
                                null,
                                null,
                                null,
                                null));

        assertThat(view.status()).isEqualTo(ConsentStatus.REQUIRED);
        assertThat(view.valid()).isFalse();
        assertThat(view.requiresAction()).isTrue();
        verify(auditService)
                .logConsentCreation(
                        eq((UUID) null),
                        eq(CONSENT_ID),
                        eq(
                                Map.ofEntries(
                                        Map.entry("customerId", CUSTOMER_ID),
                                        Map.entry("consentType", "GUARDIAN"),
                                        Map.entry("status", "REQUIRED"),
                                        Map.entry("purpose", "Guardian consent required"),
                                        Map.entry("source", "PHONE"))));
    }

    @Test
    void consentChangeToRejectedMarketingOptOutCreatesAuditLog() throws Exception {
        Customer customer = customer();
        User createdBy = user();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createdBy));
        when(consentRepository.save(any(ConsentRecord.class)))
                .thenAnswer(
                        invocation -> {
                            ConsentRecord consentRecord = invocation.getArgument(0);
                            setConsentId(consentRecord, CONSENT_ID);
                            return consentRecord;
                        });

        ConsentRecordView view =
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
                                USER_ID));

        assertThat(view.status()).isEqualTo(ConsentStatus.REJECTED);
        assertThat(view.valid()).isFalse();
        verify(auditService)
                .logConsentCreation(
                        eq(USER_ID),
                        eq(CONSENT_ID),
                        eq(
                                Map.ofEntries(
                                        Map.entry("customerId", CUSTOMER_ID),
                                        Map.entry("consentType", "MARKETING_EMAIL"),
                                        Map.entry("status", "REJECTED"),
                                        Map.entry("purpose", "Marketing opt-out"),
                                        Map.entry("source", "CUSTOMER_REQUEST"))));
    }

    @Test
    void withdrawsConsentAndAuditsUpdate() throws Exception {
        ConsentRecord consentRecord = givenConsent();
        setConsentId(consentRecord, CONSENT_ID);
        when(consentRepository.findById(CONSENT_ID)).thenReturn(Optional.of(consentRecord));
        when(consentRepository.save(consentRecord)).thenReturn(consentRecord);

        ConsentRecordView view =
                consentService.withdrawConsent(new WithdrawConsentCommand(CONSENT_ID, null));

        assertThat(consentRecord.getStatus()).isEqualTo(ConsentStatus.WITHDRAWN);
        assertThat(consentRecord.getWithdrawnAt()).isEqualTo(NOW);
        assertThat(view.valid()).isFalse();
        assertThat(view.requiresAction()).isTrue();
        ArgumentCaptor<Map> oldValueCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> newValueCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logConsentWithdrawal(
                        eq((UUID) null),
                        eq(CONSENT_ID),
                        oldValueCaptor.capture(),
                        newValueCaptor.capture());
        assertThat(oldValueCaptor.getValue())
                .containsEntry("customerId", CUSTOMER_ID)
                .containsEntry("consentType", "MARKETING_SMS")
                .containsEntry("status", "GIVEN")
                .containsEntry("grantedAt", NOW)
                .containsEntry("expiresAt", EXPIRES_AT)
                .containsEntry("evidenceFileUrl", "s3://evidence/sms.pdf")
                .containsEntry("createdBy", USER_ID);
        assertThat(newValueCaptor.getValue())
                .containsEntry("customerId", CUSTOMER_ID)
                .containsEntry("consentType", "MARKETING_SMS")
                .containsEntry("status", "WITHDRAWN")
                .containsEntry("grantedAt", NOW)
                .containsEntry("withdrawnAt", NOW)
                .containsEntry("expiresAt", EXPIRES_AT)
                .containsEntry("evidenceFileUrl", "s3://evidence/sms.pdf")
                .containsEntry("createdBy", USER_ID);
    }

    @Test
    void listsAndFiltersConsentHistory() {
        ConsentRecord validMarketing = givenConsent();
        ConsentRecord requiredGuardian =
                ConsentRecord.create(
                        customer(),
                        ConsentType.GUARDIAN,
                        ConsentStatus.REQUIRED,
                        "Guardian consent",
                        "PHONE");
        when(consentRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(validMarketing, requiredGuardian));

        List<ConsentRecordView> views =
                consentService.listConsents(
                        new ConsentSearchCriteria(
                                CUSTOMER_ID, ConsentType.MARKETING_SMS, null, true));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).consentType()).isEqualTo(ConsentType.MARKETING_SMS);
        assertThat(views.get(0).valid()).isTrue();
    }

    @Test
    void returnsLatestConsentStatusForCustomerAndType() {
        ConsentRecord consentRecord = givenConsent();
        when(consentRepository.findLatestByType(CUSTOMER_ID, ConsentType.MARKETING_SMS))
                .thenReturn(Optional.of(consentRecord));

        Optional<ConsentRecordView> view =
                consentService.getConsentStatus(CUSTOMER_ID, ConsentType.MARKETING_SMS);

        assertThat(view).isPresent();
        assertThat(view.orElseThrow().status()).isEqualTo(ConsentStatus.GIVEN);
    }

    @Test
    void checksMarketingGuardianAndCommunicationEligibility() {
        when(consentRepository.findOptOuts(CUSTOMER_ID)).thenReturn(List.of());
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, NOW))
                .thenReturn(Optional.empty());
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.MARKETING_PHONE, NOW))
                .thenReturn(Optional.empty());
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.MARKETING_SMS, NOW))
                .thenReturn(Optional.of(givenConsent()));
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.GUARDIAN, NOW))
                .thenReturn(Optional.of(givenConsent()));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));

        assertThat(consentService.hasValidMarketingConsent(CUSTOMER_ID)).isTrue();
        assertThat(
                        consentService.hasValidMarketingConsent(
                                CUSTOMER_ID, ConsentType.MARKETING_SMS))
                .isTrue();
        assertThat(consentService.hasValidGuardianConsent(CUSTOMER_ID)).isTrue();
        assertThat(consentService.isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_SMS))
                .isTrue();
    }

    @Test
    void validMarketingConsentIsFalseWhenNoMarketingChannelHasValidConsent() {
        when(consentRepository.findOptOuts(CUSTOMER_ID)).thenReturn(List.of());
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, NOW))
                .thenReturn(Optional.empty());
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.MARKETING_PHONE, NOW))
                .thenReturn(Optional.empty());
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.MARKETING_SMS, NOW))
                .thenReturn(Optional.empty());

        assertThat(consentService.hasValidMarketingConsent(CUSTOMER_ID)).isFalse();
        verify(consentRepository, never())
                .findValidConsent(CUSTOMER_ID, ConsentType.GUARDIAN, NOW);
    }

    @Test
    void communicationEligibilityIsFalseWhenMarketingChannelConsentIsMissing() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentRepository.findOptOuts(CUSTOMER_ID)).thenReturn(List.of());
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, NOW))
                .thenReturn(Optional.empty());

        assertThat(
                        consentService.isCommunicationEligible(
                                CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .isFalse();
    }

    @Test
    void validMarketingConsentStopsAfterFirstValidMarketingChannel() {
        when(consentRepository.findOptOuts(CUSTOMER_ID)).thenReturn(List.of());
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, NOW))
                .thenReturn(Optional.of(givenConsent()));

        assertThat(consentService.hasValidMarketingConsent(CUSTOMER_ID)).isTrue();
        verify(consentRepository, never())
                .findValidConsent(CUSTOMER_ID, ConsentType.MARKETING_PHONE, NOW);
        verify(consentRepository, never())
                .findValidConsent(CUSTOMER_ID, ConsentType.MARKETING_SMS, NOW);
    }

    @Test
    void validMarketingConsentRejectsMissingCustomerAndNonMarketingType() {
        assertThatThrownBy(() -> consentService.hasValidMarketingConsent(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Consent validation failed");
        assertThatThrownBy(
                        () ->
                                consentService.hasValidMarketingConsent(
                                        CUSTOMER_ID, ConsentType.GUARDIAN))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Consent validation failed");
    }

    @Test
    void marketingOptOutBlocksMarketingConsentAndCommunicationEligibility() {
        ConsentRecord optOut =
                ConsentRecord.create(
                        customer(),
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.REQUIRED,
                        "Email marketing consent",
                        "PHONE");
        optOut.withdraw(NOW);
        when(consentRepository.findOptOuts(CUSTOMER_ID)).thenReturn(List.of(optOut));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));

        assertThat(consentService.hasValidMarketingConsent(CUSTOMER_ID)).isFalse();
        assertThat(
                        consentService.hasValidMarketingConsent(
                                CUSTOMER_ID, ConsentType.MARKETING_SMS))
                .isFalse();
        assertThat(consentService.isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_SMS))
                .isFalse();
        verify(consentRepository, never()).findValidConsent(any(), any(), any());
    }

    @Test
    void customerWhoOptedOutWithRejectedMarketingConsentIsExcludedFromMarketing() {
        ConsentRecord optOut =
                ConsentRecord.create(
                        customer(),
                        ConsentType.MARKETING_SMS,
                        ConsentStatus.REQUIRED,
                        "SMS marketing consent",
                        "CUSTOMER_REQUEST");
        optOut.reject();
        when(consentRepository.findOptOuts(CUSTOMER_ID)).thenReturn(List.of(optOut));
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));

        assertThat(consentService.hasMarketingOptOut(CUSTOMER_ID)).isTrue();
        assertThat(consentService.hasValidMarketingConsent(CUSTOMER_ID)).isFalse();
        assertThat(consentService.isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_SMS))
                .isFalse();
        verify(consentRepository, never()).findValidConsent(any(), any(), any());
    }

    @Test
    void marketingOptOutDoesNotBlockGuardianConsentChecks() {
        ConsentRecord optOut =
                ConsentRecord.create(
                        customer(),
                        ConsentType.MARKETING_EMAIL,
                        ConsentStatus.REQUIRED,
                        "Email marketing consent",
                        "PHONE");
        optOut.withdraw(NOW);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.GUARDIAN, NOW))
                .thenReturn(Optional.of(givenConsent()));

        assertThat(consentService.hasValidGuardianConsent(CUSTOMER_ID)).isTrue();
        assertThat(consentService.isCommunicationEligible(CUSTOMER_ID, ConsentType.GUARDIAN))
                .isTrue();
    }

    @Test
    void validGuardianConsentIsFalseWhenNoValidGuardianConsentExists() {
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.GUARDIAN, NOW))
                .thenReturn(Optional.empty());

        assertThat(consentService.hasValidGuardianConsent(CUSTOMER_ID)).isFalse();
    }

    @Test
    void validGuardianConsentRejectsMissingCustomerId() {
        assertThatThrownBy(() -> consentService.hasValidGuardianConsent(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Consent validation failed");
        verify(consentRepository, never()).findValidConsent(any(), any(), any());
    }

    @Test
    void guardianConsentIsSatisfiedWhenNotRequired() {
        assertThat(consentService.isGuardianConsentSatisfied(CUSTOMER_ID, false)).isTrue();
        verify(consentRepository, never()).findValidConsent(any(), any(), any());
    }

    @Test
    void guardianConsentValidationRequiresValidGuardianConsentWhenRequired() {
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.GUARDIAN, NOW))
                .thenReturn(Optional.empty());

        assertThat(consentService.isGuardianConsentSatisfied(CUSTOMER_ID, true)).isFalse();
        assertThatThrownBy(() -> consentService.validateGuardianConsent(CUSTOMER_ID, true))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Consent validation failed");
    }

    @Test
    void guardianConsentValidationPassesWhenRequiredConsentIsValid() {
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.GUARDIAN, NOW))
                .thenReturn(Optional.of(givenConsent()));

        assertThat(consentService.isGuardianConsentSatisfied(CUSTOMER_ID, true)).isTrue();
        consentService.validateGuardianConsent(CUSTOMER_ID, true);
    }

    @Test
    void guardianConsentRequirementBlocksCommunicationEligibilityWhenMissing() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentRepository.findOptOuts(CUSTOMER_ID)).thenReturn(List.of());
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.MARKETING_SMS, NOW))
                .thenReturn(Optional.of(givenConsent()));
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.GUARDIAN, NOW))
                .thenReturn(Optional.empty());

        assertThat(
                        consentService.isCommunicationEligible(
                                CUSTOMER_ID, ConsentType.MARKETING_SMS, true))
                .isFalse();
    }

    @Test
    void guardianConsentRequirementAllowsCommunicationEligibilityWhenValid() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentRepository.findOptOuts(CUSTOMER_ID)).thenReturn(List.of());
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.MARKETING_SMS, NOW))
                .thenReturn(Optional.of(givenConsent()));
        when(consentRepository.findValidConsent(CUSTOMER_ID, ConsentType.GUARDIAN, NOW))
                .thenReturn(Optional.of(givenConsent()));

        assertThat(
                        consentService.isCommunicationEligible(
                                CUSTOMER_ID, ConsentType.MARKETING_SMS, true))
                .isTrue();
    }

    @Test
    void doNotContactCustomerIsNeverCommunicationEligible() {
        Customer customer = customer();
        customer.markDoNotContact();

        assertThat(consentService.isCommunicationEligible(customer, ConsentType.MARKETING_EMAIL))
                .isFalse();
        verify(consentRepository, never()).findValidConsent(any(), any(), any());
    }

    @Test
    void doNotContactLoadedCustomerIsNeverCommunicationEligible() {
        Customer customer = customer();
        customer.markDoNotContact();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        assertThat(consentService.isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_SMS))
                .isFalse();
        verify(consentRepository, never()).findValidConsent(any(), any(), any());
    }

    @Test
    void validatesRequiredCommandsAndMissingResources() {
        assertThatThrownBy(() -> consentService.recordConsent(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Consent validation failed");
        assertThatThrownBy(() -> consentService.withdrawConsent(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Consent validation failed");
        assertThatThrownBy(
                        () ->
                                consentService.recordConsent(
                                        new RecordConsentCommand(
                                                null,
                                                null,
                                                null,
                                                " ",
                                                null,
                                                NOW,
                                                EXPIRES_AT,
                                                null,
                                                null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Consent validation failed");
        assertThatThrownBy(
                        () ->
                                consentService.recordConsent(
                                        new RecordConsentCommand(
                                                CUSTOMER_ID,
                                                ConsentType.MARKETING_EMAIL,
                                                ConsentStatus.GIVEN,
                                                "Consent",
                                                null,
                                                EXPIRES_AT,
                                                NOW,
                                                null,
                                                null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Consent validation failed");

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                consentService.recordConsent(
                                        new RecordConsentCommand(
                                                CUSTOMER_ID,
                                                ConsentType.MARKETING_EMAIL,
                                                ConsentStatus.REQUIRED,
                                                "Consent",
                                                null,
                                                null,
                                                null,
                                                null,
                                                null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");
    }

    private static void assertPreAuthorize(String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = ConsentService.class.getMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    private static ConsentRecord givenConsent() {
        ConsentRecord consentRecord =
                ConsentRecord.create(
                        customer(),
                        ConsentType.MARKETING_SMS,
                        ConsentStatus.REQUIRED,
                        "SMS marketing consent",
                        "PHONE");
        consentRecord.grant(NOW, EXPIRES_AT, "s3://evidence/sms.pdf", user());
        return consentRecord;
    }

    private static Customer customer() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Anna", "Keller");
        setBaseEntityId(customer, CUSTOMER_ID);
        return customer;
    }

    private static User user() {
        User user =
                User.create(
                        "agent@bayer-westphalian.test",
                        "$2a$10$hashed-password-placeholder",
                        "Customer Service Agent");
        setBaseEntityId(user, USER_ID);
        return user;
    }

    private static void setConsentId(ConsentRecord consentRecord, UUID consentId)
            throws Exception {
        Field id = ConsentRecord.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(consentRecord, consentId);
    }

    private static void setBaseEntityId(Object entity, UUID id) {
        try {
            Field idField = BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
