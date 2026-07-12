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
import com.bayerwestphalian.campaign.product.OwnershipStatus;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
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
 * KB item 192 / FR-073 acceptance: segment filter by product ownership works.
 *
 * <p>Proves product_type, product_id, and ownership_status filters (including aliases such as
 * {@code product_ownership} and {@code owned_product_type}) correctly select active customer
 * profiles via EQUALS / NOT_EQUALS / IN, including preview and AND combinations.
 */
@ExtendWith(MockitoExtension.class)
class SegmentFilterByProductOwnershipWorksTests {

    private static final UUID ID_LIFE = UUID.fromString("20000000-0000-0000-0000-000000000501");
    private static final UUID ID_HOME = UUID.fromString("20000000-0000-0000-0000-000000000502");
    private static final UUID ID_FUND = UUID.fromString("20000000-0000-0000-0000-000000000503");
    private static final UUID ID_NONE = UUID.fromString("20000000-0000-0000-0000-000000000504");
    private static final UUID ID_EXPIRED = UUID.fromString("20000000-0000-0000-0000-000000000505");
    private static final UUID ID_CANCELLED =
            UUID.fromString("20000000-0000-0000-0000-000000000506");

    private static final UUID PRODUCT_LIFE_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000501");
    private static final UUID PRODUCT_HOME_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000502");
    private static final UUID PRODUCT_FUND_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000503");
    private static final UUID PRODUCT_AUTO_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000504");

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

    @ParameterizedTest(name = "EQUALS product_type {0}")
    @CsvSource({
        "LIFE_INSURANCE,Tom Life",
        "life_insurance,Tom Life",
        "HOMEOWNER_INSURANCE,Home Owner",
        "INVESTMENT_FUND,Fund Investor"
    })
    void equalsProductTypeFilterMatchesOnlyOwnersOfThatType(String filterValue, String expectedName)
            throws Exception {
        stubOwnershipProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(productTypeEquals(filterValue));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo(expectedName);
    }

    @Test
    void notEqualsProductTypeExcludesOwnersAndKeepsNonOwners() throws Exception {
        stubOwnershipProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "product_type",
                                        SegmentOperator.NOT_EQUALS,
                                        "LIFE_INSURANCE",
                                        "ownership",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder(
                        "Home Owner",
                        "Fund Investor",
                        "No Ownership",
                        "Expired Policy",
                        "Cancelled Policy");
        assertThat(matches).noneMatch(view -> view.fullName().equals("Tom Life"));
    }

    @Test
    void inOperatorMatchesAnyListedActiveProductType() throws Exception {
        stubOwnershipProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "product_type",
                                        SegmentOperator.IN,
                                        "LIFE_INSURANCE, INVESTMENT_FUND",
                                        "ownership",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Tom Life", "Fund Investor");
    }

    @Test
    void productOwnershipAliasFieldNameFiltersByProductType() throws Exception {
        stubOwnershipProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "product_ownership",
                                        SegmentOperator.EQUALS,
                                        "HOMEOWNER_INSURANCE",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Home Owner");
    }

    @Test
    void ownedProductTypeAliasWorksWithInOperator() throws Exception {
        stubOwnershipProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "owned_product_type",
                                        SegmentOperator.IN,
                                        "homeowner_insurance,auto_insurance",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Home Owner");
    }

    @Test
    void equalsProductIdFilterMatchesActiveOwnerOfProduct() throws Exception {
        stubOwnershipProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "product_id",
                                        SegmentOperator.EQUALS,
                                        PRODUCT_LIFE_ID.toString(),
                                        "ownership",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Tom Life");
    }

    @Test
    void inProductIdFilterMatchesAnyListedProduct() throws Exception {
        stubOwnershipProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "product_id",
                                        SegmentOperator.IN,
                                        PRODUCT_HOME_ID + "," + PRODUCT_FUND_ID,
                                        "ownership",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Home Owner", "Fund Investor");
    }

