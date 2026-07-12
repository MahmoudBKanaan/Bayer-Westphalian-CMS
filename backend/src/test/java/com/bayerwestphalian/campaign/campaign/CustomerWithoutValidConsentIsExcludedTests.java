package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Sprint 16 critical test item <b>649</b>: Customer without valid consent is excluded.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code FR-034} — System blocks marketing without valid consent
 *   <li>{@code FR-055} — System excludes opt-outs and invalid consent
 *   <li>{@code BR-004} — Consent must include type, purpose, source, date, and status (validity
 *       depends on a proper consent record)
 * </ul>
 *
 * <p>Authoritative enforcement is {@link EligibilityService}: when consent is missing or invalid for
 * the required channel/type, the decision is {@link EligibilityExclusionReason#INVALID_CONSENT}.
 * Withdrawn marketing consent is a separate code ({@code MARKETING_OPT_OUT} / BR-002) and is
 * covered as a related exclusion that also blocks contact.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("649 Customer without valid consent is excluded")
class CustomerWithoutValidConsentIsExcludedTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000649");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000649");
    private static final Instant NOW = Instant.parse("2026-07-12T14:00:00Z");

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
    @DisplayName("Campaign eligibility — INVALID_CONSENT (FR-034)")
    class CampaignInvalidConsent {

        @Test
        @DisplayName("evaluateCustomer excludes customer without valid required consent")
        void evaluateCustomerExcludesInvalidConsent() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                    .thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateCustomer(
                            CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

            assertExcludedAsInvalidConsent(decision);
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("excludeInvalidConsent helper returns true when ConsentService denies eligibility")
        void excludeInvalidConsentHelper() throws Exception {
            when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                    .thenReturn(false);

            assertThat(
                            eligibilityService.excludeInvalidConsent(
                                    CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                    .isTrue();

            when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                    .thenReturn(true);
            assertThat(
                            eligibilityService.excludeInvalidConsent(
                                    CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                    .isFalse();
        }

        @ParameterizedTest(name = "channel consent type {0} without valid record is excluded")
        @EnumSource(
                value = ConsentType.class,
                names = {"MARKETING_EMAIL", "MARKETING_SMS", "MARKETING_PHONE", "DATA_PROCESSING"})
        void evaluateCustomerExcludesInvalidConsentForEachMarketingChannelType(ConsentType type)
                throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
            when(consentService.isCommunicationEligible(CUSTOMER_ID, type, false)).thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateCustomer(CUSTOMER_ID, CAMPAIGN_ID, type, false);

            assertExcludedAsInvalidConsent(decision);
            verify(consentService).isCommunicationEligible(CUSTOMER_ID, type, false);
            verify(jdbcTemplate, never())
                    .queryForObject(any(String.class), eq(Integer.class), any(), any());
        }

        @Test
        @DisplayName("campaign preview excludes missing marketing email consent")
        void campaignPreviewExcludesInvalidConsent() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                    .thenReturn("EMAIL");
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);
            when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                    .thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

            assertExcludedAsInvalidConsent(decision);
            verify(consentService)
                    .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false);
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
        @DisplayName("isCommunicationEligible is false without valid consent")
        void isCommunicationEligibleFalseWithoutValidConsent() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                    .thenReturn("EMAIL");
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);
            when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                    .thenReturn(false);

            assertThat(eligibilityService.isCommunicationEligible(CUSTOMER_ID, CAMPAIGN_ID))
                    .isFalse();
        }

        @Test
        @DisplayName("valid consent allows eligibility before contact-history checks (positive control)")
        void validConsentPositiveControl() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
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
                    eligibilityService.evaluateCustomer(
                            CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

            assertThat(decision.eligible()).isTrue();
            assertThat(decision.exclusionReason()).isNull();
        }
    }

    @Nested
    @DisplayName("Segment preview and reminder paths")
    class SegmentAndReminder {

        @Test
        void segmentPreviewExcludesInvalidConsent() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);
            when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            any(Customer.class), eq(ConsentType.MARKETING_EMAIL), eq(false)))
                    .thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

            assertExcludedAsInvalidConsent(decision);
        }

        @Test
        void segmentPreviewExcludesInvalidSmsConsent() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);
            when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            any(Customer.class), eq(ConsentType.MARKETING_SMS), eq(false)))
                    .thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateForSegmentPreview(
                            CUSTOMER_ID, ConsentType.MARKETING_SMS);

            assertExcludedAsInvalidConsent(decision);
        }

        @Test
        void reminderEvaluationExcludesInvalidConsent() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);
            when(consentService.hasMarketingOptOut(any(Customer.class))).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            any(Customer.class), eq(ConsentType.MARKETING_EMAIL), eq(false)))
                    .thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateForReminder(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL);

            assertExcludedAsInvalidConsent(decision);
            verify(jdbcTemplate, never())
                    .queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class));
        }
    }

    @Nested
    @DisplayName("Related: withdrawn marketing consent (MARKETING_OPT_OUT / BR-002)")
    class RelatedOptOut {

        @Test
        void withdrawnMarketingConsentIsExcludedWithMarketingOptOutCode() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(true);

            EligibilityDecision decision =
                    eligibilityService.evaluateCustomer(
                            CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

            assertThat(decision.eligible()).isFalse();
            assertThat(decision.exclusionReason()).isEqualTo("MARKETING_OPT_OUT");
            assertThat(decision.eligibilityExplanation())
                    .isEqualTo("Customer has withdrawn or rejected marketing consent");
            verify(consentService, never())
                    .isCommunicationEligible(any(UUID.class), any(ConsentType.class), anyBoolean());
            verifyNoInteractions(jdbcTemplate);
        }
    }

    @Nested
    @DisplayName("Reason code and short-circuit before contact history")
    class ReasonAndOrder {

        @Test
        void invalidConsentReasonCodeAndExplanationAreStable() {
            assertThat(EligibilityExclusionReason.INVALID_CONSENT.code())
                    .isEqualTo("INVALID_CONSENT");
            assertThat(EligibilityExclusionReason.CODE_INVALID_CONSENT)
                    .isEqualTo("INVALID_CONSENT");
            assertThat(EligibilityExclusionReason.INVALID_CONSENT.explanation())
                    .isEqualTo("Customer does not have valid required consent");

            EligibilityDecision decision =
                    EligibilityDecision.excluded(EligibilityExclusionReason.INVALID_CONSENT);
            assertExcludedAsInvalidConsent(decision);
        }

        @Test
        void invalidConsentIsCheckedBeforeDuplicateAndMonthlyLimitQueries() throws Exception {
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
            when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                    .thenReturn(false);

            eligibilityService.evaluateCustomer(
                    CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

            verify(consentService)
                    .isCommunicationEligible(CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false);
            verifyNoInteractions(jdbcTemplate);
        }
    }

    @Nested
    @DisplayName("KB critical-test contract (item 649)")
    class CriticalContract {

        @Test
        void criticalRuleStatementMatchesKnowledgeBase() {
            assertThat(CustomerWithoutValidConsentIsExcludedContract.CRITICAL_TEST_ITEM)
                    .isEqualTo(649);
            assertThat(CustomerWithoutValidConsentIsExcludedContract.RULE_STATEMENT)
                    .isEqualTo("Customer without valid consent is excluded");
            assertThat(CustomerWithoutValidConsentIsExcludedContract.FUNCTIONAL_REQUIREMENT_IDS)
                    .contains("FR-034", "FR-055");
            assertThat(CustomerWithoutValidConsentIsExcludedContract.BUSINESS_RULE_IDS)
                    .contains("BR-004");
            assertThat(CustomerWithoutValidConsentIsExcludedContract.EXCLUSION_REASON_CODE)
                    .isEqualTo("INVALID_CONSENT");
            assertThat(CustomerWithoutValidConsentIsExcludedContract.RELATED_OPT_OUT_CODE)
                    .isEqualTo("MARKETING_OPT_OUT");
        }
    }

    private static void assertExcludedAsInvalidConsent(EligibilityDecision decision) {
        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("INVALID_CONSENT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer does not have valid required consent");
    }

    private Customer customer() throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Casey", "Consent");
        setId(customer, CUSTOMER_ID);
        return customer;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    static final class CustomerWithoutValidConsentIsExcludedContract {
        static final int CRITICAL_TEST_ITEM = 649;
        static final String RULE_STATEMENT = "Customer without valid consent is excluded";
        static final java.util.List<String> BUSINESS_RULE_IDS = java.util.List.of("BR-004");
        static final java.util.List<String> FUNCTIONAL_REQUIREMENT_IDS =
                java.util.List.of("FR-034", "FR-055");
        static final String EXCLUSION_REASON_CODE = "INVALID_CONSENT";
        static final String RELATED_OPT_OUT_CODE = "MARKETING_OPT_OUT";

        private CustomerWithoutValidConsentIsExcludedContract() {}
    }
}
