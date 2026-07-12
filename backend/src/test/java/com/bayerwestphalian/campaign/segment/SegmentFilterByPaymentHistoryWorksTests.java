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
import com.bayerwestphalian.campaign.product.PaymentRecord;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.PaymentStatus;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.UserRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
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
 * KB item 193 / FR-074 acceptance: segment filter by payment history works.
 *
 * <p>Proves payment_status (and payment_history alias), reminder_count, days_overdue, and
 * default_risk filters correctly select active profiles via EQUALS / NOT_EQUALS / IN / AFTER /
 * BEFORE / BETWEEN, including segment preview and multi-criteria AND combinations.
 */
@ExtendWith(MockitoExtension.class)
class SegmentFilterByPaymentHistoryWorksTests {

    private static final UUID ID_DUE = UUID.fromString("20000000-0000-0000-0000-000000000601");
    private static final UUID ID_PAID = UUID.fromString("20000000-0000-0000-0000-000000000602");
    private static final UUID ID_OVERDUE = UUID.fromString("20000000-0000-0000-0000-000000000603");
    private static final UUID ID_DEFAULT_RISK =
            UUID.fromString("20000000-0000-0000-0000-000000000604");
    private static final UUID ID_NONE = UUID.fromString("20000000-0000-0000-0000-000000000605");

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

    @ParameterizedTest(name = "EQUALS payment_status {0} matches {1}")
    @CsvSource({
        "DUE,Due Customer",
        "due,Due Customer",
        "PAID,Paid Customer",
        "OVERDUE,Overdue Customer",
        "overdue,Overdue Customer",
        "DEFAULT_RISK,Risk Customer"
    })
    void equalsPaymentStatusFilterMatchesOnlyThatStatus(String filterValue, String expectedName)
            throws Exception {
        stubPaymentProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(paymentStatusEquals(filterValue));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo(expectedName);
    }

    @Test
    void notEqualsPaymentStatusExcludesTargetStatus() throws Exception {
        stubPaymentProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "payment_status",
                                        SegmentOperator.NOT_EQUALS,
                                        "OVERDUE",
                                        "payment",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder(
                        "Due Customer", "Paid Customer", "Risk Customer", "No Payments");
        assertThat(matches).noneMatch(view -> view.fullName().equals("Overdue Customer"));
    }

    @Test
    void inOperatorMatchesAnyListedPaymentStatus() throws Exception {
        stubPaymentProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "payment_status",
                                        SegmentOperator.IN,
                                        "OVERDUE, DEFAULT_RISK",
                                        "payment",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Overdue Customer", "Risk Customer");
    }

