package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.auth.AuthorizationExpressions;
import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KB item 190 / FR-070 acceptance: segment filter by age group works.
 *
 * <p>Proves every KB age-group value ({@code MINOR}, {@code 18_25}, {@code 26_40}, {@code 41_60},
 * {@code 60_PLUS}) can filter active profiles via EQUALS / NOT_EQUALS / IN, including preview and
 * enum-name aliases such as {@code AGE_26_40}.
 */
@ExtendWith(MockitoExtension.class)
class SegmentFilterByAgeGroupWorksTests {

    private static final UUID ID_MINOR = UUID.fromString("20000000-0000-0000-0000-000000000301");
    private static final UUID ID_18_25 = UUID.fromString("20000000-0000-0000-0000-000000000302");
    private static final UUID ID_26_40 = UUID.fromString("20000000-0000-0000-0000-000000000303");
    private static final UUID ID_41_60 = UUID.fromString("20000000-0000-0000-0000-000000000304");
    private static final UUID ID_60_PLUS = UUID.fromString("20000000-0000-0000-0000-000000000305");
    private static final UUID ID_NULL_AGE = UUID.fromString("20000000-0000-0000-0000-000000000306");

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

    @ParameterizedTest(name = "EQUALS {0} matches only that age group")
    @CsvSource({
        "MINOR,MINOR",
        "18_25,AGE_18_25",
        "26_40,AGE_26_40",
        "41_60,AGE_41_60",
        "60_PLUS,AGE_60_PLUS",
        "AGE_18_25,AGE_18_25",
        "AGE_26_40,AGE_26_40"
    })
    void equalsFilterMatchesOnlyTargetAgeGroup(String filterValue, CustomerAgeGroup expected)
            throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allAgeGroupProfiles());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(ageEqualsCriteria(filterValue));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().ageGroup()).isEqualTo(expected);
    }

    @Test
    void notEqualsFilterExcludesTargetAgeGroupAndKeepsOthers() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allAgeGroupProfiles());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "age_group",
                                        SegmentOperator.NOT_EQUALS,
                                        "26_40",
                                        "demographics",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::ageGroup)
                .containsExactlyInAnyOrder(
                        CustomerAgeGroup.MINOR,
                        CustomerAgeGroup.AGE_18_25,
                        CustomerAgeGroup.AGE_41_60,
                        CustomerAgeGroup.AGE_60_PLUS);
        assertThat(matches).noneMatch(view -> view.ageGroup() == CustomerAgeGroup.AGE_26_40);
        assertThat(matches).noneMatch(view -> view.id().equals(ID_NULL_AGE));
    }

    @Test
    void inOperatorMatchesAnyListedKbAgeGroup() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allAgeGroupProfiles());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "age_group",
                                        SegmentOperator.IN,
                                        "MINOR,AGE_41_60,60_PLUS",
                                        "demographics",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::ageGroup)
                .containsExactlyInAnyOrder(
                        CustomerAgeGroup.MINOR,
                        CustomerAgeGroup.AGE_41_60,
                        CustomerAgeGroup.AGE_60_PLUS);
    }

    @Test
    void agegroupAliasFieldNameWorksForEqualsFilter() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allAgeGroupProfiles());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "agegroup",
                                        SegmentOperator.EQUALS,
                                        "AGE_60_PLUS",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().ageGroup()).isEqualTo(CustomerAgeGroup.AGE_60_PLUS);
    }

    @Test
    void customersWithoutAgeGroupDoNotMatchEqualsFilter() throws Exception {
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(profile("No", "Age", ID_NULL_AGE, null)));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(ageEqualsCriteria("26_40"));

        assertThat(matches).isEmpty();
    }

    @Test
    void previewSegmentAppliesAgeGroupFilterAndReturnsEligibleMatches() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allAgeGroupProfiles());

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(ageEqualsCriteria("AGE_18_25")));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().ageGroup())
                .isEqualTo(CustomerAgeGroup.AGE_18_25);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Young Prospect");
    }

    @Test
    void previewWithInAgeGroupsReturnsAllMatchingProfiles() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allAgeGroupProfiles());

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "age_group",
                                                SegmentOperator.IN,
                                                "18_25,26_40",
                                                null,
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(2);
        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::ageGroup)
                .containsExactlyInAnyOrder(CustomerAgeGroup.AGE_18_25, CustomerAgeGroup.AGE_26_40);
    }

    @ParameterizedTest
    @EnumSource(CustomerAgeGroup.class)
    void everyKbAgeGroupValueIsAcceptedAndNormalized(CustomerAgeGroup ageGroup) {
        String normalized =
                SegmentAgeGroupSupport.normalizeFilterValue(
                        SegmentOperator.EQUALS, ageGroup.name());
        assertThat(normalized).isEqualTo(ageGroup.getDatabaseValue());

        SegmentAgeGroupSupport.validateFilterValue(
                SegmentOperator.EQUALS, ageGroup.getDatabaseValue());
        SegmentAgeGroupSupport.validateFilterValue(SegmentOperator.EQUALS, ageGroup.name());
    }

    @Test
    void unsupportedAgeGroupValueIsRejectedOnFindMatchingCustomers() {
        assertThatThrownBy(
                        () -> segmentService.findMatchingCustomers(ageEqualsCriteria("TEENAGER")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void ageGroupFilterCombinedWithLocationAndWorks() throws Exception {
        Customer munichYoung =
                profile("Lena", "Mueller", ID_18_25, CustomerAgeGroup.AGE_18_25, "Munich");
        Customer berlinYoung =
                profile("Tom", "Schmidt", ID_26_40, CustomerAgeGroup.AGE_18_25, "Berlin");
        Customer munichMature =
                profile("Anna", "Weber", ID_41_60, CustomerAgeGroup.AGE_41_60, "Munich");
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichYoung, berlinYoung, munichMature));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "age_group",
                                        SegmentOperator.EQUALS,
                                        "18_25",
                                        "demographics",
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        "location",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(matches.getFirst().ageGroup()).isEqualTo(CustomerAgeGroup.AGE_18_25);
        assertThat(matches.getFirst().city()).isEqualTo("Munich");
    }

    private static List<CreateSegmentCriteriaCommand> ageEqualsCriteria(String value) {
        return List.of(
                new CreateSegmentCriteriaCommand(
                        "age_group",
                        SegmentOperator.EQUALS,
                        value,
                        "demographics",
                        SegmentJoinOperator.AND));
    }

    private static List<Customer> allAgeGroupProfiles() throws Exception {
        return List.of(
                profile("Minor", "Child", ID_MINOR, CustomerAgeGroup.MINOR),
                profile("Young", "Prospect", ID_18_25, CustomerAgeGroup.AGE_18_25),
                profile("Adult", "Customer", ID_26_40, CustomerAgeGroup.AGE_26_40),
                profile("Mature", "Client", ID_41_60, CustomerAgeGroup.AGE_41_60),
                profile("Senior", "Member", ID_60_PLUS, CustomerAgeGroup.AGE_60_PLUS),
                profile("No", "Age", ID_NULL_AGE, null));
    }

    private static Customer profile(String first, String last, UUID id, CustomerAgeGroup ageGroup)
            throws Exception {
        return profile(first, last, id, ageGroup, "Munich");
    }

    private static Customer profile(
            String first, String last, UUID id, CustomerAgeGroup ageGroup, String city)
            throws Exception {
        Customer customer = Customer.create(CustomerType.PROSPECT, first, last);
        customer.updateAddress(null, city, "Germany");
        customer.updateDemographics(null, ageGroup);
        customer.changeStatus(CustomerStatus.ACTIVE);
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(customer, id);
        return customer;
    }
}
