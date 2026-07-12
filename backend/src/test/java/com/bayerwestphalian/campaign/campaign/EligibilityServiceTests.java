package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.ai.AiRecommendation;
import com.bayerwestphalian.campaign.ai.AiRecommendationType;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.settings.SystemSettingsService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000101");
    private static final UUID CAMPAIGN_ID = UUID.fromString("50000000-0000-0000-0000-000000000101");
    private static final Instant NOW = Instant.parse("2026-07-06T12:00:00Z");

    @Mock private ConsentService consentService;

    @Mock private CustomerRepository customerRepository;

    @Mock private JdbcTemplate jdbcTemplate;

    @Mock private SystemSettingsService systemSettingsService;

    private EligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        lenient().when(systemSettingsService.monthlyContactLimit()).thenReturn(3);
        lenient().when(systemSettingsService.uninterestedExclusionDays()).thenReturn(90);
        eligibilityService =
                new EligibilityService(
                        consentService,
                        customerRepository,
                        jdbcTemplate,
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        systemSettingsService);
    }

    @Test
    void serviceMethodsDeclareMethodLevelAuthorization() throws Exception {
        assertPreAuthorize(
                "evaluateCustomer", UUID.class, UUID.class, ConsentType.class, boolean.class);
        assertPreAuthorize("evaluateForSegmentPreview", UUID.class);
        assertPreAuthorize("evaluateForSegmentPreview", UUID.class, ConsentType.class);
        assertPreAuthorize("evaluateForReminder", UUID.class, ConsentType.class);
        assertPreAuthorize("isCommunicationEligible", UUID.class, UUID.class);
        assertPreAuthorize("excludeInvalidConsent", UUID.class, ConsentType.class, boolean.class);
        assertPreAuthorize("excludeOptOuts", UUID.class);
        assertPreAuthorize("excludeDuplicateContacts", UUID.class, UUID.class);
        assertPreAuthorize("checkMonthlyLimit", UUID.class);
        assertPreAuthorize("checkMonthlyLimit", UUID.class, int.class);
    }

    @Test
    void evaluateForSegmentPreviewExcludesDoNotContactCustomers() {
        Customer blocked = customer();
        blocked.markDoNotContact();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(blocked));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);

        EligibilityDecision decision = eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("DO_NOT_CONTACT");
        verifyNoInteractions(consentService);
    }

    @Test
    void evaluateForSegmentPreviewIncludesEligibleMarketingAudience() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        any(Customer.class), eq(ConsentType.MARKETING_EMAIL), eq(false)))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(0);

        EligibilityDecision decision = eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

        assertThat(decision.eligible()).isTrue();
        assertThat(decision.exclusionReason()).isNull();
    }

    @Test
    void evaluateForSegmentPreviewExcludesMarketingOptOuts() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(true);

        EligibilityDecision decision = eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("MARKETING_OPT_OUT");
    }

    @Test
    void evaluateForSegmentPreviewExcludesInvalidConsent() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        any(Customer.class), eq(ConsentType.MARKETING_EMAIL), eq(false)))
                .thenReturn(false);

        EligibilityDecision decision = eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("INVALID_CONSENT");
    }

    @Test
    void evaluateForSegmentPreviewExcludesMonthlyContactLimit() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        any(Customer.class), eq(ConsentType.MARKETING_EMAIL), eq(false)))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(3);

        EligibilityDecision decision = eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("MONTHLY_CONTACT_LIMIT");
    }

    @Test
    void evaluateForReminderAppliesEligibilityAndMonthlyContactLimitRules() {
        // KB item 401 / BR-011 / FR-092: monthly contact limit blocks reminder eligibility.
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        any(Customer.class), eq(ConsentType.MARKETING_EMAIL), eq(false)))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(3);

        EligibilityDecision decision =
                eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("MONTHLY_CONTACT_LIMIT");
    }

    @Test
    void evaluateForReminderRejectsInvalidConsent() {
        // KB item 401 / FR-034: missing or invalid marketing consent blocks reminder eligibility.
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        any(Customer.class), eq(ConsentType.MARKETING_EMAIL), eq(false)))
                .thenReturn(false);

        EligibilityDecision decision =
                eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("INVALID_CONSENT");
        assertThat(decision.eligibilityExplanation())
                .contains("does not have valid required consent");
    }

    @Test
    void evaluateForReminderRejectsMarketingOptOut() {
        // KB item 401 / BR-002: marketing opt-out blocks reminder eligibility.
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(true);

        EligibilityDecision decision =
                eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("MARKETING_OPT_OUT");
    }

    @Test
    void evaluateForSegmentPreviewDoesNotCheckCampaignDuplicates() {
        // Segment preview is not campaign-scoped: only segment audience rules apply.
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        any(Customer.class), eq(ConsentType.MARKETING_EMAIL), eq(false)))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(0);

        EligibilityDecision decision = eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

        assertThat(decision.eligible()).isTrue();
        // No campaign_recipients count query with campaign id for segment preview path.
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID));
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
                        EligibilityExclusionReason.CODE_UNINTERESTED,
                        EligibilityExclusionReason.CODE_CONVERTED,
                        EligibilityExclusionReason.CODE_MARKETING_OPT_OUT,
                        EligibilityExclusionReason.CODE_INVALID_CONSENT,
                        EligibilityExclusionReason.CODE_DUPLICATE_CAMPAIGN_RECIPIENT,
                        EligibilityExclusionReason.CODE_MONTHLY_CONTACT_LIMIT);
        assertThat(EligibilityExclusionReason.DO_NOT_CONTACT.code())
                .isEqualTo(EligibilityExclusionReason.CODE_DO_NOT_CONTACT);
        assertThat(EligibilityExclusionReason.UNINTERESTED.code())
                .isEqualTo(EligibilityExclusionReason.CODE_UNINTERESTED);
        assertThat(EligibilityExclusionReason.CONVERTED.code())
                .isEqualTo(EligibilityExclusionReason.CODE_CONVERTED);
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

        assertThat(eligibilityService.isCommunicationEligible(CUSTOMER_ID, CAMPAIGN_ID)).isTrue();
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

        assertThat(eligibilityService.isCommunicationEligible(CUSTOMER_ID, CAMPAIGN_ID)).isFalse();
        verify(consentService)
                .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true);
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID));
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
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID));
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

        assertThat(eligibilityService.isCommunicationEligible(CUSTOMER_ID, CAMPAIGN_ID)).isFalse();
        verify(consentService, never())
                .isCommunicationEligible(any(UUID.class), any(ConsentType.class), anyBoolean());
    }

    @Test
    void communicationEligibilityRejectsMissingOrUnknownCampaign() {
        assertThatThrownBy(() -> eligibilityService.isCommunicationEligible(CUSTOMER_ID, null))
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
    void campaignPreviewExcludesDoNotContactCustomer() {
        Customer customer = customer();
        customer.markDoNotContact();
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                .thenReturn("EMAIL");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        EligibilityDecision decision =
                eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("DO_NOT_CONTACT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer has do-not-contact enabled");
        verifyNoInteractions(consentService);
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID));
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class));
    }

    @Test
    void excludesUninterestedCustomerBeforeConsentOrContactChecks() {
        Customer customer = customer();
        customer.changeStatus(CustomerStatus.UNINTERESTED);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("UNINTERESTED");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer is marked as uninterested");
        verifyNoInteractions(consentService);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void segmentPreviewExcludesUninterestedCustomer() {
        Customer customer = customer();
        customer.changeStatus(CustomerStatus.UNINTERESTED);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);

        EligibilityDecision decision = eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("UNINTERESTED");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer is marked as uninterested");
        verifyNoInteractions(consentService);
    }

    @Test
    void campaignPreviewExcludesUninterestedCustomer() {
        Customer customer = customer();
        customer.changeStatus(CustomerStatus.UNINTERESTED);
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                .thenReturn("EMAIL");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        EligibilityDecision decision =
                eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("UNINTERESTED");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer is marked as uninterested");
        verifyNoInteractions(consentService);
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID));
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class));
    }

    @Test
    void excludesConvertedCustomerBeforeConsentOrContactChecks() {
        Customer customer = customer();
        customer.changeStatus(CustomerStatus.CONVERTED);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("CONVERTED");
        assertThat(decision.eligibilityExplanation()).isEqualTo("Customer has already converted");
        verifyNoInteractions(consentService);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void segmentPreviewExcludesConvertedCustomer() {
        Customer customer = customer();
        customer.changeStatus(CustomerStatus.CONVERTED);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);

        EligibilityDecision decision = eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("CONVERTED");
        assertThat(decision.eligibilityExplanation()).isEqualTo("Customer has already converted");
        verifyNoInteractions(consentService);
    }

    @Test
    void campaignPreviewExcludesConvertedCustomerFromSameCampaignProduct() {
        Customer customer = customer();
        customer.changeStatus(CustomerStatus.CONVERTED);
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                .thenReturn("EMAIL");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        EligibilityDecision decision =
                eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("CONVERTED");
        assertThat(decision.eligibilityExplanation()).isEqualTo("Customer has already converted");
        verifyNoInteractions(consentService);
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID));
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class));
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
    void campaignPreviewExcludesCustomerWithMarketingOptOut() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                .thenReturn("SMS");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(true);

        EligibilityDecision decision =
                eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("MARKETING_OPT_OUT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer has withdrawn or rejected marketing consent");
        verify(consentService, never())
                .isCommunicationEligible(any(UUID.class), any(ConsentType.class), anyBoolean());
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID));
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class));
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
    void campaignPreviewExcludesMinorBeneficiaryWithoutGuardianConsent() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                .thenReturn("EMAIL");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(true);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true))
                .thenReturn(false);

        EligibilityDecision decision =
                eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("INVALID_CONSENT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer does not have valid required consent");
        verify(consentService)
                .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true);
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID));
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class));
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
    void aiRecommendationCannotOverrideMissingConsent() {
        AiRecommendation recommendation =
                AiRecommendation.create(
                        AiRecommendationType.PRODUCT,
                        "customer",
                        CUSTOMER_ID,
                        "AI ranked the customer as a product fit",
                        "Prioritize customer for a marketing campaign",
                        "Decision-support recommendation only; consent remains authoritative");
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                .thenReturn(false);

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(recommendation.getRecommendation())
                .isEqualTo("Prioritize customer for a marketing campaign");
        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("INVALID_CONSENT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer does not have valid required consent");
        verify(consentService)
                .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void aiRecommendationCannotBypassDoNotContact() {
        AiRecommendation recommendation =
                AiRecommendation.create(
                        AiRecommendationType.SEGMENT,
                        "customer",
                        CUSTOMER_ID,
                        "AI suggested the customer for a high-fit segment",
                        "Add customer to campaign audience",
                        "Decision-support recommendation only; do-not-contact remains authoritative");
        Customer customer = customer();
        customer.markDoNotContact();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(recommendation.getRecommendation())
                .isEqualTo("Add customer to campaign audience");
        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("DO_NOT_CONTACT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer has do-not-contact enabled");
        verifyNoInteractions(consentService);
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
    void campaignPreviewExcludesCustomerAlreadyAssignedToSameCampaign() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                .thenReturn("EMAIL");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID)))
                .thenReturn(1);

        EligibilityDecision decision =
                eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("DUPLICATE_CAMPAIGN_RECIPIENT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer is already assigned to this campaign");
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class));
    }

    @Test
    void sameCampaignDuplicatePreventionDoesNotBlockDifferentCampaigns() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(0);

        EligibilityDecision decision =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

        assertThat(decision.eligible()).isTrue();
        assertThat(decision.exclusionReason()).isNull();
        verify(jdbcTemplate)
                .queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID));
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
    @DisplayName("557 Configurable monthly contact limit is applied")
    void eligibilityUsesCurrentConfiguredMonthlyContactLimitForEachEvaluation() {
        when(systemSettingsService.monthlyContactLimit()).thenReturn(3, 4);
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

        EligibilityDecision atConfiguredLimit =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_PHONE, false);
        EligibilityDecision belowRaisedConfiguredLimit =
                eligibilityService.evaluateCustomer(
                        CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_PHONE, false);

        assertThat(atConfiguredLimit.eligible()).isFalse();
        assertThat(atConfiguredLimit.exclusionReason()).isEqualTo("MONTHLY_CONTACT_LIMIT");
        assertThat(belowRaisedConfiguredLimit.eligible()).isTrue();
        assertThat(belowRaisedConfiguredLimit.exclusionReason()).isNull();
        verify(systemSettingsService, times(2)).monthlyContactLimit();
    }

    @Test
    void campaignPreviewEligibilityEnforcesMonthlyContactLimit() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                .thenReturn("EMAIL");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
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
                eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("MONTHLY_CONTACT_LIMIT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer has reached the monthly marketing contact limit");
        verify(jdbcTemplate)
                .queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class));
    }

    @Test
    void monthlyContactLimitCountsSentAndCalledEventsInThirtyDayWindow() {
        when(jdbcTemplate.queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class)))
                .thenReturn(3);

        boolean reachedLimit = eligibilityService.checkMonthlyLimit(CUSTOMER_ID, 3);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Timestamp> windowStartCaptor = ArgumentCaptor.forClass(Timestamp.class);
        verify(jdbcTemplate)
                .queryForObject(
                        queryCaptor.capture(),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        windowStartCaptor.capture());
        assertThat(reachedLimit).isTrue();
        assertThat(queryCaptor.getValue())
                .contains("from contact_events")
                .contains("event_type in ('SENT', 'CALLED')")
                .contains("occurred_at >= ?");
        assertThat(windowStartCaptor.getValue())
                .isEqualTo(Timestamp.from(Instant.parse("2026-06-06T12:00:00Z")));
    }

    @Test
    void campaignPreviewExcludesCustomerWithoutConsent() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                .thenReturn("EMAIL");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                .thenReturn(false);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
        when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
        when(consentService.isCommunicationEligible(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                .thenReturn(false);

        EligibilityDecision decision =
                eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("INVALID_CONSENT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer does not have valid required consent");
        verify(consentService)
                .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false);
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class), eq(Integer.class), eq(CAMPAIGN_ID), eq(CUSTOMER_ID));
        verify(jdbcTemplate, never())
                .queryForObject(
                        any(String.class),
                        eq(Integer.class),
                        eq(CUSTOMER_ID),
                        any(Timestamp.class));
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
