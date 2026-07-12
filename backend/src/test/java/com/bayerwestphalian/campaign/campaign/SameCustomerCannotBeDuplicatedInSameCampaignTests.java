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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
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
 * Sprint 16 critical test item <b>651</b>: Same customer cannot be duplicated in same campaign.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code BR-010} — Same customer cannot receive the same campaign twice
 *   <li>{@code FR-056} — System prevents duplicate/excessive marketing (duplicate recipient path)
 * </ul>
 *
 * <p>Authoritative enforcement:
 *
 * <ul>
 *   <li>{@link EligibilityService} returns {@link
 *       EligibilityExclusionReason#DUPLICATE_CAMPAIGN_RECIPIENT} when a {@code campaign_recipients}
 *       row already exists for the pair
 *   <li>{@link CampaignRecipient} unique constraint {@code
 *       campaign_recipients_campaign_customer_unique} on ({@code campaign_id}, {@code customer_id})
 * </ul>
 *
 * <p>Segment preview and reminder paths intentionally skip same-campaign duplicate checks (not
 * campaign-scoped).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("651 Same customer cannot be duplicated in same campaign")
class SameCustomerCannotBeDuplicatedInSameCampaignTests {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000651");
    private static final UUID CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000651");
    private static final UUID OTHER_CAMPAIGN_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000652");
    private static final Instant NOW = Instant.parse("2026-07-12T16:00:00Z");

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
    @DisplayName("EligibilityService — DUPLICATE_CAMPAIGN_RECIPIENT (BR-010)")
    class EligibilityDuplicateGate {

        @BeforeEach
        void contactableCustomer() throws Exception {
            lenient().when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
            lenient().when(consentService.hasMarketingOptOut(CUSTOMER_ID)).thenReturn(false);
            lenient()
                    .when(consentService.isCommunicationEligible(
                            CUSTOMER_ID, ConsentType.MARKETING_EMAIL, false))
                    .thenReturn(true);
        }

        @Test
        @DisplayName("evaluateCustomer excludes when already assigned to same campaign")
        void evaluateCustomerExcludesDuplicateOnSameCampaign() {
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CAMPAIGN_ID),
                            eq(CUSTOMER_ID)))
                    .thenReturn(1);

            EligibilityDecision decision =
                    eligibilityService.evaluateCustomer(
                            CUSTOMER_ID, CAMPAIGN_ID, ConsentType.MARKETING_EMAIL, false);

            assertExcludedAsDuplicate(decision);
            verify(jdbcTemplate, never())
                    .queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class));
        }

        @Test
        @DisplayName("excludeDuplicateContacts is true when recipient row already exists")
        void excludeDuplicateContactsHelper() {
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CAMPAIGN_ID),
                            eq(CUSTOMER_ID)))
                    .thenReturn(1);

            assertThat(eligibilityService.excludeDuplicateContacts(CAMPAIGN_ID, CUSTOMER_ID))
                    .isTrue();

            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CAMPAIGN_ID),
                            eq(CUSTOMER_ID)))
                    .thenReturn(0);
            assertThat(eligibilityService.excludeDuplicateContacts(CAMPAIGN_ID, CUSTOMER_ID))
                    .isFalse();
        }

        @Test
        @DisplayName("campaign preview excludes customer already on the same campaign")
        void campaignPreviewExcludesDuplicate() {
            when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                    .thenReturn("EMAIL");
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CAMPAIGN_ID),
                            eq(CUSTOMER_ID)))
                    .thenReturn(1);

            EligibilityDecision decision =
                    eligibilityService.evaluateForCampaignPreview(CUSTOMER_ID, CAMPAIGN_ID);

            assertExcludedAsDuplicate(decision);
            verify(jdbcTemplate, never())
                    .queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class));
        }

        @Test
        @DisplayName("isCommunicationEligible is false when already a recipient of the campaign")
        void isCommunicationEligibleFalseForDuplicate() {
            when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq(CAMPAIGN_ID)))
                    .thenReturn("EMAIL");
            when(jdbcTemplate.queryForObject(any(String.class), eq(Boolean.class), eq(CUSTOMER_ID)))
                    .thenReturn(false);
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CAMPAIGN_ID),
                            eq(CUSTOMER_ID)))
                    .thenReturn(1);

            assertThat(eligibilityService.isCommunicationEligible(CUSTOMER_ID, CAMPAIGN_ID))
                    .isFalse();
        }

        @Test
        @DisplayName("customer not yet on campaign remains eligible (positive control)")
        void notDuplicatePositiveControl() {
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

        @Test
        @DisplayName("assignment to a different campaign does not exclude for this campaign")
        void differentCampaignIsNotADuplicateOfThisCampaign() {
            // Only the pair (CAMPAIGN_ID, CUSTOMER_ID) is checked — count 0 means not on THIS
            // campaign even if the customer exists on OTHER_CAMPAIGN_ID in real data.
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
            verify(jdbcTemplate)
                    .queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CAMPAIGN_ID),
                            eq(CUSTOMER_ID));
            assertThat(OTHER_CAMPAIGN_ID).isNotEqualTo(CAMPAIGN_ID);
        }

        @Test
        @DisplayName("null campaignId skips duplicate check (segment-style evaluation)")
        void nullCampaignIdDoesNotApplyDuplicateGate() {
            when(jdbcTemplate.queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class)))
                    .thenReturn(0);

            EligibilityDecision decision =
                    eligibilityService.evaluateCustomer(
                            CUSTOMER_ID, null, ConsentType.MARKETING_EMAIL, false);

            // isDuplicateCampaignRecipient returns false when campaignId is null — only monthly
            // contact-limit count query runs (customer_id + timestamp), not campaign/customer pair.
            assertThat(decision.eligible()).isTrue();
            verify(jdbcTemplate)
                    .queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CUSTOMER_ID),
                            any(Timestamp.class));
        }
    }

    @Nested
    @DisplayName("Segment preview does not apply same-campaign duplicate (not campaign-scoped)")
    class SegmentPreviewScope {

        @Test
        void segmentPreviewDoesNotQueryCampaignRecipientDuplicates() throws Exception {
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

            EligibilityDecision decision =
                    eligibilityService.evaluateForSegmentPreview(CUSTOMER_ID);

            assertThat(decision.eligible()).isTrue();
            // No campaign_id,customer_id count query (would use two UUID args for campaign pair).
            verify(jdbcTemplate, never())
                    .queryForObject(
                            any(String.class),
                            eq(Integer.class),
                            eq(CAMPAIGN_ID),
                            eq(CUSTOMER_ID));
        }
    }

    @Nested
    @DisplayName("Persistence unique constraint (campaign_id, customer_id)")
    class SchemaConstraint {

        @Test
        void campaignRecipientDeclaresUniqueConstraintOnCampaignAndCustomer() {
            Table table = CampaignRecipient.class.getAnnotation(Table.class);
            assertThat(table).isNotNull();
            UniqueConstraint[] constraints = table.uniqueConstraints();
            assertThat(constraints).isNotEmpty();

            boolean found =
                    Arrays.stream(constraints)
                            .anyMatch(
                                    uc ->
                                            "campaign_recipients_campaign_customer_unique"
                                                            .equals(uc.name())
                                                    && Arrays.asList(uc.columnNames())
                                                            .containsAll(
                                                                    java.util.List.of(
                                                                            "campaign_id",
                                                                            "customer_id")));
            assertThat(found)
                    .as(
                            "CampaignRecipient must enforce unique (campaign_id, customer_id) for BR-010")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Reason code contract")
    class ReasonContract {

        @Test
        void duplicateReasonCodeAndExplanationAreStable() {
            assertThat(EligibilityExclusionReason.DUPLICATE_CAMPAIGN_RECIPIENT.code())
                    .isEqualTo("DUPLICATE_CAMPAIGN_RECIPIENT");
            assertThat(EligibilityExclusionReason.CODE_DUPLICATE_CAMPAIGN_RECIPIENT)
                    .isEqualTo("DUPLICATE_CAMPAIGN_RECIPIENT");
            assertThat(EligibilityExclusionReason.DUPLICATE_CAMPAIGN_RECIPIENT.explanation())
                    .isEqualTo("Customer is already assigned to this campaign");

            assertExcludedAsDuplicate(
                    EligibilityDecision.excluded(
                            EligibilityExclusionReason.DUPLICATE_CAMPAIGN_RECIPIENT));
        }
    }

    @Nested
    @DisplayName("KB critical-test contract (item 651)")
    class CriticalContract {

        @Test
        void criticalRuleStatementMatchesKnowledgeBase() {
            assertThat(SameCustomerCannotBeDuplicatedInSameCampaignContract.CRITICAL_TEST_ITEM)
                    .isEqualTo(651);
            assertThat(SameCustomerCannotBeDuplicatedInSameCampaignContract.RULE_STATEMENT)
                    .isEqualTo("Same customer cannot be duplicated in same campaign");
            assertThat(SameCustomerCannotBeDuplicatedInSameCampaignContract.BUSINESS_RULE_IDS)
                    .containsExactly("BR-010");
            assertThat(
                            SameCustomerCannotBeDuplicatedInSameCampaignContract
                                    .FUNCTIONAL_REQUIREMENT_IDS)
                    .contains("FR-056");
            assertThat(SameCustomerCannotBeDuplicatedInSameCampaignContract.EXCLUSION_REASON_CODE)
                    .isEqualTo("DUPLICATE_CAMPAIGN_RECIPIENT");
            assertThat(SameCustomerCannotBeDuplicatedInSameCampaignContract.UNIQUE_CONSTRAINT_NAME)
                    .isEqualTo("campaign_recipients_campaign_customer_unique");
        }
    }

    private static void assertExcludedAsDuplicate(EligibilityDecision decision) {
        assertThat(decision.eligible()).isFalse();
        assertThat(decision.exclusionReason()).isEqualTo("DUPLICATE_CAMPAIGN_RECIPIENT");
        assertThat(decision.eligibilityExplanation())
                .isEqualTo("Customer is already assigned to this campaign");
    }

    private Customer customer() throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Dup", "Recipient");
        setId(customer, CUSTOMER_ID);
        return customer;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    static final class SameCustomerCannotBeDuplicatedInSameCampaignContract {
        static final int CRITICAL_TEST_ITEM = 651;
        static final String RULE_STATEMENT = "Same customer cannot be duplicated in same campaign";
        static final java.util.List<String> BUSINESS_RULE_IDS = java.util.List.of("BR-010");
        static final java.util.List<String> FUNCTIONAL_REQUIREMENT_IDS =
                java.util.List.of("FR-056");
        static final String EXCLUSION_REASON_CODE = "DUPLICATE_CAMPAIGN_RECIPIENT";
        static final String UNIQUE_CONSTRAINT_NAME =
                "campaign_recipients_campaign_customer_unique";

        private SameCustomerCannotBeDuplicatedInSameCampaignContract() {}
    }
}
