package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
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
 * KB item 197 / FR-078 acceptance: OR logic returns correct result.
 *
 * <p>Proves multi-criteria disjunctive joins (OR after the first criterion) return the union of
 * matching branches, preserve left-to-right mixed AND/OR associativity, and work through preview.
 */
@ExtendWith(MockitoExtension.class)
class SegmentOrLogicReturnsCorrectResultTests {

    private static final UUID ID_MUNICH_PROSPECT =
            UUID.fromString("20000000-0000-0000-0000-000000000a01");
    private static final UUID ID_MUNICH_CUSTOMER =
            UUID.fromString("20000000-0000-0000-0000-000000000a02");
    private static final UUID ID_BERLIN_CUSTOMER =
            UUID.fromString("20000000-0000-0000-0000-000000000a03");
    private static final UUID ID_HAMBURG_CUSTOMER =
            UUID.fromString("20000000-0000-0000-0000-000000000a04");
    private static final UUID ID_COLOGNE_PROSPECT =
            UUID.fromString("20000000-0000-0000-0000-000000000a05");

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
        lenient()
                .when(eligibilityService.evaluateForSegmentPreview(any(UUID.class)))
                .thenReturn(EligibilityDecision.included());
        lenient().when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @ParameterizedTest(name = "OR truth table accumulated={0} next={1} => {2}")
    @CsvSource({
        "true, true, true",
        "true, false, true",
        "false, true, true",
        "false, false, false"
    })
    void combineOrReturnsInclusiveOrTruthTable(
            boolean accumulated, boolean nextMatch, boolean expected) {
        assertThat(
                        SegmentCriteriaLogicSupport.combine(
                                accumulated, nextMatch, SegmentJoinOperator.OR))
                .isEqualTo(expected);
    }

