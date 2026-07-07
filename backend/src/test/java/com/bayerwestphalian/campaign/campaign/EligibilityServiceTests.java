package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000101");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000101");
    private static final Instant NOW = Instant.parse("2026-07-06T12:00:00Z");

    @Mock private ConsentService consentService;

    @Mock private CustomerRepository customerRepository;

    @Mock private JdbcTemplate jdbcTemplate;

    private EligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        eligibilityService =
                new EligibilityService(
                        consentService,
                        customerRepository,
                        jdbcTemplate,
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        3);
    }

    @Test
    void serviceMethodsDeclareMethodLevelAuthorization() throws Exception {
        assertPreAuthorize(
                "evaluateCustomer", UUID.class, UUID.class, ConsentType.class, boolean.class);
        assertPreAuthorize("isCommunicationEligible", UUID.class, UUID.class);
        assertPreAuthorize(
                "excludeInvalidConsent", UUID.class, ConsentType.class, boolean.class);
        assertPreAuthorize("excludeOptOuts", UUID.class);
        assertPreAuthorize("excludeDuplicateContacts", UUID.class, UUID.class);
        assertPreAuthorize("checkMonthlyLimit", UUID.class);
        assertPreAuthorize("checkMonthlyLimit", UUID.class, int.class);
    }

    @Test
    void eligibleCustomerPassesAllKbRules() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(2);

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, true);

        assertThat(decision.eligible()).isTrue();
        assertThat(decision.exclusionReason()).isNull();
    }

    @Test
    void exclusionReasonGenerationUsesStableKbCodesAndExplanations() {
        EligibilityDecision decision =
                EligibilityDecision.excluded(EligibilityExclusionReason.INVALID_CONSENT);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("INVALID_CONSENT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer does not have valid required consent");
        assertThat(EligibilityExclusionReason.values())
                .extracting(EligibilityExclusionReason::code)
                .containsExactly(
                        EligibilityExclusionReason.CODE_DO_NOT_CONTACT,
                        EligibilityExclusionReason.CODE_MARKETING_OPT_OUT,
                        EligibilityExclusionReason.CODE_INVALID_CONSENT,
                        EligibilityExclusionReason.CODE_DUPLICATE_CAMPAIGN_RECIPIENT,
                        EligibilityExclusionReason.CODE_MONTHLY_CONTACT_LIMIT);
        assertThat(EligibilityExclusionReason.DO_NOT_CONTACT.code())
                .isEqualTo(EligibilityExclusionReason.CODE_DO_NOT_CONTACT);
        assertThat(EligibilityExclusionReason.MARKETING_OPT_OUT.code())
                .isEqualTo(EligibilityExclusionReason.CODE_MARKETING_OPT_OUT);
        assertThat(EligibilityExclusionReason.INVALID_CONSENT.code())
                .isEqualTo(EligibilityExclusionReason.CODE_INVALID_CONSENT);
        assertThat(EligibilityExclusionReason.DUPLICATE_CAMPAIGN_RECIPIENT.code())
                .isEqualTo(EligibilityExclusionReason.CODE_DUPLICATE_CAMPAIGN_RECIPIENT);
        assertThat(EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT.code())
                .isEqualTo(EligibilityExclusionReason.CODE_MONTHLY_CONTACT_LIMIT);
    }

    @Test
    void communicationEligibilityUsesCampaignChannelAndGuardianRequirement() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                .thenReturn("EMAIL");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(true);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(1);

        assertThat(eligibilityService.isCommunicationEligible(CUSTOMER_ID, CAMPAIGN_ID))
                .isTrue();
    }

    @Test
    void minorBeneficiaryWithoutGuardianConsentIsExcludedFromCampaignEligibility() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                .thenReturn("EMAIL");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(true);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true))
                .thenReturn(false);

        assertThat(eligibilityService.isCommunicationEligible(CUSTOMER_ID, CAMPAIGN_ID))
                .isFalse();
        verify(consentService)
                .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true);
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CAMPAIGN_ID),
                        eq(CUSTOMER_ID));
    }

    @Test
    void beneficiaryWithValidGuardianConsentContinuesEligibilityChecks() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(1);

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, true);

        assertThat(decision.eligible()).isTrue();
        assertThat(decision.exclusionReason()).isNull();
        verify(consentService)
                .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true);
        verify(jdbcTemplate)
                .queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CAMPAIGN_ID),
                        eq(CUSTOMER_ID));
        verify(jdbcTemplate)
                .queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class));
    }

    @Test
    void communicationEligibilityReturnsFalseWhenCampaignRuleExcludesCustomer() {
        Customer customer = customer();
        customer.markDoNotContact();
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                .thenReturn("SMS");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        assertThat(eligibilityService.isCommunicationEligible(CUSTOMER_ID, CAMPAIGN_ID))
                .isFalse();
        verify(consentService, never())
                .isCommunicationEligible(any(UUID.class), any(ConsentType.class), anyBoolean());
    }

    @Test
    void communicationEligibilityRejectsMissingOrUnknownCampaign() {
        assertThatThrownBy(
                        () -> eligibilityService.isCommunicationEligible(CUSTOMER_ID, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Eligibility validation failed");
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertThatThrownBy(
                        () -> eligibilityService.isCommunicationEligible(CUSTOMER_ID, CAMPAIGN_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Campaign");
    }

    @Test
    void communicationEligibilityRejectsUnsupportedCampaignChannel() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                .thenReturn("PUSH");

        assertThatThrownBy(
                        () -> eligibilityService.isCommunicationEligible(CUSTOMER_ID, CAMPAIGN_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Eligibility validation failed");
    }

    @Test
    void excludesDoNotContactCustomerBeforeConsentOrContactChecks() {
        Customer customer = customer();
        customer.markDoNotContact();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("DO_NOT_CONTACT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer has do-not-contact enabled");
        verifyNoInteractions(consentService);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void excludesMarketingOptOuts() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(true);

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_SMS, false);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("MARKETING_OPT_OUT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer has withdrawn or rejected marketing consent");
        verify(consentService, never())
                .isCommunicationEligible(any(UUID.class), any(ConsentType.class), anyBoolean());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void customerWithWithdrawnConsentIsExcludedBeforeContactHistoryChecks() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(true);

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason())
                .isEqualTo(EligibilityExclusionReason.CODE_MARKETING_OPT_OUT);
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer has withdrawn or rejected marketing consent");
        verify(consentService, never())
                .isCommunicationEligible(any(UUID.class), any(ConsentType.class), anyBoolean());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void excludesInvalidOrMissingConsentIncludingGuardianConsent() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_SMS, true))
                .thenReturn(false);

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_SMS, true);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("INVALID_CONSENT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer does not have valid required consent");
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void excludesCustomerWithoutValidMarketingConsentBeforeContactHistoryChecks() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                .thenReturn(false);

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("INVALID_CONSENT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer does not have valid required consent");
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void excludesDuplicateCampaignRecipient() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID)))
                .thenReturn(1);

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("DUPLICATE_CAMPAIGN_RECIPIENT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer is already assigned to this campaign");
    }

    @Test
    void excludesCustomerAtMonthlyContactLimit() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        CUSTOMER_ID, ConsentType.MARKETING_PHONE, false))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(3);

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_PHONE, false);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("MONTHLY_CONTACT_LIMIT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer has reached the monthly marketing contact limit");
    }

    @Test
    void exposesIndividualKbRuleChecks() {
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(true);
        when(consentService.isCommunicationEligible(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                .thenReturn(false);
        when(jdbcTemplate.queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID)))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(4);

        assertThat(eligibilityService.excludeOptOuts(CUSTOMER_ID)).isTrue();
        assertThat(
                        eligibilityService.excludeInvalidConsent(
                                CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                .isTrue();
        assertThat(eligibilityService.excludeDuplicateContacts(CAMPAIGN_ID, CUSTOMER_ID)).isTrue();
        assertThat(eligibilityService.checkMonthlyLimit(CUSTOMER_ID, 3)).isTrue();
    }

    @Test
    void validatesInputsAndMissingCustomer() {
        assertThatThrownBy(
                        () ->
                                eligibilityService.evaluateCustomer(
                                        null, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Eligibility validation failed");
        assertThatThrownBy(() -> eligibilityService.checkMonthlyLimit(CUSTOMER_ID, 0))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Eligibility validation failed");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                eligibilityService.evaluateCustomer(
                                        CUSTOMER_ID,
                                        CAMPAIGN_ID,
                                        ConsentType.MARKETING_EMAIL,
                                        false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");
    }

    private static void assertPreAuthorize(String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = EligibilityService.class.getMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    private static Customer customer() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Anna", "Keller");
        try {
            Field id = BaseEntity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(customer, CUSTOMER_ID);
            return customer;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
