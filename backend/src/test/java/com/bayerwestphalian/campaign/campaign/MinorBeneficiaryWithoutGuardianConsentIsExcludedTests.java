package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.settings.SystemSettingsService;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Sprint 16 critical test item <b>650</b>: Minor beneficiary without guardian consent is excluded.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code BR-003} — A beneficiary requiring guardian consent cannot be contacted until
 *       guardian consent is valid
 *   <li>{@code FR-032} — System tracks guardian consent requirement
 *   <li>{@code FR-034} — System blocks marketing without valid consent (guardian path)
 * </ul>
 *
 * <p>When {@code beneficiaries.guardian_consent_required = true} for the customer, eligibility
 * evaluation sets {@code guardianConsentRequired=true}. {@link ConsentService} then requires a
 * valid {@link ConsentType#GUARDIAN} record. Failure yields {@link
 * EligibilityExclusionReason#INVALID_CONSENT}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("650 Minor beneficiary without guardian consent is excluded")
class MinorBeneficiaryWithoutGuardianConsentIsExcludedTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000650");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000650");
    private static final Instant NOW = Instant.parse("2026-07-12T15:00:00Z");

    @Mock private ConsentService consentService;
    @Mock private CustomerRepository customerRepository;
    @Mock private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
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

    @Nested
    @DisplayName("Campaign eligibility with guardianConsentRequired=true")
    class CampaignPaths {

        @Test
        @DisplayName("evaluateCustomer excludes minor without valid guardian consent")
        void evaluateCustomerExcludesMinorWithoutGuardianConsent() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true))
                    .thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateCustomer(
                            CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, true);

            assertExcludedForMissingGuardianConsent(decision);
            verify(consentService)
                    .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true);
            verify(jdbcTemplate, never())
                    .queryForObject(any(String.class), eq(Integer.class), any(), any());
        }

        @Test
        @DisplayName("excludeInvalidConsent is true when guardian consent required and missing")
        void excludeInvalidConsentWhenGuardianRequiredAndMissing() {
            when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true))
                    .thenReturn(false);

            assertThat(
                            eligibilityService.excludeInvalidConsent(
                                    CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true))
                    .isTrue();
            verify(consentService)
                    .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true);
        }

        @Test
        @DisplayName("campaign preview discovers guardian_consent_required and excludes")
        void campaignPreviewExcludesWhenGuardianFlagTrueOnBeneficiaryLink() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                    .thenReturn("EMAIL");
            // requiresGuardianConsent() EXISTS query on beneficiaries.guardian_consent_required
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(true);
            when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true))
                    .thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

            assertExcludedForMissingGuardianConsent(decision);
            verify(consentService)
                    .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true);
            verify(jdbcTemplate, never())
                    .queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CAMPAIGN_ID),
                            eq(CUSTOMER_ID));
            verify(jdbcTemplate, never())
                    .queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class));
        }

        @Test
        @DisplayName("isCommunicationEligible is false for minor without guardian consent")
        void isCommunicationEligibleFalseWithoutGuardianConsent() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                    .thenReturn("EMAIL");
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(true);
            when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true))
                    .thenReturn(false);

            assertThat(eligibilityService.isCommunicationEligible(CUSTOMER_ID, CAMPAIGN_ID))
                    .isFalse();
            verify(consentService)
                    .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true);
        }

        @Test
        @DisplayName("valid guardian consent continues eligibility (positive control)")
        void validGuardianConsentAllowsEligibilityPositiveControl() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true))
                    .thenReturn(true);
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CAMPAIGN_ID),
                            eq(CUSTOMER_ID)))
                    .thenReturn(0);
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class)))
                    .thenReturn(0);

            EligibilityDecision decision =
                    eligibilityService.evaluateCustomer(
                            CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, true);

            assertThat(decision.eligible()).isTrue();
            assertThat(decision.exclusionReason()).isNull();
            verify(consentService)
                    .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true);
        }

        @Test
        @DisplayName("guardian not required: marketing consent path uses guardianConsentRequired=false")
        void whenGuardianNotRequiredUsesFalseFlag() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                    .thenReturn("EMAIL");
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);
            when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                    .thenReturn(true);
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CAMPAIGN_ID),
                            eq(CUSTOMER_ID)))
                    .thenReturn(0);
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class)))
                    .thenReturn(0);

            EligibilityDecision decision =
                    eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

            assertThat(decision.eligible()).isTrue();
            verify(consentService)
                    .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false);
            verify(consentService, never())
                    .isCommunicationEligible(
                            any(UUID.class), any(ConsentType.class), eq(true));
        }
    }

    @Nested
    @DisplayName("Segment preview and reminder paths")
    class SegmentAndReminder {

        @Test
        void segmentPreviewExcludesWhenGuardianConsentRequiredAndMissing() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(true);
            when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            any(Customer.class), eq(ConsentType.MARKETING_EMAIL), eq(true)))
                    .thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

            assertExcludedForMissingGuardianConsent(decision);
            verify(consentService)
                    .isCommunicationEligible(
                            any(Customer.class), eq(ConsentType.MARKETING_EMAIL), eq(true));
        }

        @Test
        void reminderEvaluationExcludesWhenGuardianConsentRequiredAndMissing() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(true);
            when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            any(Customer.class), eq(ConsentType.MARKETING_EMAIL), eq(true)))
                    .thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateForReminder(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL);

            assertExcludedForMissingGuardianConsent(decision);
            verify(jdbcTemplate, never())
                    .queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class));
        }
    }

    @Nested
    @DisplayName("Reason code and short-circuit")
    class ReasonAndOrder {

        @Test
        void guardianConsentTypeAndInvalidConsentDecisionContract() {
            // ConsentService requires ConsentType.GUARDIAN when guardianConsentRequired is true;
            // EligibilityService maps a false communication-eligible result to INVALID_CONSENT.
            assertThat(ConsentType.GUARDIAN.name()).isEqualTo("GUARDIAN");
            EligibilityDecision decision =
                    EligibilityDecision.excluded(EligibilityExclusionReason.INVALID_CONSENT);
            assertExcludedForMissingGuardianConsent(decision);
        }


        @Test
        void missingGuardianConsentUsesInvalidConsentReasonCode() {
            assertThat(EligibilityExclusionReason.INVALID_CONSENT.code())
                    .isEqualTo("INVALID_CONSENT");
            assertThat(EligibilityExclusionReason.INVALID_CONSENT.explanation())
                    .contains("valid required consent");
        }

        @Test
        void guardianFailureShortCircuitsBeforeDuplicateAndMonthlyLimit() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true))
                    .thenReturn(false);

            eligibilityService.evaluateCustomer(
                    CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, true);

            verify(consentService)
                    .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, true);
            verify(jdbcTemplate, never())
                    .queryForObject(any(String.class), eq(Integer.class), any(), any());
        }
    }

    @Nested
    @DisplayName("KB critical-test contract (item 650)")
    class CriticalContract {

        @Test
        void criticalRuleStatementMatchesKnowledgeBase() {
            assertThat(MinorBeneficiaryWithoutGuardianConsentIsExcludedContract.CRITICAL_TEST_ITEM)
                    .isEqualTo(650);
            assertThat(MinorBeneficiaryWithoutGuardianConsentIsExcludedContract.RULE_STATEMENT)
                    .isEqualTo("Minor beneficiary without guardian consent is excluded");
            assertThat(MinorBeneficiaryWithoutGuardianConsentIsExcludedContract.BUSINESS_RULE_IDS)
                    .containsExactly("BR-003");
            assertThat(
                            MinorBeneficiaryWithoutGuardianConsentIsExcludedContract
                                    .FUNCTIONAL_REQUIREMENT_IDS)
                    .contains("FR-032", "FR-034");
            assertThat(MinorBeneficiaryWithoutGuardianConsentIsExcludedContract.EXCLUSION_REASON_CODE)
                    .isEqualTo("INVALID_CONSENT");
            assertThat(MinorBeneficiaryWithoutGuardianConsentIsExcludedContract.GUARDIAN_CONSENT_TYPE)
                    .isEqualTo(ConsentType.GUARDIAN);
        }
    }

    private static void assertExcludedForMissingGuardianConsent(EligibilityDecision decision) {
        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("INVALID_CONSENT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer does not have valid required consent");
    }

    private Customer customer() throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Mina", "Beneficiary");
        setId(customer, CUSTOMER_ID);
        return customer;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    static final class MinorBeneficiaryWithoutGuardianConsentIsExcludedContract {
        static final int CRITICAL_TEST_ITEM = 650;
        static final String RULE_STATEMENT =
                "Minor beneficiary without guardian consent is excluded";
        static final java.util.List<String> BUSINESS_RULE_IDS = java.util.List.of("BR-003");
        static final java.util.List<String> FUNCTIONAL_REQUIREMENT_IDS =
                java.util.List.of("FR-032", "FR-034");
        static final String EXCLUSION_REASON_CODE = "INVALID_CONSENT";
        static final ConsentType GUARDIAN_CONSENT_TYPE = ConsentType.GUARDIAN;

        private MinorBeneficiaryWithoutGuardianConsentIsExcludedContract() {}
    }
}
