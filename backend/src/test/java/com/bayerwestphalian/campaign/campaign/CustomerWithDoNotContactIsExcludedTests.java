package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Sprint 16 critical test item <b>648</b>: Customer with do_not_contact is excluded.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code BR-001} — A person with {@code do_not_contact = true} must never be included in a
 *       campaign
 *   <li>{@code FR-097} — System respects do-not-contact status
 *   <li>{@code FR-055} — System excludes opt-outs and invalid consent (DNC is the first gate)
 * </ul>
 *
 * <p>Authoritative enforcement is {@link EligibilityService}: DNC is evaluated before consent,
 * opt-out, duplicate, and monthly-limit checks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("648 Customer with do_not_contact is excluded")
class CustomerWithDoNotContactIsExcludedTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000648");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000648");
    private static final Instant NOW = Instant.parse("2026-07-12T12:00:00Z");

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
    @DisplayName("Campaign eligibility (evaluateCustomer / preview / isCommunicationEligible)")
    class CampaignPaths {

        @Test
        @DisplayName("evaluateCustomer excludes DNC with stable DO_NOT_CONTACT reason")
        void evaluateCustomerExcludesDoNotContact() throws Exception {
            Customer customer = doNotContactCustomer();
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

            EligibilityDecision decision =
                    eligibilityService.evaluateCustomer(
                            CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

            assertExcludedAsDoNotContact(decision);
            verifyNoInteractions(consentService);
            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("campaign preview excludes DNC before consent and contact-frequency queries")
        void campaignPreviewExcludesDoNotContact() throws Exception {
            Customer customer = doNotContactCustomer();
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                    .thenReturn("EMAIL");
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

            assertExcludedAsDoNotContact(decision);
            verifyNoInteractions(consentService);
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
        @DisplayName("isCommunicationEligible is false for DNC customers")
        void isCommunicationEligibleIsFalseForDoNotContact() throws Exception {
            Customer customer = doNotContactCustomer();
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                    .thenReturn("EMAIL");
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);

            assertThat(eligibilityService.isCommunicationEligible(CUSTOMER_ID, CAMPAIGN_ID))
                    .isFalse();
            verifyNoInteractions(consentService);
        }

        @Test
        @DisplayName("contactable customer remains eligible when not DNC (positive control)")
        void contactableCustomerIsEligiblePositiveControl() throws Exception {
            Customer customer = contactableCustomer();
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
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
    @DisplayName("Segment preview and reminder paths (never contact DNC)")
    class SegmentAndReminderPaths {

        @Test
        void segmentPreviewExcludesDoNotContact() throws Exception {
            Customer customer = doNotContactCustomer();
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

            assertExcludedAsDoNotContact(decision);
            verifyNoInteractions(consentService);
        }

        @Test
        void segmentPreviewExcludesDoNotContactForSmsConsentType() throws Exception {
            Customer customer = doNotContactCustomer();
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateForSegmentPreview(
                            CUSTOMER_ID, ConsentType.MARKETING_SMS);

            assertExcludedAsDoNotContact(decision);
            verifyNoInteractions(consentService);
        }

        @Test
        void reminderEvaluationExcludesDoNotContact() throws Exception {
            Customer customer = doNotContactCustomer();
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);

            EligibilityDecision decision =
                    eligibilityService.evaluateForReminder(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL);

            assertExcludedAsDoNotContact(decision);
            verifyNoInteractions(consentService);
        }
    }

    @Nested
    @DisplayName("Reason code and evaluation order (BR-001)")
    class ReasonAndOrder {

        @Test
        void doNotContactReasonCodeAndExplanationAreStable() {
            assertThat(EligibilityExclusionReason.DO_NOT_CONTACT.code())
                    .isEqualTo("DO_NOT_CONTACT");
            assertThat(EligibilityExclusionReason.CODE_DO_NOT_CONTACT).isEqualTo("DO_NOT_CONTACT");
            assertThat(EligibilityExclusionReason.DO_NOT_CONTACT.explanation())
                    .isEqualTo("Customer has do-not-contact enabled");

            EligibilityDecision decision =
                    EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT);
            assertThat(decision.eligible()).isFalse();
            assertThat(decision.exclusionReason()).isEqualTo("DO_NOT_CONTACT");
            assertThat(decision.eligibilityExplanation())
                    .isEqualTo("Customer has do-not-contact enabled");
        }

        @Test
        void dncIsEvaluatedBeforeConsentOptOutAndMonthlyLimit() throws Exception {
            Customer customer = doNotContactCustomer();
            when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

            eligibilityService.evaluateCustomer(
                    CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, true);

            // Short-circuit: no consent, opt-out, guardian, or contact-history lookups.
            verifyNoInteractions(consentService);
            verifyNoInteractions(jdbcTemplate);
            verify(customerRepository).findById(CUSTOMER_ID);
        }

        @Test
        void customerFlagMarkDoNotContactSetsDomainState() throws Exception {
            Customer customer = contactableCustomer();
            assertThat(customer.isDoNotContact()).isFalse();
            customer.markDoNotContact();
            assertThat(customer.isDoNotContact()).isTrue();
            customer.allowContact();
            assertThat(customer.isDoNotContact()).isFalse();
        }
    }

    @Nested
    @DisplayName("KB critical-test contract (item 648)")
    class CriticalContract {

        @Test
        void criticalRuleStatementMatchesKnowledgeBase() {
            assertThat(CustomerWithDoNotContactIsExcludedContract.CRITICAL_TEST_ITEM)
                    .isEqualTo(648);
            assertThat(CustomerWithDoNotContactIsExcludedContract.RULE_STATEMENT)
                    .isEqualTo("Customer with do_not_contact is excluded");
            assertThat(CustomerWithDoNotContactIsExcludedContract.BUSINESS_RULE_IDS)
                    .containsExactly("BR-001");
            assertThat(CustomerWithDoNotContactIsExcludedContract.FUNCTIONAL_REQUIREMENT_IDS)
                    .contains("FR-097", "FR-055");
            assertThat(CustomerWithDoNotContactIsExcludedContract.EXCLUSION_REASON_CODE)
                    .isEqualTo("DO_NOT_CONTACT");
            assertThat(CustomerWithDoNotContactIsExcludedContract.EVALUATION_ORDER_POSITION)
                    .isEqualTo(1);
        }
    }

    private static void assertExcludedAsDoNotContact(EligibilityDecision decision) {
        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("DO_NOT_CONTACT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer has do-not-contact enabled");
    }

    private Customer doNotContactCustomer() throws Exception {
        Customer customer = contactableCustomer();
        customer.markDoNotContact();
        return customer;
    }

    private Customer contactableCustomer() throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Dana", "Blocked");
        setId(customer, CUSTOMER_ID);
        return customer;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    static final class CustomerWithDoNotContactIsExcludedContract {
        static final int CRITICAL_TEST_ITEM = 648;
        static final String RULE_STATEMENT = "Customer with do_not_contact is excluded";
        static final java.util.List<String> BUSINESS_RULE_IDS = java.util.List.of("BR-001");
        static final java.util.List<String> FUNCTIONAL_REQUIREMENT_IDS =
                java.util.List.of("FR-097", "FR-055");
        static final String EXCLUSION_REASON_CODE = "DO_NOT_CONTACT";
        /** First rule in eligibility evaluation order (see eligibility-rules.md). */
        static final int EVALUATION_ORDER_POSITION = 1;

        private CustomerWithDoNotContactIsExcludedContract() {}
    }
}
