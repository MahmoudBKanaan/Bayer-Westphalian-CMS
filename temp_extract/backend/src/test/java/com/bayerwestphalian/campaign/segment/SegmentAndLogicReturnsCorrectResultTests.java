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
 * KB item 196 / FR-078 acceptance: AND logic returns correct result.
 *
 * <p>Proves multi-criteria conjunctive joins (explicit AND and null→AND default) return only the
 * intersection of all criterion matches, exclude partial matches, and work through preview.
 */
@ExtendWith(MockitoExtension.class)
class SegmentAndLogicReturnsCorrectResultTests {

    private static final UUID ID_MUNICH_PROSPECT =
            UUID.fromString("20000000-0000-0000-0000-000000000901");
    private static final UUID ID_MUNICH_CUSTOMER =
            UUID.fromString("20000000-0000-0000-0000-000000000902");
    private static final UUID ID_BERLIN_PROSPECT =
            UUID.fromString("20000000-0000-0000-0000-000000000903");
    private static final UUID ID_VIENNA_PROSPECT =
            UUID.fromString("20000000-0000-0000-0000-000000000904");
    private static final UUID ID_MUNICH_PROSPECT_AT =
            UUID.fromString("20000000-0000-0000-0000-000000000905");

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

    @ParameterizedTest(name = "AND truth table accumulated={0} next={1} => {2}")
    @CsvSource({
        "true, true, true",
        "true, false, false",
        "false, true, false",
        "false, false, false"
    })
    void combineAndReturnsConjunctionTruthTable(
            boolean accumulated, boolean nextMatch, boolean expected) {
        assertThat(
                        SegmentCriteriaLogicSupport.combine(
                                accumulated, nextMatch, SegmentJoinOperator.AND))
                .isEqualTo(expected);
        // Null join is AND per KB default.
        assertThat(SegmentCriteriaLogicSupport.combine(accumulated, nextMatch, null))
                .isEqualTo(expected);
    }