    @Test
    void twoCityOrReturnsUnionOfBothCities() throws Exception {
        stubOrProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                cityEquals("Munich", SegmentJoinOperator.AND),
                                cityEquals("Berlin", SegmentJoinOperator.OR)));

        assertThat(matches)
                .extracting(CustomerView::city)
                .containsExactlyInAnyOrder("Munich", "Munich", "Berlin");
        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Anna Weber", "Tom Schmidt");
        assertThat(matches).noneMatch(view -> "Hamburg".equals(view.city()));
        assertThat(matches).noneMatch(view -> "Cologne".equals(view.city()));
    }

    @Test
    void threeWayCityOrReturnsUnionOfAllBranches() throws Exception {
        stubOrProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                cityEquals("Munich", SegmentJoinOperator.AND),
                                cityEquals("Berlin", SegmentJoinOperator.OR),
                                cityEquals("Hamburg", SegmentJoinOperator.OR)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder(
                        "Lena Mueller", "Anna Weber", "Tom Schmidt", "Max Bauer");
        assertThat(matches).noneMatch(view -> view.fullName().equals("Kai Fischer"));
    }

    @Test
    void crossFieldProspectOrMunichReturnsUnion() throws Exception {
        stubOrProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                typeEquals(CustomerType.PROSPECT, SegmentJoinOperator.AND),
                                cityEquals("Munich", SegmentJoinOperator.OR)));

        // Lena (prospect Munich), Kai (prospect Cologne), Anna (customer Munich)
        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Anna Weber", "Kai Fischer");
        assertThat(matches).noneMatch(view -> view.fullName().equals("Tom Schmidt"));
        assertThat(matches).noneMatch(view -> view.fullName().equals("Max Bauer"));
    }

    @Test
    void mixedAndOrLeftAssociativityReturnsCorrectUnion() throws Exception {
        stubOrProfiles();

        // (PROSPECT AND Munich) OR Berlin => Lena + Tom (not Anna Munich customer, not Kai, not
        // Max)
        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                typeEquals(CustomerType.PROSPECT, SegmentJoinOperator.AND),
                                cityEquals("Munich", SegmentJoinOperator.AND),
                                cityEquals("Berlin", SegmentJoinOperator.OR)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Tom Schmidt");
        assertThat(matches).noneMatch(view -> view.fullName().equals("Anna Weber"));
        assertThat(matches).noneMatch(view -> view.fullName().equals("Kai Fischer"));
    }

    @Test
    void orDoesNotIncludeProfilesOutsideAnyBranch() throws Exception {
        stubOrProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                cityEquals("Munich", SegmentJoinOperator.AND),
                                cityEquals("Berlin", SegmentJoinOperator.OR)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .doesNotContain("Max Bauer", "Kai Fischer");
    }

    @Test
    void orReturnsEmptyWhenNoBranchMatches() throws Exception {
        stubOrProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                cityEquals("Vienna", SegmentJoinOperator.AND),
                                cityEquals("Zurich", SegmentJoinOperator.OR)));

        assertThat(matches).isEmpty();
    }

    @Test
    void singleCriterionWithOrJoinStillMatchesFirstCriterionOnly() throws Exception {
        stubOrProfiles();

        // Only one criterion — first join is ignored; behaves like a simple filter.
        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(cityEquals("Berlin", SegmentJoinOperator.OR)));

        assertThat(matches).extracting(CustomerView::fullName).containsExactly("Tom Schmidt");
    }

    @Test
    void previewSegmentWithOrCriteriaReturnsUnionCounts() throws Exception {
        stubOrProfiles();

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        cityEquals("Munich", SegmentJoinOperator.AND),
                                        cityEquals("Berlin", SegmentJoinOperator.OR))));

        assertThat(preview.totalAudienceCount()).isEqualTo(3);
        assertThat(preview.eligibleCount()).isEqualTo(3);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Anna Weber", "Tom Schmidt");
    }

    @Test
    void previewMixedAndOrReturnsCorrectEligibleUnion() throws Exception {
        stubOrProfiles();

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        typeEquals(CustomerType.PROSPECT, SegmentJoinOperator.AND),
                                        cityEquals("Munich", SegmentJoinOperator.AND),
                                        cityEquals("Berlin", SegmentJoinOperator.OR))));

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Tom Schmidt");
    }

    @Test
    void pureOrChainDetectionAndEvaluateMatchAnyBranch() {
        assertThat(
                        SegmentCriteriaLogicSupport.isPureOrChain(
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.OR,
                                        SegmentJoinOperator.OR)))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.isPureOrChain(
                                List.of(
                                        SegmentJoinOperator.OR,
                                        SegmentJoinOperator.OR,
                                        SegmentJoinOperator.AND)))
                .isFalse();
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(false, true, false),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.OR,
                                        SegmentJoinOperator.OR)))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(false, false, false),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.OR,
                                        SegmentJoinOperator.OR)))
                .isFalse();
        assertThat(SegmentCriteriaLogicSupport.matchesAnyOr(List.of(false, true))).isTrue();
        assertThat(SegmentCriteriaLogicSupport.matchesAnyOr(List.of(false, false))).isFalse();
    }

    @Test
    void firstCriterionJoinOperatorDoesNotForceOrResult() throws Exception {
        stubOrProfiles();

        // First join OR ignored; second join AND requires both Munich AND PROSPECT.
        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                cityEquals("Munich", SegmentJoinOperator.OR),
                                typeEquals(CustomerType.PROSPECT, SegmentJoinOperator.AND)));

        assertThat(matches).extracting(CustomerView::fullName).containsExactly("Lena Mueller");
        assertThat(matches).noneMatch(view -> view.fullName().equals("Anna Weber"));
    }

    @Test
    void orUnionIsLargerThanAndIntersectionOnSameFields() throws Exception {
        stubOrProfiles();

        List<CustomerView> andMatches =
                segmentService.findMatchingCustomers(
                        List.of(
                                cityEquals("Munich", SegmentJoinOperator.AND),
                                typeEquals(CustomerType.PROSPECT, SegmentJoinOperator.AND)));
        List<CustomerView> orMatches =
                segmentService.findMatchingCustomers(
                        List.of(
                                cityEquals("Munich", SegmentJoinOperator.AND),
                                typeEquals(CustomerType.PROSPECT, SegmentJoinOperator.OR)));

        assertThat(andMatches).extracting(CustomerView::fullName).containsExactly("Lena Mueller");
        assertThat(orMatches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Anna Weber", "Kai Fischer");
        assertThat(orMatches.size()).isGreaterThan(andMatches.size());
    }

    private void stubOrProfiles() throws Exception {
        Customer lena =
                profile("Lena", "Mueller", ID_MUNICH_PROSPECT, "Munich", CustomerType.PROSPECT);
        Customer anna =
                profile("Anna", "Weber", ID_MUNICH_CUSTOMER, "Munich", CustomerType.CUSTOMER);
        Customer tom =
                profile("Tom", "Schmidt", ID_BERLIN_CUSTOMER, "Berlin", CustomerType.CUSTOMER);
        Customer max =
                profile("Max", "Bauer", ID_HAMBURG_CUSTOMER, "Hamburg", CustomerType.CUSTOMER);
        Customer kai =
                profile("Kai", "Fischer", ID_COLOGNE_PROSPECT, "Cologne", CustomerType.PROSPECT);

        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(lena, anna, tom, max, kai));
    }

    private static CreateSegmentCriteriaCommand cityEquals(
            String city, SegmentJoinOperator joinOperator) {
        return new CreateSegmentCriteriaCommand(
                "city", SegmentOperator.EQUALS, city, "location", joinOperator);
    }

    private static CreateSegmentCriteriaCommand typeEquals(
            CustomerType type, SegmentJoinOperator joinOperator) {
        return new CreateSegmentCriteriaCommand(
                "customer_type", SegmentOperator.EQUALS, type.name(), "audience", joinOperator);
    }

    private static Customer profile(
            String first, String last, UUID id, String city, CustomerType type) throws Exception {
        Customer customer = Customer.create(type, first, last);
        customer.updateAddress(null, city, "Germany");
        customer.updateDemographics(null, CustomerAgeGroup.AGE_26_40);
        customer.changeStatus(CustomerStatus.ACTIVE);
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(customer, id);
        return customer;
    }
}
