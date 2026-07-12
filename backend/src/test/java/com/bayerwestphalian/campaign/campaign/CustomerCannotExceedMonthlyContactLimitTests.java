package com.bayerwestphalian.campaign.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Sprint 16 critical test item <b>652</b>: Customer cannot exceed monthly contact limit.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code BR-011} — Same customer cannot receive more than the configured number of marketing
 *       messages per month
 *   <li>{@code FR-092} — System prevents excessive contact frequency
 *   <li>{@code FR-056} — System prevents duplicate/excessive marketing (frequency path)
 * </ul>
 *
 * <p>Authoritative enforcement is {@link EligibilityService}: counts {@code SENT}/{@code CALLED}
 * contact events in a rolling 30-day window and compares to {@link
 * SystemSettingsService#monthlyContactLimit()} (item 535). At or above the limit → {@link
 * EligibilityExclusionReason#MONTHLY_CONTACT_LIMIT}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("652 Customer cannot exceed monthly contact limit")
class CustomerCannotExceedMonthlyContactLimitTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000652");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000652");
    private static final Instant NOW = Instant.parse("2026-07-12T17:00:00Z");
    private static final int DEFAULT_LIMIT = 3;

    @Mock private ConsentService consentService;
    @Mock private CustomerRepository customerRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private SystemSettingsService systemSettingsService;

    private EligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        lenient().when(systemSettingsService.monthlyContactLimit()).thenReturn(DEFAULT_LIMIT);
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
    @DisplayName("Campaign eligibility — MONTHLY_CONTACT_LIMIT (BR-011)")
    class CampaignPaths {

        @BeforeEach
        void contactableAndNotDuplicate() throws Exception {
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
        }

        @Test
        @DisplayName("evaluateCustomer excludes when contact count reaches configured limit")
        void evaluateCustomerExcludesAtLimit() {
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class)))
                    .thenReturn(DEFAULT_LIMIT);

            EligibilityDecision decision =
                    eligibilityService.evaluateCustomer(
                            CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

            assertExcludedAsMonthlyLimit(decision);
            verify(systemSettingsService).monthlyContactLimit();
        }

        @Test
        @DisplayName("evaluateCustomer excludes when contact count exceeds configured limit")
        void evaluateCustomerExcludesAboveLimit() {
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class)))
                    .thenReturn(DEFAULT_LIMIT + 2);

            EligibilityDecision decision =
                    eligibilityService.evaluateCustomer(
                            CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

            assertExcludedAsMonthlyLimit(decision);
        }

        @Test
        @DisplayName("evaluateCustomer allows contact when count is below configured limit")
        void evaluateCustomerAllowsBelowLimitPositiveControl() {
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class)))
                    .thenReturn(DEFAULT_LIMIT - 1);

            EligibilityDecision decision =
                    eligibilityService.evaluateCustomer(
                            CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

            assertThat(decision.eligible()).isTrue();
            assertThat(decision.exclusionReason()).isNull();
        }

        @Test
        @DisplayName("campaign preview enforces monthly contact limit")
        void campaignPreviewEnforcesLimit() {
            when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                    .thenReturn("EMAIL");
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class)))
                    .thenReturn(DEFAULT_LIMIT);

            EligibilityDecision decision =
                    eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

            assertExcludedAsMonthlyLimit(decision);
        }

        @Test
        @DisplayName("isCommunicationEligible is false at monthly limit")
        void isCommunicationEligibleFalseAtLimit() {
            when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                    .thenReturn("EMAIL");
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class)))
                    .thenReturn(DEFAULT_LIMIT);

            assertThat(eligibilityService.isCommunicationEligible(CUSTOMER_ID, CAMPAIGN_ID))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Segment preview and reminder paths")
    class SegmentAndReminder {

        @Test
        void segmentPreviewExcludesAtMonthlyLimit() throws Exception {
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
                    .thenReturn(DEFAULT_LIMIT);

            EligibilityDecision decision =
                    eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

            assertExcludedAsMonthlyLimit(decision);
        }

        @Test
        void reminderEvaluationExcludesAtMonthlyLimit() throws Exception {
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
                    .thenReturn(DEFAULT_LIMIT);

            EligibilityDecision decision =
                    eligibilityService.evaluateForReminder(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL);

            assertExcludedAsMonthlyLimit(decision);
        }
    }

    @Nested
    @DisplayName("checkMonthlyLimit helper and configured limit (item 535)")
    class HelperAndSettings {

        @Test
        void checkMonthlyLimitTrueWhenCountReachesLimit() {
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class)))
                    .thenReturn(3);

            assertThat(eligibilityService.checkMonthlyLimit(CUSTOMER_ID, 3)).isTrue();
            assertThat(eligibilityService.checkMonthlyLimit(CUSTOMER_ID)).isTrue();
            verify(systemSettingsService).monthlyContactLimit();
        }

        @Test
        void checkMonthlyLimitFalseWhenCountBelowLimit() {
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class)))
                    .thenReturn(2);

            assertThat(eligibilityService.checkMonthlyLimit(CUSTOMER_ID, 3)).isFalse();
        }

        @Test
        void checkMonthlyLimitRejectsLimitBelowOne() {
            assertThatThrownBy(() -> eligibilityService.checkMonthlyLimit(CUSTOMER_ID, 0))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Eligibility validation failed");
        }

        @Test
        void eligibilityUsesLiveConfiguredLimitFromSystemSettings() throws Exception {
            when(systemSettingsService.monthlyContactLimit()).thenReturn(3, 5);
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
                    .thenReturn(3);

            EligibilityDecision atThree =
                    eligibilityService.evaluateCustomer(
                            CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);
            EligibilityDecision withRaisedLimit =
                    eligibilityService.evaluateCustomer(
                            CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

            assertExcludedAsMonthlyLimit(atThree);
            assertThat(withRaisedLimit.eligible()).isTrue();
            verify(systemSettingsService, times(2)).monthlyContactLimit();
        }

        @Test
        void rollingWindowStartIsThirtyDaysBeforeClockNow() {
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class)))
                    .thenReturn(0);

            eligibilityService.checkMonthlyLimit(CUSTOMER_ID, 5);

            ArgumentCaptor<Timestamp> windowStart = ArgumentCaptor.forClass(Timestamp.class);
            verify(jdbcTemplate)
                    .queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            windowStart.capture());

            Instant expectedStart = NOW.minus(30, java.time.temporal.ChronoUnit.DAYS);
            assertThat(windowStart.getValue().toInstant()).isEqualTo(expectedStart);
        }
    }

    @Nested
    @DisplayName("Reason code contract")
    class ReasonContract {

        @Test
        void monthlyContactLimitReasonCodeAndExplanationAreStable() {
            assertThat(EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT.code())
                    .isEqualTo("MONTHLY_CONTACT_LIMIT");
            assertThat(EligibilityExclusionReason.CODE_MONTHLY_CONTACT_LIMIT)
                    .isEqualTo("MONTHLY_CONTACT_LIMIT");
            assertThat(EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT.explanation())
                    .isEqualTo("Customer has reached the monthly marketing contact limit");

            assertExcludedAsMonthlyLimit(
                    EligibilityDecision.excluded(EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT));
        }
    }

    @Nested
    @DisplayName("KB critical-test contract (item 652)")
    class CriticalContract {

        @Test
        void criticalRuleStatementMatchesKnowledgeBase() {
            assertThat(CustomerCannotExceedMonthlyContactLimitContract.CRITICAL_TEST_ITEM)
                    .isEqualTo(652);
            assertThat(CustomerCannotExceedMonthlyContactLimitContract.RULE_STATEMENT)
                    .isEqualTo("Customer cannot exceed monthly contact limit");
            assertThat(CustomerCannotExceedMonthlyContactLimitContract.BUSINESS_RULE_IDS)
                    .containsExactly("BR-011");
            assertThat(CustomerCannotExceedMonthlyContactLimitContract.FUNCTIONAL_REQUIREMENT_IDS)
                    .contains("FR-092", "FR-056");
            assertThat(CustomerCannotExceedMonthlyContactLimitContract.EXCLUSION_REASON_CODE)
                    .isEqualTo("MONTHLY_CONTACT_LIMIT");
            assertThat(CustomerCannotExceedMonthlyContactLimitContract.ROLLING_WINDOW_DAYS)
                    .isEqualTo(30);
            assertThat(CustomerCannotExceedMonthlyContactLimitContract.COUNTED_EVENT_TYPES)
                    .containsExactlyInAnyOrder("SENT", "CALLED");
        }
    }

    private static void assertExcludedAsMonthlyLimit(EligibilityDecision decision) {
        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("MONTHLY_CONTACT_LIMIT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer has reached the monthly marketing contact limit");
    }

    private Customer customer() throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Max", "Contacted");
        setId(customer, CUSTOMER_ID);
        return customer;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    static final class CustomerCannotExceedMonthlyContactLimitContract {
        static final int CRITICAL_TEST_ITEM = 652;
        static final String RULE_STATEMENT = "Customer cannot exceed monthly contact limit";
        static final java.util.List<String> BUSINESS_RULE_IDS = java.util.List.of("BR-011");
        static final java.util.List<String> FUNCTIONAL_REQUIREMENT_IDS =
                java.util.List.of("FR-092", "FR-056");
        static final String EXCLUSION_REASON_CODE = "MONTHLY_CONTACT_LIMIT";
        static final int ROLLING_WINDOW_DAYS = 30;
        static final java.util.List<String> COUNTED_EVENT_TYPES =
                java.util.List.of("SENT", "CALLED");

        private CustomerCannotExceedMonthlyContactLimitContract() {}
    }
}
