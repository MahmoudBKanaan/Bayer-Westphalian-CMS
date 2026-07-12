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
 * KB item 191 / FR-071 acceptance: segment filter by location works.
 *
 * <p>Proves city, country, and address_line filters (plus {@code location} / {@code addressline}
 * aliases) correctly select active profiles via EQUALS, NOT_EQUALS, CONTAINS, and IN, including
 * segment preview and multi-criteria AND with other location fields.
 */
@ExtendWith(MockitoExtension.class)
class SegmentFilterByLocationWorksTests {

    private static final UUID ID_MUNICH_DE =
            UUID.fromString("20000000-0000-0000-0000-000000000401");
    private static final UUID ID_BERLIN_DE =
            UUID.fromString("20000000-0000-0000-0000-000000000402");
    private static final UUID ID_VIENNA_AT =
            UUID.fromString("20000000-0000-0000-0000-000000000403");
    private static final UUID ID_HAMBURG_DE =
            UUID.fromString("20000000-0000-0000-0000-000000000404");
    private static final UUID ID_NO_LOCATION =
            UUID.fromString("20000000-0000-0000-0000-000000000405");

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

    @ParameterizedTest(name = "EQUALS city {0} matches {1}")
    @CsvSource({"Munich,Lena Mueller", "Berlin,Tom Schmidt", "Vienna,Anna Weber"})
    void equalsCityFilterMatchesOnlyTargetCity(String city, String expectedName) throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allLocationProfiles());

        List<CustomerView> matches = segmentService.findMatchingCustomers(cityEqualsCriteria(city));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo(expectedName);
        assertThat(matches.getFirst().city()).isEqualToIgnoringCase(city);
    }

    @Test
    void equalsCountryFilterMatchesAllCustomersInCountry() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allLocationProfiles());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "country",
                                        SegmentOperator.EQUALS,
                                        "Germany",
                                        "location",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Tom Schmidt", "Max Bauer");
        assertThat(matches).allMatch(view -> "Germany".equalsIgnoreCase(view.country()));
    }

    @Test
    void equalsAddressLineFilterMatchesStreet() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allLocationProfiles());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "address_line",
                                        SegmentOperator.EQUALS,
                                        "Main Street 1",
                                        "location",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(matches.getFirst().addressLine()).isEqualTo("Main Street 1");
    }

    @Test
    void notEqualsCityExcludesTargetAndKeepsOthersWithCity() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allLocationProfiles());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.NOT_EQUALS,
                                        "Munich",
                                        "location",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::city)
                .containsExactlyInAnyOrder("Berlin", "Vienna", "Hamburg");
        assertThat(matches).noneMatch(view -> "Munich".equalsIgnoreCase(view.city()));
        assertThat(matches).noneMatch(view -> view.id().equals(ID_NO_LOCATION));
    }

    @Test
    void containsCityFilterIsCaseInsensitive() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allLocationProfiles());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.CONTAINS,
                                        "mun",
                                        "location",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().city()).isEqualTo("Munich");
    }

    @Test
    void inOperatorMatchesAnyListedCity() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allLocationProfiles());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.IN,
                                        " Munich , Vienna , Zurich ",
                                        "location",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Lena Mueller", "Anna Weber");
    }

    @Test
    void locationAliasFieldNameFiltersByCity() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allLocationProfiles());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "location",
                                        SegmentOperator.EQUALS,
                                        "Berlin",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Tom Schmidt");
        assertThat(matches.getFirst().city()).isEqualTo("Berlin");
    }

    @Test
    void addresslineAliasFieldNameFiltersByAddressLine() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allLocationProfiles());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "addressline",
                                        SegmentOperator.CONTAINS,
                                        "Ring",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Anna Weber");
        assertThat(matches.getFirst().addressLine()).isEqualTo("Ringstrasse 5");
    }

    @Test
    void customersWithoutCityDoNotMatchEqualsCityFilter() throws Exception {
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(profile("No", "City", ID_NO_LOCATION, null, null, null)));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(cityEqualsCriteria("Munich"));

        assertThat(matches).isEmpty();
    }

    @Test
    void previewSegmentAppliesCityFilterAndReturnsEligibleMatches() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allLocationProfiles());

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(cityEqualsCriteria("Munich")));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().city()).isEqualTo("Munich");
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
    }

    @Test
    void previewWithCountryAndCityAndReturnsIntersection() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allLocationProfiles());

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "country",
                                                SegmentOperator.EQUALS,
                                                "Germany",
                                                "location",
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "city",
                                                SegmentOperator.IN,
                                                "Munich,Vienna",
                                                "location",
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(preview.matchingCustomers().getFirst().country()).isEqualTo("Germany");
        assertThat(preview.matchingCustomers().getFirst().city()).isEqualTo("Munich");
    }

    @Test
    void cityAndCountryAndCombinationWorks() throws Exception {
        when(customerRepository.findActiveProfiles()).thenReturn(allLocationProfiles());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Vienna",
                                        "location",
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "country",
                                        SegmentOperator.EQUALS,
                                        "Austria",
                                        "location",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Anna Weber");
        assertThat(matches.getFirst().city()).isEqualTo("Vienna");
        assertThat(matches.getFirst().country()).isEqualTo("Austria");
    }

    @Test
    void blankCityFilterValueIsRejectedOnFindMatchingCustomers() {
        assertThatThrownBy(() -> segmentService.findMatchingCustomers(cityEqualsCriteria("  ")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void overlongCityFilterValueIsRejectedOnFindMatchingCustomers() {
        String overlong = "x".repeat(101);
        assertThatThrownBy(() -> segmentService.findMatchingCustomers(cityEqualsCriteria(overlong)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void locationSupportRecognizesAndNormalizesKbFields() {
        assertThat(SegmentLocationSupport.isLocationField("city")).isTrue();
        assertThat(SegmentLocationSupport.isLocationField("country")).isTrue();
        assertThat(SegmentLocationSupport.isLocationField("address_line")).isTrue();
        assertThat(SegmentLocationSupport.canonicalizeFieldName("location")).isEqualTo("city");
        assertThat(SegmentLocationSupport.canonicalizeFieldName("addressline"))
                .isEqualTo("address_line");
        assertThat(
                        SegmentLocationSupport.normalizeFilterValue(
                                SegmentOperator.IN, "city", " Munich , Berlin "))
                .isEqualTo("Munich,Berlin");
        SegmentLocationSupport.validateFilterValue(SegmentOperator.EQUALS, "country", "Germany");
    }

    private static List<CreateSegmentCriteriaCommand> cityEqualsCriteria(String value) {
        return List.of(
                new CreateSegmentCriteriaCommand(
                        "city",
                        SegmentOperator.EQUALS,
                        value,
                        "location",
                        SegmentJoinOperator.AND));
    }

    private static List<Customer> allLocationProfiles() throws Exception {
        return List.of(
                profile("Lena", "Mueller", ID_MUNICH_DE, "Main Street 1", "Munich", "Germany"),
                profile("Tom", "Schmidt", ID_BERLIN_DE, "Alexanderplatz 2", "Berlin", "Germany"),
                profile("Anna", "Weber", ID_VIENNA_AT, "Ringstrasse 5", "Vienna", "Austria"),
                profile("Max", "Bauer", ID_HAMBURG_DE, "Hafenstrasse 9", "Hamburg", "Germany"),
                profile("No", "Location", ID_NO_LOCATION, null, null, null));
    }

    private static Customer profile(
            String first, String last, UUID id, String addressLine, String city, String country)
            throws Exception {
        Customer customer = Customer.create(CustomerType.PROSPECT, first, last);
        customer.updateAddress(addressLine, city, country);
        customer.changeStatus(CustomerStatus.ACTIVE);
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(customer, id);
        return customer;
    }
}