    @Test
    void equalsOwnershipStatusActiveMatchesOnlyActiveOwnerships() throws Exception {
        stubOwnershipProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "ownership_status",
                                        SegmentOperator.EQUALS,
                                        "ACTIVE",
                                        "ownership",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Tom Life", "Home Owner", "Fund Investor");
        assertThat(matches).noneMatch(view -> view.fullName().equals("Expired Policy"));
        assertThat(matches).noneMatch(view -> view.fullName().equals("Cancelled Policy"));
    }

    @Test
    void equalsOwnershipStatusExpiredAndCancelledWork() throws Exception {
        stubOwnershipProfiles();

        List<CustomerView> expired =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "ownership_status",
                                        SegmentOperator.EQUALS,
                                        "EXPIRED",
                                        null,
                                        SegmentJoinOperator.AND)));
        List<CustomerView> cancelled =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "ownership_status",
                                        SegmentOperator.EQUALS,
                                        "CANCELLED",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(expired).extracting(CustomerView::fullName).containsExactly("Expired Policy");
        assertThat(cancelled)
                .extracting(CustomerView::fullName)
                .containsExactly("Cancelled Policy");
    }

    @Test
    void expiredOwnershipDoesNotMatchActiveProductTypeFilter() throws Exception {
        stubOwnershipProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(productTypeEquals("AUTO_INSURANCE"));

        assertThat(matches).isEmpty();
    }

    @Test
    void customersWithoutOwnershipDoNotMatchEqualsProductType() throws Exception {
        Customer none = profile("No", "Ownership", ID_NONE);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(none));
        when(productOwnershipRepository.findByCustomerId(ID_NONE)).thenReturn(List.of());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(productTypeEquals("LIFE_INSURANCE"));

        assertThat(matches).isEmpty();
    }

    @Test
    void previewSegmentAppliesProductTypeFilterAndReturnsEligibleMatches() throws Exception {
        stubOwnershipProfiles();

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(productTypeEquals("LIFE_INSURANCE")));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Tom Life");
    }

    @Test
    void previewWithProductTypeAndOwnershipStatusAndWorks() throws Exception {
        stubOwnershipProfiles();

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "product_type",
                                                SegmentOperator.EQUALS,
                                                "HOMEOWNER_INSURANCE",
                                                "ownership",
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "ownership_status",
                                                SegmentOperator.EQUALS,
                                                "ACTIVE",
                                                "ownership",
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Home Owner");
    }

    @Test
    void productTypeAndCityAndCombinationWorks() throws Exception {
        Customer munichLife = profile("Lena", "Mueller", ID_LIFE, "Munich");
        Customer berlinLife = profile("Tom", "Schmidt", ID_HOME, "Berlin");
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(munichLife, berlinLife));
        when(productOwnershipRepository.findByCustomerId(ID_LIFE))
                .thenReturn(
                        List.of(
                                activeOwnership(
                                        munichLife, ProductType.LIFE_INSURANCE, PRODUCT_LIFE_ID)));
        when(productOwnershipRepository.findByCustomerId(ID_HOME))
                .thenReturn(
                        List.of(
                                activeOwnership(
                                        berlinLife, ProductType.LIFE_INSURANCE, PRODUCT_HOME_ID)));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "product_type",
                                        SegmentOperator.EQUALS,
                                        "LIFE_INSURANCE",
                                        "ownership",
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

    @ParameterizedTest
    @EnumSource(ProductType.class)
    void everyKbProductTypeValueIsAcceptedAndNormalized(ProductType productType) {
        String normalized =
                SegmentProductOwnershipSupport.normalizeFilterValue(
                        SegmentOperator.EQUALS, "product_type", productType.name().toLowerCase());
        assertThat(normalized).isEqualTo(productType.name());
        SegmentProductOwnershipSupport.validateFilterValue(
                SegmentOperator.EQUALS, "product_type", productType.name());
    }

    @ParameterizedTest
    @EnumSource(OwnershipStatus.class)
    void everyOwnershipStatusValueIsAcceptedAndNormalized(OwnershipStatus status) {
        String normalized =
                SegmentProductOwnershipSupport.normalizeFilterValue(
                        SegmentOperator.EQUALS, "ownership_status", status.name().toLowerCase());
        assertThat(normalized).isEqualTo(status.name());
        SegmentProductOwnershipSupport.validateFilterValue(
                SegmentOperator.EQUALS, "ownership_status", status.name());
    }

    @Test
    void unsupportedProductTypeIsRejectedOnFindMatchingCustomers() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        productTypeEquals("TRAVEL_INSURANCE")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void invalidProductIdIsRejectedOnFindMatchingCustomers() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "product_id",
                                                        SegmentOperator.EQUALS,
                                                        "not-a-uuid",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void productOwnershipSupportRecognizesKbFields() {
        assertThat(SegmentProductOwnershipSupport.isProductOwnershipField("product_type")).isTrue();
        assertThat(SegmentProductOwnershipSupport.isProductOwnershipField("product_id")).isTrue();
        assertThat(SegmentProductOwnershipSupport.isProductOwnershipField("ownership_status"))
                .isTrue();
        assertThat(SegmentProductOwnershipSupport.canonicalizeFieldName("product_ownership"))
                .isEqualTo("product_type");
        assertThat(SegmentProductOwnershipSupport.canonicalizeFieldName("owned_product_id"))
                .isEqualTo("product_id");
    }

    private void stubOwnershipProfiles() throws Exception {
        Customer life = profile("Tom", "Life", ID_LIFE);
        Customer home = profile("Home", "Owner", ID_HOME);
        Customer fund = profile("Fund", "Investor", ID_FUND);
        Customer none = profile("No", "Ownership", ID_NONE);
        Customer expiredOwner = profile("Expired", "Policy", ID_EXPIRED);
        Customer cancelledOwner = profile("Cancelled", "Policy", ID_CANCELLED);

        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(life, home, fund, none, expiredOwner, cancelledOwner));

        when(productOwnershipRepository.findByCustomerId(ID_LIFE))
                .thenReturn(
                        List.of(
                                activeOwnership(
                                        life, ProductType.LIFE_INSURANCE, PRODUCT_LIFE_ID)));
        when(productOwnershipRepository.findByCustomerId(ID_HOME))
                .thenReturn(
                        List.of(
                                activeOwnership(
                                        home, ProductType.HOMEOWNER_INSURANCE, PRODUCT_HOME_ID)));
        when(productOwnershipRepository.findByCustomerId(ID_FUND))
                .thenReturn(
                        List.of(
                                activeOwnership(
                                        fund, ProductType.INVESTMENT_FUND, PRODUCT_FUND_ID)));
        when(productOwnershipRepository.findByCustomerId(ID_NONE)).thenReturn(List.of());

        ProductOwnership expired =
                activeOwnership(expiredOwner, ProductType.AUTO_INSURANCE, PRODUCT_AUTO_ID);
        expired.expire();
        when(productOwnershipRepository.findByCustomerId(ID_EXPIRED)).thenReturn(List.of(expired));

        ProductOwnership cancelled =
                activeOwnership(cancelledOwner, ProductType.HEALTH_INSURANCE, PRODUCT_AUTO_ID);
        cancelled.cancel();
        when(productOwnershipRepository.findByCustomerId(ID_CANCELLED))
                .thenReturn(List.of(cancelled));
    }

    private static List<CreateSegmentCriteriaCommand> productTypeEquals(String value) {
        return List.of(
                new CreateSegmentCriteriaCommand(
                        "product_type",
                        SegmentOperator.EQUALS,
                        value,
                        "ownership",
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

    private static ProductOwnership activeOwnership(
            Customer customer, ProductType productType, UUID productId) {
        Product product =
                Product.create(
                        "Product " + productType.name(), productType, new BigDecimal("99.00"), 12);
        setEntityId(product, productId);
        return ProductOwnership.create(
                customer, product, LocalDate.now().minusMonths(6), LocalDate.now().plusYears(1));
    }

    private static void setEntityId(BaseEntity entity, UUID id) {
        try {
            Field idField = BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Failed to assign entity id for test fixture", exception);
        }
    }
}