    @Test
    void twoFieldAndReturnsOnlyIntersection() throws Exception {
        stubAndProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                cityEquals("Munich", SegmentJoinOperator.AND),
                                typeEquals(CustomerType.PROSPECT, SegmentJoinOperator.AND)));

        assertThat(matches).extracting(CustomerView::fullName).containsExactly("Lena Mueller");
        assertThat(matches.getFirst().city()).isEqualTo("Munich");
        assertThat(matches.getFirst().customerType()).isEqualTo(CustomerType.PROSPECT);
    }

    @Test
    void twoFieldAndWithNullJoinOperatorsDefaultsToAndAndReturnsIntersection() throws Exception {
        stubAndProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        null,
                                        null),
                                new CreateSegmentCriteriaCommand(
                                        "customer_type",
                                        SegmentOperator.EQUALS,
                                        "PROSPECT",
                                        null,
                                        null)));

        assertThat(matches).extracting(CustomerView::fullName).containsExactly("Lena Mueller");
    }

    @Test
    void threeFieldAndExcludesEveryPartialMatch() throws Exception {
        stubAndProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                cityEquals("Munich", SegmentJoinOperator.AND),
                                typeEquals(CustomerType.PROSPECT, SegmentJoinOperator.AND),
                                countryEquals("Germany", SegmentJoinOperator.AND)));

        // Only Munich + PROSPECT + Germany (Lena). Munich prospect in Austria (Kai) fails country.
        assertThat(matches).extracting(CustomerView::fullName).containsExactly("Lena Mueller");
        assertThat(matches).noneMatch(view -> view.fullName().equals("Anna Weber")); // Munich CUSTOMER
        assertThat(matches).noneMatch(view -> view.fullName().equals("Tom Schmidt")); // Berlin PROSPECT
        assertThat(matches).noneMatch(view -> view.fullName().equals("Max Bauer")); // Vienna PROSPECT
        assertThat(matches).noneMatch(view -> view.fullName().equals("Kai Fischer")); // Munich AT
    }

    @Test
    void andChainWithAgeGroupAndCityReturnsCorrectIntersection() throws Exception {
        Customer youngMunich =
                profile(
                        "Young",
                        "Munich",
                        ID_MUNICH_PROSPECT,
                        "Munich",
                        "Germany",
                        CustomerType.PROSPECT,
                        CustomerAgeGroup.AGE_18_25);
        Customer matureMunich =
                profile(
                        "Mature",
                        "Munich",
                        ID_MUNICH_CUSTOMER,
                        "Munich",
                        "Germany",
                        CustomerType.PROSPECT,
                        CustomerAgeGroup.AGE_41_60);
        Customer youngBerlin =
                profile(
                        "Young",
                        "Berlin",
                        ID_BERLIN_PROSPECT,
                        "Berlin",
                        "Germany",
                        CustomerType.PROSPECT,
                        CustomerAgeGroup.AGE_18_25);
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(youngMunich, matureMunich, youngBerlin));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "age_group",
                                        SegmentOperator.EQUALS,
                                        "18_25",
                                        "demographics",
                                        SegmentJoinOperator.AND),
                                cityEquals("Munich", SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Young Munich");
        assertThat(matches.getFirst().ageGroup()).isEqualTo(CustomerAgeGroup.AGE_18_25);
        assertThat(matches.getFirst().city()).isEqualTo("Munich");
    }

    @Test
    void singleCriterionAndStillReturnsAllMatchingProfiles() throws Exception {
        stubAndProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(cityEquals("Munich", SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Anna Weber", "Kai Fischer");
    }

    @Test
    void emptyCriteriaMatchesAllActiveProfiles() throws Exception {
        stubAndProfiles();

        List<CustomerView> matches = segmentService.findMatchingCustomers(List.of());

        assertThat(matches).hasSize(5);
    }

    @Test
    void previewSegmentWithAndCriteriaReturnsOnlyIntersectionEligible() throws Exception {
        stubAndProfiles();

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        cityEquals("Munich", SegmentJoinOperator.AND),
                                        typeEquals(
                                                CustomerType.PROSPECT, SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(2); // Lena (DE) + Kai (AT) both Munich prospects
        assertThat(preview.eligibleCount()).isEqualTo(2);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Kai Fischer");
        assertThat(preview.matchingCustomers())
                .allMatch(view -> "Munich".equals(view.city()))
                .allMatch(view -> view.customerType() == CustomerType.PROSPECT);
    }

    @Test
    void previewThreeFieldAndReturnsSingleIntersection() throws Exception {
        stubAndProfiles();

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        cityEquals("Munich", null),
                                        typeEquals(CustomerType.PROSPECT, null),
                                        countryEquals("Germany", null))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(preview.matchingCustomers().getFirst().country()).isEqualTo("Germany");
    }

    @Test
    void andDoesNotMatchWhenOnlyFirstCriterionHolds() throws Exception {
        stubAndProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                cityEquals("Munich", SegmentJoinOperator.AND),
                                typeEquals(CustomerType.BENEFICIARY, SegmentJoinOperator.AND)));

        assertThat(matches).isEmpty();
    }

    @Test
    void andDoesNotMatchWhenOnlySecondCriterionHolds() throws Exception {
        stubAndProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                cityEquals("Hamburg", SegmentJoinOperator.AND),
                                typeEquals(CustomerType.PROSPECT, SegmentJoinOperator.AND)));

        assertThat(matches).isEmpty();
    }

    @Test
    void pureAndChainDetectionAndEvaluateRequireAllTrue() {
        assertThat(
                        SegmentCriteriaLogicSupport.isPureAndChain(
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND)))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.isPureAndChain(
                                List.of(null, null, SegmentJoinOperator.AND)))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(true, true, true),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND)))
                .isTrue();
        assertThat(
                        SegmentCriteriaLogicSupport.evaluate(
                                List.of(true, true, false),
                                List.of(
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND,
                                        SegmentJoinOperator.AND)))
                .isFalse();
        assertThat(SegmentCriteriaLogicSupport.matchesAllAnd(List.of(true, true))).isTrue();
        assertThat(SegmentCriteriaLogicSupport.matchesAllAnd(List.of(true, false))).isFalse();
    }

    @Test
    void firstCriterionJoinOperatorDoesNotAffectAndResult() throws Exception {
        stubAndProfiles();

        // First join OR is ignored; second join AND still requires both city and type.
        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                cityEquals("Munich", SegmentJoinOperator.OR),
                                typeEquals(CustomerType.PROSPECT, SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Kai Fischer");
    }

    private void stubAndProfiles() throws Exception {
        Customer lena =
                profile(
                        "Lena",
                        "Mueller",
                        ID_MUNICH_PROSPECT,
                        "Munich",
                        "Germany",
                        CustomerType.PROSPECT,
                        CustomerAgeGroup.AGE_26_40);
        Customer anna =
                profile(
                        "Anna",
                        "Weber",
                        ID_MUNICH_CUSTOMER,
                        "Munich",
                        "Germany",
                        CustomerType.CUSTOMER,
                        CustomerAgeGroup.AGE_26_40);
        Customer tom =
                profile(
                        "Tom",
                        "Schmidt",
                        ID_BERLIN_PROSPECT,
                        "Berlin",
                        "Germany",
                        CustomerType.PROSPECT,
                        CustomerAgeGroup.AGE_26_40);
        Customer max =
                profile(
                        "Max",
                        "Bauer",
                        ID_VIENNA_PROSPECT,
                        "Vienna",
                        "Austria",
                        CustomerType.PROSPECT,
                        CustomerAgeGroup.AGE_26_40);
        Customer kai =
                profile(
                        "Kai",
                        "Fischer",
                        ID_MUNICH_PROSPECT_AT,
                        "Munich",
                        "Austria",
                        CustomerType.PROSPECT,
                        CustomerAgeGroup.AGE_26_40);

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
                "customer_type",
                SegmentOperator.EQUALS,
                type.name(),
                "audience",
                joinOperator);
    }

    private static CreateSegmentCriteriaCommand countryEquals(
            String country, SegmentJoinOperator joinOperator) {
        return new CreateSegmentCriteriaCommand(
                "country", SegmentOperator.EQUALS, country, "location", joinOperator);
    }

    private static Customer profile(
            String first,
            String last,
            UUID id,
            String city,
            String country,
            CustomerType type,
            CustomerAgeGroup ageGroup)
            throws Exception {
        Customer customer = Customer.create(type, first, last);
        customer.updateAddress(null, city, country);
        customer.updateDemographics(null, ageGroup);
        customer.changeStatus(CustomerStatus.ACTIVE);
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(customer, id);
        return customer;
    }
}
