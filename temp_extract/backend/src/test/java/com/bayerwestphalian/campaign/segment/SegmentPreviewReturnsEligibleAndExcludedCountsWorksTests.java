package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityExclusionReason;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.consent.ConsentRepository;
import com.bayerwestphalian.campaign.consent.ConsentService;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerAgeGroup;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.customer.CustomerView;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KB item 199 / FR-079 acceptance: preview returns eligible and excluded counts.
 *
 * <p>Proves {@link SegmentService#previewSegment} always populates {@code totalAudienceCount},
 * {@code eligibleCount}, and {@code excludedCount} with the FR-079 invariants: total is criteria
 * match size; eligible is contactable after EligibilityService; excluded = total − eligible; and
 * {@code matchingCustomers.size() == eligibleCount}.
 */
@ExtendWith(MockitoExtension.class)
class SegmentPreviewReturnsEligibleAndExcludedCountsWorksTests {

    private static final UUID ID_1 = UUID.fromString("20000000-0000-0000-0000-000000000c01");
    private static final UUID ID_2 = UUID.fromString("20000000-0000-0000-0000-000000000c02");
    private static final UUID ID_3 = UUID.fromString("20000000-0000-0000-0000-000000000c03");
    private static final UUID ID_4 = UUID.fromString("20000000-0000-0000-0000-000000000c04");
    private static final UUID ID_5 = UUID.fromString("20000000-0000-0000-0000-000000000c05");

    @Mock private SegmentRepository segmentRepository;
    @Mock private SegmentCriteriaRepository segmentCriteriaRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ProductOwnershipRepository productOwnershipRepository;
    @Mock private PaymentRecordRepository paymentRecordRepository;
    @Mock private ConsentRepository consentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthorizationExpressions authorizationExpressions;
    @Mock private ConsentService consentService;
    @Mock private EligibilityService eligibilityService;
    @Mock private AuditService auditService;

    private SegmentService segmentService;

    @BeforeEach
    void setUp() {
        segmentService =
                new SegmentService(
                        segmentRepository,
                        segmentCriteriaRepository,
                        customerRepository,
                        productOwnershipRepository,
                        paymentRecordRepository,
                        consentRepository,
                        userRepository,
                        authorizationExpressions,
                        consentService,
                        eligibilityService,
                        auditService);
    }

    @ParameterizedTest(name = "eligible={0} excluded={1} => total={2}")
    @CsvSource({
        "0, 0, 0",
        "1, 0, 1",
        "0, 1, 1",
        "2, 1, 3",
        "1, 3, 4",
        "3, 2, 5"
    })
    void previewCountIdentityEligiblePlusExcludedEqualsTotal(
            int eligible, int excluded, int total) throws Exception {
        stubMunichProfilesWithEligibility(eligible, excluded);

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(total);
        assertThat(preview.eligibleCount()).isEqualTo(eligible);
        assertThat(preview.excludedCount()).isEqualTo(excluded);
        assertThat(preview.eligibleCount() + preview.excludedCount())
                .isEqualTo(preview.totalAudienceCount());
        assertThat(preview.matchingCustomers()).hasSize(eligible);
    }

    @Test
    void previewReturnsEligibleCountOfContactableMatches() throws Exception {
        Customer e1 = munich("Lena", "Mueller", ID_1);
        Customer e2 = munich("Anna", "Weber", ID_2);
        Customer blocked = munich("Tom", "Schmidt", ID_3);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(e1, e2, blocked));
        when(eligibilityService.evaluateForSegmentPreview(ID_1))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ID_2))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ID_3))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(3);
        assertThat(preview.eligibleCount()).isEqualTo(2);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Anna Weber");
    }

    @Test
    void previewReturnsExcludedCountOfIneligibleCriteriaMatches() throws Exception {
        Customer eligible = munich("Lena", "Mueller", ID_1);
        Customer dnc = munich("Tom", "Schmidt", ID_2);
        Customer optOut = munich("Anna", "Weber", ID_3);
        Customer noConsent = munich("Kai", "Fischer", ID_4);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(eligible, dnc, optOut, noConsent));
        when(eligibilityService.evaluateForSegmentPreview(ID_1))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ID_2))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));
        when(eligibilityService.evaluateForSegmentPreview(ID_3))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.MARKETING_OPT_OUT));
        when(eligibilityService.evaluateForSegmentPreview(ID_4))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.INVALID_CONSENT));

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(4);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(3);
        assertThat(preview.excludedCount())
                .isEqualTo(preview.totalAudienceCount() - preview.eligibleCount());
        assertThat(preview.matchingCustomers()).hasSize(1);
        int summaryExcluded =
                preview.exclusionReasonSummary().stream()
                        .mapToInt(SegmentExclusionReasonSummary::count)
                        .sum();
        assertThat(summaryExcluded).isEqualTo(preview.excludedCount());
    }

    @Test
    void previewReturnsZeroEligibleAndPositiveExcludedWhenAllIneligible() throws Exception {
        Customer dnc = munich("Tom", "Schmidt", ID_1);
        Customer optOut = munich("Anna", "Weber", ID_2);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(dnc, optOut));
        when(eligibilityService.evaluateForSegmentPreview(ID_1))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));
        when(eligibilityService.evaluateForSegmentPreview(ID_2))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.MARKETING_OPT_OUT));

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(0);
        assertThat(preview.excludedCount()).isEqualTo(2);
        assertThat(preview.matchingCustomers()).isEmpty();
        assertThat(preview.eligibleCount() + preview.excludedCount())
                .isEqualTo(preview.totalAudienceCount());
    }

    @Test
    void previewReturnsZeroExcludedWhenAllCriteriaMatchesAreEligible() throws Exception {
        Customer e1 = munich("Lena", "Mueller", ID_1);
        Customer e2 = munich("Anna", "Weber", ID_2);
        Customer e3 = munich("Sara", "Klein", ID_3);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(e1, e2, e3));
        when(eligibilityService.evaluateForSegmentPreview(any(UUID.class)))
                .thenReturn(EligibilityDecision.included());

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(3);
        assertThat(preview.eligibleCount()).isEqualTo(3);
        assertThat(preview.excludedCount()).isEqualTo(0);
        assertThat(preview.matchingCustomers()).hasSize(3);
        assertThat(preview.exclusionReasonSummary()).isEmpty();
    }

    @Test
    void previewReturnsZeroTotalEligibleAndExcludedWhenNoCriteriaMatches() throws Exception {
        Customer berlin = berlin("Max", "Bauer", ID_5);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(berlin));
        // Eligibility must not matter — no Munich criteria matches.

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        assertThat(preview.totalAudienceCount()).isEqualTo(0);
        assertThat(preview.eligibleCount()).isEqualTo(0);
        assertThat(preview.excludedCount()).isEqualTo(0);
        assertThat(preview.matchingCustomers()).isEmpty();
        assertThat(preview.exclusionReasonSummary()).isEmpty();
    }

    @Test
    void totalAudienceCountStaysCriteriaSizeWhileEligibleAndExcludedSplit() throws Exception {
        Customer e1 = munich("Lena", "Mueller", ID_1);
        Customer blocked1 = munich("Tom", "Schmidt", ID_2);
        Customer blocked2 = munich("Anna", "Weber", ID_3);
        Customer berlin = berlin("Max", "Bauer", ID_5);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(e1, blocked1, blocked2, berlin));
        when(eligibilityService.evaluateForSegmentPreview(ID_1))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ID_2))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));
        when(eligibilityService.evaluateForSegmentPreview(ID_3))
                .thenReturn(
                        EligibilityDecision.excluded(
                                EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT));

        SegmentPreviewView preview = segmentService.previewSegment(munichPreview());

        // Only 3 Munich profiles match criteria; Berlin is not in total.
        assertThat(preview.totalAudienceCount()).isEqualTo(3);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(2);
        assertThat(preview.matchingCustomers()).hasSize(preview.eligibleCount());
    }

    @Test
    void emptyCriteriaPreviewStillReturnsEligibleAndExcludedCounts() throws Exception {
        Customer eligible = munich("Lena", "Mueller", ID_1);
        Customer blocked = munich("Tom", "Schmidt", ID_2);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(eligible, blocked));
        when(eligibilityService.evaluateForSegmentPreview(ID_1))
                .thenReturn(EligibilityDecision.included());
        when(eligibilityService.evaluateForSegmentPreview(ID_2))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        SegmentPreviewView preview =
                segmentService.previewSegment(new SegmentPreviewCommand(List.of()));

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
    }

    @Test
    void segmentPreviewViewEnforcesEligiblePlusExcludedEqualsTotal() {
        CustomerView customer =
                new CustomerView(
                        ID_1,
                        CustomerType.PROSPECT,
                        "Lena",
                        "Mueller",
                        "Lena Mueller",
                        null,
                        null,
                        null,
                        "Munich",
                        "Germany",
                        null,
                        CustomerAgeGroup.AGE_26_40,
                        CustomerStatus.ACTIVE,
                        false,
                        true,
                        true,
                        null,
                        null,
                        null,
                        null);

        SegmentPreviewView mixed =
                SegmentPreviewView.of(
                        5,
                        2,
                        3,
                        List.of(customer, customer),
                        List.of(
                                SegmentExclusionReasonSummary.of(
                                        "DO_NOT_CONTACT",
                                        "Customer has do-not-contact enabled",
                                        2),
                                SegmentExclusionReasonSummary.of(
                                        "MARKETING_OPT_OUT",
                                        "Customer has withdrawn or rejected marketing consent",
                                        1)));

        assertThat(mixed.eligibleCount() + mixed.excludedCount())
                .isEqualTo(mixed.totalAudienceCount());
        assertThat(mixed.eligibleCount()).isEqualTo(2);
        assertThat(mixed.excludedCount()).isEqualTo(3);

        SegmentPreviewView allEligible = SegmentPreviewView.from(List.of(customer));
        assertThat(allEligible.excludedCount()).isEqualTo(0);
        assertThat(allEligible.eligibleCount()).isEqualTo(allEligible.totalAudienceCount());
    }

    private void stubMunichProfilesWithEligibility(int eligible, int excluded) throws Exception {
        int total = eligible + excluded;
        List<Customer> profiles = new java.util.ArrayList<>();
        UUID[] ids = {ID_1, ID_2, ID_3, ID_4, ID_5};
        for (int i = 0; i < total; i++) {
            UUID id = ids[i];
            Customer customer = munich("Person", String.valueOf(i), id);
            profiles.add(customer);
            if (i < eligible) {
                when(eligibilityService.evaluateForSegmentPreview(id))
                        .thenReturn(EligibilityDecision.included());
            } else {
                when(eligibilityService.evaluateForSegmentPreview(id))
                        .thenReturn(
                                EligibilityDecision.excluded(
                                        EligibilityExclusionReason.DO_NOT_CONTACT));
            }
        }
        when(customerRepository.findActiveProfiles()).thenReturn(profiles);
    }

    private static SegmentPreviewCommand munichPreview() {
        return new SegmentPreviewCommand(
                List.of(
                        new CreateSegmentCriteriaCommand(
                                "city",
                                SegmentOperator.EQUALS,
                                "Munich",
                                "location",
                                SegmentJoinOperator.AND)));
    }

    private static Customer munich(String first, String last, UUID id) throws Exception {
        return profile(first, last, id, "Munich");
    }

    private static Customer berlin(String first, String last, UUID id) throws Exception {
        return profile(first, last, id, "Berlin");
    }

    private static Customer profile(String first, String last, UUID id, String city)
            throws Exception {
        Customer customer = Customer.create(CustomerType.PROSPECT, first, last);
        customer.updateAddress(null, city, "Germany");
        customer.updateDemographics(null, CustomerAgeGroup.AGE_26_40);
        customer.changeStatus(CustomerStatus.ACTIVE);
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(customer, id);
        return customer;
    }
}
