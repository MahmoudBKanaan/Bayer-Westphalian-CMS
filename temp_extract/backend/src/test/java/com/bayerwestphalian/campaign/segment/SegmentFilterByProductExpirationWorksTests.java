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
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
 * KB item 195 / FR-076 acceptance: segment filter by product expiration works.
 *
 * <p>Proves expiring_within_months (KB 3/6/12 windows and aliases), expiration_date, and is_expiring
 * filters correctly select active ownerships via EQUALS / NOT_EQUALS / IN / BEFORE / AFTER /
 * BETWEEN, including segment preview and multi-criteria AND combinations.
 */
@ExtendWith(MockitoExtension.class)
class SegmentFilterByProductExpirationWorksTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 9);
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-09T12:00:00Z"), ZoneOffset.UTC);

    private static final UUID ID_2M = UUID.fromString("20000000-0000-0000-0000-000000000801");
    private static final UUID ID_5M = UUID.fromString("20000000-0000-0000-0000-000000000802");
    private static final UUID ID_10M = UUID.fromString("20000000-0000-0000-0000-000000000803");
    private static final UUID ID_18M = UUID.fromString("20000000-0000-0000-0000-000000000804");
    private static final UUID ID_NONE = UUID.fromString("20000000-0000-0000-0000-000000000805");
    private static final UUID ID_CANCELLED =
            UUID.fromString("20000000-0000-0000-0000-000000000806");

    private static final LocalDate DATE_2M = TODAY.plusMonths(2);
    private static final LocalDate DATE_5M = TODAY.plusMonths(5);
    private static final LocalDate DATE_10M = TODAY.plusMonths(10);
    private static final LocalDate DATE_18M = TODAY.plusMonths(18);

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
                        auditService,
                        FIXED_CLOCK);
        lenient()
                .when(eligibilityService.evaluateForSegmentPreview(any(UUID.class)))
                .thenReturn(EligibilityDecision.included());
        lenient().when(authorizationExpressions.isAuthenticated()).thenReturn(true);
    }

    @ParameterizedTest(name = "EQUALS expiring_within_months {0} matches {1}")
    @CsvSource({
        "3, 'Soon Two Months'",
        "6, 'Soon Two Months'",
        "6, 'Mid Five Months'",
        "12, 'Soon Two Months'",
        "12, 'Mid Five Months'",
        "12, 'Later Ten Months'"
    })
    void equalsExpiringWithinMonthsKbWindowsMatchCorrectOwners(
            String months, String expectedName) throws Exception {
        stubExpirationProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(expiringWithinMonthsEquals(months));

        assertThat(matches).extracting(CustomerView::fullName).contains(expectedName);
    }

    @Test
    void equalsThreeMonthsOnlyMatchesOwnershipsInsideThreeMonthWindow() throws Exception {
        stubExpirationProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(expiringWithinMonthsEquals("3"));

        assertThat(matches).extracting(CustomerView::fullName).containsExactly("Soon Two Months");
        assertThat(matches).noneMatch(view -> view.fullName().equals("Mid Five Months"));
        assertThat(matches).noneMatch(view -> view.fullName().equals("Far Eighteen Months"));
    }

    @Test
    void equalsSixMonthsMatchesTwoAndFiveMonthExpirations() throws Exception {
        stubExpirationProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(expiringWithinMonthsEquals("6"));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Soon Two Months", "Mid Five Months");
    }

    @Test
    void equalsTwelveMonthsMatchesAllWithinYearWindow() throws Exception {
        stubExpirationProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(expiringWithinMonthsEquals("12"));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder(
                        "Soon Two Months", "Mid Five Months", "Later Ten Months");
        assertThat(matches).noneMatch(view -> view.fullName().equals("Far Eighteen Months"));
    }

    @Test
    void notEqualsExpiringWithinMonthsKeepsOwnersOutsideWindow() throws Exception {
        stubExpirationProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "expiring_within_months",
                                        SegmentOperator.NOT_EQUALS,
                                        "6",
                                        "expiration",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .contains(
                        "Later Ten Months",
                        "Far Eighteen Months",
                        "No Ownership",
                        "Cancelled Soon");
        assertThat(matches).noneMatch(view -> view.fullName().equals("Soon Two Months"));
        assertThat(matches).noneMatch(view -> view.fullName().equals("Mid Five Months"));
    }

    @Test
    void inOperatorMatchesAnyKbMonthWindow() throws Exception {
        stubExpirationProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "expiring_within_months",
                                        SegmentOperator.IN,
                                        "3, 6, 12",
                                        "expiration",
                                        SegmentJoinOperator.AND)));

        // IN is true if any listed window matches; 12 covers 2m/5m/10m
        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder(
                        "Soon Two Months", "Mid Five Months", "Later Ten Months");
    }

    @Test
    void productExpirationAliasFiltersByExpiringWithinMonths() throws Exception {
        stubExpirationProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "product_expiration",
                                        SegmentOperator.EQUALS,
                                        "3",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).extracting(CustomerView::fullName).containsExactly("Soon Two Months");
    }

    @Test
    void isExpiringTrueUsesTwelveMonthWindow() throws Exception {
        stubExpirationProfiles();

        List<CustomerView> expiring =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "is_expiring",
                                        SegmentOperator.EQUALS,
                                        "true",
                                        "expiration",
                                        SegmentJoinOperator.AND)));
        List<CustomerView> notExpiring =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "is_expiring",
                                        SegmentOperator.EQUALS,
                                        "false",
                                        "expiration",
                                        SegmentJoinOperator.AND)));

        assertThat(expiring)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder(
                        "Soon Two Months", "Mid Five Months", "Later Ten Months");
        assertThat(notExpiring)
                .extracting(CustomerView::fullName)
                .contains(
                        "Far Eighteen Months", "No Ownership", "Cancelled Soon");
    }

    @Test
    void productExpiringAliasWorksForIsExpiring() throws Exception {
        stubExpirationProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "product_expiring",
                                        SegmentOperator.EQUALS,
                                        "true",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .contains("Soon Two Months", "Mid Five Months", "Later Ten Months");
    }

    @Test
    void expirationDateEqualsAndBeforeAfterFiltersWork() throws Exception {
        stubExpirationProfiles();

        List<CustomerView> equalsDate =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "expiration_date",
                                        SegmentOperator.EQUALS,
                                        DATE_5M.toString(),
                                        "expiration",
                                        SegmentJoinOperator.AND)));
        List<CustomerView> before =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "expiration_date",
                                        SegmentOperator.BEFORE,
                                        DATE_10M.toString(),
                                        "expiration",
                                        SegmentJoinOperator.AND)));
        List<CustomerView> after =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "expiration_date",
                                        SegmentOperator.AFTER,
                                        DATE_10M.toString(),
                                        "expiration",
                                        SegmentJoinOperator.AND)));

        assertThat(equalsDate).extracting(CustomerView::fullName).containsExactly("Mid Five Months");
        assertThat(before)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Soon Two Months", "Mid Five Months");
        assertThat(after)
                .extracting(CustomerView::fullName)
                .contains("Far Eighteen Months");
    }

    @Test
    void expirationDateBetweenFilterWorks() throws Exception {
        stubExpirationProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "expiration_date",
                                        SegmentOperator.BETWEEN,
                                        DATE_2M + ".." + DATE_10M,
                                        "expiration",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder(
                        "Soon Two Months", "Mid Five Months", "Later Ten Months");
    }

    @Test
    void cancelledOwnershipDoesNotMatchExpiringWithinMonths() throws Exception {
        stubExpirationProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(expiringWithinMonthsEquals("3"));

        assertThat(matches).noneMatch(view -> view.fullName().equals("Cancelled Soon"));
    }

    @Test
    void customersWithoutOwnershipDoNotMatchEqualsExpiringWithinMonths() throws Exception {
        Customer none = profile("No", "Ownership", ID_NONE);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(none));
        when(productOwnershipRepository.findByCustomerId(ID_NONE)).thenReturn(List.of());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(expiringWithinMonthsEquals("12"));

        assertThat(matches).isEmpty();
    }

    @Test
    void previewSegmentAppliesExpiringWithinMonthsAndReturnsEligibleMatches() throws Exception {
        stubExpirationProfiles();

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(expiringWithinMonthsEquals("3")));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Soon Two Months");
    }

    @Test
    void previewWithIsExpiringAndExpirationDateAndWorks() throws Exception {
        stubExpirationProfiles();

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "is_expiring",
                                                SegmentOperator.EQUALS,
                                                "true",
                                                "expiration",
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "expiration_date",
                                                SegmentOperator.BEFORE,
                                                DATE_10M.toString(),
                                                "expiration",
                                                SegmentJoinOperator.AND))));

        assertThat(preview.matchingCustomers())
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Soon Two Months", "Mid Five Months");
    }

    @Test
    void expiringWithinMonthsAndCityAndCombinationWorks() throws Exception {
        Customer munichSoon = profile("Lena", "Mueller", ID_2M, "Munich");
        Customer berlinSoon = profile("Tom", "Schmidt", ID_5M, "Berlin");
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(munichSoon, berlinSoon));
        when(productOwnershipRepository.findByCustomerId(ID_2M))
                .thenReturn(List.of(ownershipExpiringOn(munichSoon, DATE_2M)));
        when(productOwnershipRepository.findByCustomerId(ID_5M))
                .thenReturn(List.of(ownershipExpiringOn(berlinSoon, DATE_2M)));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "expiring_within_months",
                                        SegmentOperator.EQUALS,
                                        "3",
                                        "expiration",
                                        SegmentJoinOperator.AND),
                                new CreateSegmentCriteriaCommand(
                                        "city",
                                        SegmentOperator.EQUALS,
                                        "Munich",
                                        "location",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Lena Mueller");
        assertThat(matches.getFirst().city()).isEqualTo("Munich");
    }

    @Test
    void kbMonthWindowsThreeSixTwelveAreAccepted() {
        for (String months : List.of("3", "6", "12")) {
            String normalized =
                    SegmentProductExpirationSupport.normalizeFilterValue(
                            SegmentOperator.EQUALS, "expiring_within_months", months);
            assertThat(normalized).isEqualTo(months);
            SegmentProductExpirationSupport.validateFilterValue(
                    SegmentOperator.EQUALS, "expiring_within_months", months);
        }
        SegmentProductExpirationSupport.validateFilterValue(
                SegmentOperator.IN, "expiring_within_months", "3,6,12");
    }

    @Test
    void unsupportedNegativeMonthsRejectedOnFindMatchingCustomers() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        expiringWithinMonthsEquals("-1")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void invalidExpirationDateRejectedOnFindMatchingCustomers() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "expiration_date",
                                                        SegmentOperator.EQUALS,
                                                        "next-year",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void invalidIsExpiringValueRejectedOnFindMatchingCustomers() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "is_expiring",
                                                        SegmentOperator.EQUALS,
                                                        "maybe",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void productExpirationSupportRecognizesKbFieldsAndAliases() {
        assertThat(SegmentProductExpirationSupport.isProductExpirationField("expiring_within_months"))
                .isTrue();
        assertThat(SegmentProductExpirationSupport.isProductExpirationField("product_expiration"))
                .isTrue();
        assertThat(SegmentProductExpirationSupport.isProductExpirationField("expiration_date"))
                .isTrue();
        assertThat(SegmentProductExpirationSupport.isProductExpirationField("is_expiring")).isTrue();
        assertThat(SegmentProductExpirationSupport.canonicalizeFieldName("product_expiration"))
                .isEqualTo("expiring_within_months");
        assertThat(SegmentProductExpirationSupport.canonicalizeFieldName("product_expiring"))
                .isEqualTo("is_expiring");
        assertThat(
                        SegmentProductExpirationSupport.isExpiringWithinMonths(
                                ownershipExpiringOn(profileLoose(), DATE_2M), 3, TODAY))
                .isTrue();
    }

    private void stubExpirationProfiles() throws Exception {
        Customer soon = profile("Soon", "Two Months", ID_2M);
        Customer mid = profile("Mid", "Five Months", ID_5M);
        Customer later = profile("Later", "Ten Months", ID_10M);
        Customer far = profile("Far", "Eighteen Months", ID_18M);
        Customer none = profile("No", "Ownership", ID_NONE);
        Customer cancelled = profile("Cancelled", "Soon", ID_CANCELLED);

        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(soon, mid, later, far, none, cancelled));

        when(productOwnershipRepository.findByCustomerId(ID_2M))
                .thenReturn(List.of(ownershipExpiringOn(soon, DATE_2M)));
        when(productOwnershipRepository.findByCustomerId(ID_5M))
                .thenReturn(List.of(ownershipExpiringOn(mid, DATE_5M)));
        when(productOwnershipRepository.findByCustomerId(ID_10M))
                .thenReturn(List.of(ownershipExpiringOn(later, DATE_10M)));
        when(productOwnershipRepository.findByCustomerId(ID_18M))
                .thenReturn(List.of(ownershipExpiringOn(far, DATE_18M)));
        when(productOwnershipRepository.findByCustomerId(ID_NONE)).thenReturn(List.of());

        ProductOwnership cancelledOwnership = ownershipExpiringOn(cancelled, DATE_2M);
        cancelledOwnership.cancel();
        when(productOwnershipRepository.findByCustomerId(ID_CANCELLED))
                .thenReturn(List.of(cancelledOwnership));
    }

    private static List<CreateSegmentCriteriaCommand> expiringWithinMonthsEquals(String months) {
        return List.of(
                new CreateSegmentCriteriaCommand(
                        "expiring_within_months",
                        SegmentOperator.EQUALS,
                        months,
                        "expiration",
                        SegmentJoinOperator.AND));
    }

    private static Customer profile(String first, String last, UUID id) throws Exception {
        return profile(first, last, id, "Berlin");
    }

    private static Customer profile(String first, String last, UUID id, String city)
            throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, first, last);
        customer.updateAddress(null, city, "Germany");
        customer.changeStatus(CustomerStatus.ACTIVE);
        setEntityId(customer, id);
        return customer;
    }

    private static Customer profileLoose() {
        return Customer.create(CustomerType.CUSTOMER, "Loose", "Owner");
    }

    private static ProductOwnership ownershipExpiringOn(Customer customer, LocalDate expirationDate) {
        Product product =
                Product.create(
                        "Expiring Product " + expirationDate,
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("99.00"),
                        12);
        return ProductOwnership.create(
                customer, product, TODAY.minusMonths(6), expirationDate);
    }

    private static void setEntityId(BaseEntity entity, UUID id) {
        try {
            Field idField = BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to assign entity id for test fixture", exception);
        }
    }
}