    @Test
    void paymentHistoryAliasFieldNameFiltersByPaymentStatus() throws Exception {
        stubPaymentProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "payment_history",
                                        SegmentOperator.EQUALS,
                                        "PAID",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().fullName()).isEqualTo("Paid Customer");
    }

    @Test
    void paymentHistoryAliasNotEqualsDefaultRiskKeepsNonDefaultRisk() throws Exception {
        stubPaymentProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "payment_history",
                                        SegmentOperator.NOT_EQUALS,
                                        "DEFAULT_RISK",
                                        null,
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder(
                        "Due Customer", "Paid Customer", "Overdue Customer", "No Payments");
        assertThat(matches).noneMatch(view -> view.fullName().equals("Risk Customer"));
    }

    @Test
    void reminderCountEqualsAndAfterFiltersWork() throws Exception {
        stubPaymentProfiles();

        List<CustomerView> equalsThree =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "reminder_count",
                                        SegmentOperator.EQUALS,
                                        "3",
                                        "payment",
                                        SegmentJoinOperator.AND)));
        List<CustomerView> afterTwo =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "reminder_count",
                                        SegmentOperator.AFTER,
                                        "2",
                                        "payment",
                                        SegmentJoinOperator.AND)));
        List<CustomerView> equalsZero =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "reminder_count",
                                        SegmentOperator.EQUALS,
                                        "0",
                                        "payment",
                                        SegmentJoinOperator.AND)));

        assertThat(equalsThree).extracting(CustomerView::fullName).containsExactly("Risk Customer");
        assertThat(afterTwo).extracting(CustomerView::fullName).containsExactly("Risk Customer");
        assertThat(equalsZero)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder(
                        "Due Customer", "Paid Customer", "Overdue Customer", "No Payments");
    }

    @Test
    void reminderCountBetweenFilterWorks() throws Exception {
        stubPaymentProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "reminder_count",
                                        SegmentOperator.BETWEEN,
                                        "1,3",
                                        "payment",
                                        SegmentJoinOperator.AND)));

        assertThat(matches).extracting(CustomerView::fullName).containsExactly("Risk Customer");
    }

    @Test
    void daysOverdueAfterFilterMatchesOverdueAndDefaultRisk() throws Exception {
        stubPaymentProfiles();

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "days_overdue",
                                        SegmentOperator.AFTER,
                                        "0",
                                        "payment",
                                        SegmentJoinOperator.AND)));

        assertThat(matches)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder("Due Customer", "Overdue Customer", "Risk Customer");
        // Paid has 0 days overdue; no-payments also 0
        assertThat(matches).noneMatch(view -> view.fullName().equals("Paid Customer"));
        assertThat(matches).noneMatch(view -> view.fullName().equals("No Payments"));
    }

    @Test
    void defaultRiskBooleanFilterWorks() throws Exception {
        stubPaymentProfiles();

        List<CustomerView> atRisk =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "default_risk",
                                        SegmentOperator.EQUALS,
                                        "true",
                                        "payment",
                                        SegmentJoinOperator.AND)));
        List<CustomerView> notAtRisk =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "default_risk",
                                        SegmentOperator.EQUALS,
                                        "false",
                                        "payment",
                                        SegmentJoinOperator.AND)));

        assertThat(atRisk).extracting(CustomerView::fullName).containsExactly("Risk Customer");
        assertThat(notAtRisk)
                .extracting(CustomerView::fullName)
                .containsExactlyInAnyOrder(
                        "Due Customer", "Paid Customer", "Overdue Customer", "No Payments");
    }

    @Test
    void customersWithoutPaymentsDoNotMatchEqualsOverdue() throws Exception {
        Customer none = profile("No", "Payments", ID_NONE);
        when(customerRepository.findActiveProfiles()).thenReturn(List.of(none));
        when(paymentRecordRepository.findByCustomerId(ID_NONE)).thenReturn(List.of());

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(paymentStatusEquals("OVERDUE"));

        assertThat(matches).isEmpty();
    }

    @Test
    void previewSegmentAppliesPaymentStatusFilterAndReturnsEligibleMatches() throws Exception {
        stubPaymentProfiles();

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(paymentStatusEquals("OVERDUE")));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers()).hasSize(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Overdue Customer");
    }

    @Test
    void previewWithReminderCountAndDefaultRiskAndWorks() throws Exception {
        stubPaymentProfiles();

        SegmentPreviewView preview =
                segmentService.previewSegment(
                        new SegmentPreviewCommand(
                                List.of(
                                        new CreateSegmentCriteriaCommand(
                                                "reminder_count",
                                                SegmentOperator.AFTER,
                                                "2",
                                                "payment",
                                                SegmentJoinOperator.AND),
                                        new CreateSegmentCriteriaCommand(
                                                "default_risk",
                                                SegmentOperator.EQUALS,
                                                "true",
                                                "payment",
                                                SegmentJoinOperator.AND))));

        assertThat(preview.totalAudienceCount()).isEqualTo(1);
        assertThat(preview.matchingCustomers().getFirst().fullName()).isEqualTo("Risk Customer");
    }

    @Test
    void paymentStatusAndCityAndCombinationWorks() throws Exception {
        Customer munichOverdue = profile("Lena", "Mueller", ID_OVERDUE, "Munich");
        Customer berlinOverdue = profile("Tom", "Schmidt", ID_PAID, "Berlin");
        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(munichOverdue, berlinOverdue));
        when(paymentRecordRepository.findByCustomerId(ID_OVERDUE))
                .thenReturn(List.of(overduePayment(munichOverdue)));
        when(paymentRecordRepository.findByCustomerId(ID_PAID))
                .thenReturn(List.of(overduePayment(berlinOverdue)));

        List<CustomerView> matches =
                segmentService.findMatchingCustomers(
                        List.of(
                                new CreateSegmentCriteriaCommand(
                                        "payment_status",
                                        SegmentOperator.EQUALS,
                                        "OVERDUE",
                                        "payment",
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
    @EnumSource(PaymentStatus.class)
    void everyKbPaymentStatusValueIsAcceptedAndNormalized(PaymentStatus status) {
        String normalized =
                SegmentPaymentHistorySupport.normalizeFilterValue(
                        SegmentOperator.EQUALS, "payment_status", status.name().toLowerCase());
        assertThat(normalized).isEqualTo(status.name());
        SegmentPaymentHistorySupport.validateFilterValue(
                SegmentOperator.EQUALS, "payment_status", status.name());
    }

    @Test
    void unsupportedPaymentStatusIsRejectedOnFindMatchingCustomers() {
        assertThatThrownBy(
                        () -> segmentService.findMatchingCustomers(paymentStatusEquals("PENDING")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void negativeReminderCountIsRejectedOnFindMatchingCustomers() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "reminder_count",
                                                        SegmentOperator.EQUALS,
                                                        "-1",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void invalidDefaultRiskValueIsRejectedOnFindMatchingCustomers() {
        assertThatThrownBy(
                        () ->
                                segmentService.findMatchingCustomers(
                                        List.of(
                                                new CreateSegmentCriteriaCommand(
                                                        "default_risk",
                                                        SegmentOperator.EQUALS,
                                                        "maybe",
                                                        null,
                                                        SegmentJoinOperator.AND))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Segment criteria validation failed");
    }

    @Test
    void paymentHistorySupportRecognizesKbFields() {
        assertThat(SegmentPaymentHistorySupport.isPaymentHistoryField("payment_status")).isTrue();
        assertThat(SegmentPaymentHistorySupport.isPaymentHistoryField("payment_history")).isTrue();
        assertThat(SegmentPaymentHistorySupport.isPaymentHistoryField("reminder_count")).isTrue();
        assertThat(SegmentPaymentHistorySupport.isPaymentHistoryField("days_overdue")).isTrue();
        assertThat(SegmentPaymentHistorySupport.isPaymentHistoryField("default_risk")).isTrue();
        assertThat(SegmentPaymentHistorySupport.canonicalizeFieldName("payment_history"))
                .isEqualTo("payment_status");
        assertThat(SegmentPaymentHistorySupport.canonicalizeFieldName("defaultrisk"))
                .isEqualTo("default_risk");
    }

    private void stubPaymentProfiles() throws Exception {
        Customer due = profile("Due", "Customer", ID_DUE);
        Customer paid = profile("Paid", "Customer", ID_PAID);
        Customer overdue = profile("Overdue", "Customer", ID_OVERDUE);
        Customer risk = profile("Risk", "Customer", ID_DEFAULT_RISK);
        Customer none = profile("No", "Payments", ID_NONE);

        when(customerRepository.findActiveProfiles())
                .thenReturn(List.of(due, paid, overdue, risk, none));

        when(paymentRecordRepository.findByCustomerId(ID_DUE)).thenReturn(List.of(duePayment(due)));
        when(paymentRecordRepository.findByCustomerId(ID_PAID))
                .thenReturn(List.of(paidPayment(paid)));
        when(paymentRecordRepository.findByCustomerId(ID_OVERDUE))
                .thenReturn(List.of(overduePayment(overdue)));
        when(paymentRecordRepository.findByCustomerId(ID_DEFAULT_RISK))
                .thenReturn(List.of(defaultRiskPayment(risk)));
        when(paymentRecordRepository.findByCustomerId(ID_NONE)).thenReturn(List.of());
    }

    private static List<CreateSegmentCriteriaCommand> paymentStatusEquals(String value) {
        return List.of(
                new CreateSegmentCriteriaCommand(
                        "payment_status",
                        SegmentOperator.EQUALS,
                        value,
                        "payment",
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

    private static PaymentRecord duePayment(Customer customer) {
        return PaymentRecord.create(
                customer,
                ownership(customer, ProductType.LIFE_INSURANCE),
                LocalDate.now().minusDays(5),
                new BigDecimal("100.00"));
    }

    private static PaymentRecord paidPayment(Customer customer) {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        ownership(customer, ProductType.AUTO_INSURANCE),
                        LocalDate.now().minusDays(5),
                        new BigDecimal("80.00"));
        payment.markPaid(new BigDecimal("80.00"), Instant.now());
        return payment;
    }

    private static PaymentRecord overduePayment(Customer customer) {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        ownership(customer, ProductType.LIFE_INSURANCE),
                        LocalDate.now().minusDays(10),
                        new BigDecimal("120.00"));
        payment.markOverdue();
        return payment;
    }

    private static PaymentRecord defaultRiskPayment(Customer customer) {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        ownership(customer, ProductType.HOMEOWNER_INSURANCE),
                        LocalDate.now().minusDays(30),
                        new BigDecimal("200.00"));
        payment.incrementReminder();
        payment.incrementReminder();
        payment.incrementReminder();
        return payment;
    }

    private static ProductOwnership ownership(Customer customer, ProductType productType) {
        Product product =
                Product.create(
                        "Payment Product " + productType.name(),
                        productType,
                        new BigDecimal("99.00"),
                        12);
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
