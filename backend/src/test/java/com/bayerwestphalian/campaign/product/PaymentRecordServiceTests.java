package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.audit.AuditService;
import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

@ExtendWith(MockitoExtension.class)
class PaymentRecordServiceTests {

    private static final UUID PAYMENT_ID = UUID.fromString("42000000-0000-0000-0000-000000000001");
    private static final UUID OWNERSHIP_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("41000000-0000-0000-0000-000000000101");
    private static final UUID OTHER_CUSTOMER_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000102");
    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000201");
    private static final Instant NOW = Instant.parse("2026-07-06T12:00:00Z");
    private static final LocalDate DUE_DATE = LocalDate.parse("2026-07-15");

    @Mock private PaymentRecordRepository paymentRecordRepository;

    @Mock private ProductOwnershipRepository productOwnershipRepository;

    @Mock private CustomerRepository customerRepository;

    @Mock private AuditService auditService;

    private PaymentRecordService paymentRecordService;

    @BeforeEach
    void setUp() {
        paymentRecordService =
                new PaymentRecordService(
                        paymentRecordRepository,
                        productOwnershipRepository,
                        customerRepository,
                        auditService,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void serviceMethodsDeclareMethodLevelAuthorization() throws Exception {
        assertPreAuthorizeWithExpression(
                "createPaymentRecord",
                new Class<?>[] {CreatePaymentRecordCommand.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT')");
        assertPreAuthorizeWithExpression(
                "updatePaymentRecord",
                new Class<?>[] {UUID.class, UpdatePaymentRecordCommand.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT')");
        assertPreAuthorizeWithExpression(
                "markPaid",
                new Class<?>[] {UUID.class, MarkPaymentPaidCommand.class},
                "@authz.hasAnyRole('ADMIN', 'CUSTOMER_SERVICE_AGENT')");
        assertPreAuthorize("markOverdue", UUID.class);
        assertPreAuthorize("incrementReminder", UUID.class);
        assertPreAuthorize("findDuePayments");
        assertPreAuthorize("findOverduePayments");
        assertPreAuthorize("listCustomerPayments", UUID.class);
        assertPreAuthorize("searchPayments", PaymentRecordSearchCriteria.class);
    }

    @Test
    void createsPaymentRecordFromKbCommandAndAuditsCreation() throws Exception {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Payer");
        ProductOwnership ownership = ownership(customer);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productOwnershipRepository.findById(OWNERSHIP_ID)).thenReturn(Optional.of(ownership));
        when(paymentRecordRepository.save(any(PaymentRecord.class)))
                .thenAnswer(
                        invocation -> {
                            PaymentRecord payment = invocation.getArgument(0);
                            setPaymentId(payment, PAYMENT_ID);
                            return payment;
                        });

        PaymentRecordView view =
                paymentRecordService.createPaymentRecord(
                        new CreatePaymentRecordCommand(
                                CUSTOMER_ID, OWNERSHIP_ID, DUE_DATE, new BigDecimal("129.99")));

        ArgumentCaptor<PaymentRecord> paymentCaptor = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(paymentRecordRepository).save(paymentCaptor.capture());
        PaymentRecord saved = paymentCaptor.getValue();
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getProductOwnership()).isSameAs(ownership);
        assertThat(saved.getDueDate()).isEqualTo(DUE_DATE);
        assertThat(saved.getAmountDue()).isEqualByComparingTo("129.99");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.DUE);
        assertThat(view.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(view.customerFullName()).isEqualTo("Ada Payer");
        assertThat(view.productOwnershipId()).isEqualTo(OWNERSHIP_ID);
        assertThat(view.productId()).isEqualTo(PRODUCT_ID);
        assertThat(view.productName()).isEqualTo("Life Protection");
        assertThat(view.dueDate()).isEqualTo(DUE_DATE);
        assertThat(view.amountDue()).isEqualByComparingTo("129.99");
        assertThat(view.status()).isEqualTo(PaymentStatus.DUE);
        assertThat(view.reminderCount()).isZero();
        assertThat(view.defaultRisk()).isFalse();
        verify(auditService)
                .logCreate(
                        eq((UUID) null),
                        eq("payment_records"),
                        eq(PAYMENT_ID),
                        eq(
                                Map.ofEntries(
                                        Map.entry("customerId", CUSTOMER_ID),
                                        Map.entry("productOwnershipId", OWNERSHIP_ID),
                                        Map.entry("dueDate", DUE_DATE.toString()),
                                        Map.entry("amountDue", new BigDecimal("129.99")),
                                        Map.entry("status", "DUE"),
                                        Map.entry("reminderCount", 0),
                                        Map.entry("defaultRisk", false))));
    }

    @Test
    void validatesCreatePaymentRecordCommand() {
        assertThatThrownBy(() -> paymentRecordService.createPaymentRecord(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment record validation failed");

        assertThatThrownBy(
                        () ->
                                paymentRecordService.createPaymentRecord(
                                        new CreatePaymentRecordCommand(
                                                null, null, null, new BigDecimal("-1.00"))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment record validation failed");
    }

    @Test
    void updatesPaymentRecordDetailsAndAuditsChange() throws Exception {
        PaymentRecord payment = duePayment();
        when(paymentRecordRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(paymentRecordRepository.save(payment)).thenReturn(payment);

        PaymentRecordView view =
                paymentRecordService.updatePaymentRecord(
                        PAYMENT_ID,
                        new UpdatePaymentRecordCommand(
                                LocalDate.parse("2026-08-01"), new BigDecimal("150.25")));

        assertThat(view.dueDate()).isEqualTo(LocalDate.parse("2026-08-01"));
        assertThat(view.amountDue()).isEqualByComparingTo("150.25");
        verify(paymentRecordRepository).save(payment);
        verify(auditService)
                .logUpdate(
                        eq((UUID) null),
                        eq("payment_records"),
                        eq(PAYMENT_ID),
                        any(Map.class),
                        any(Map.class));
    }

    @Test
    void rejectsInvalidOrPaidPaymentRecordUpdates() throws Exception {
        assertThatThrownBy(() -> paymentRecordService.updatePaymentRecord(null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment record validation failed");

        assertThatThrownBy(
                        () ->
                                paymentRecordService.updatePaymentRecord(
                                        PAYMENT_ID,
                                        new UpdatePaymentRecordCommand(
                                                null, new BigDecimal("-1.00"))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment record validation failed");

        PaymentRecord paidPayment = duePayment();
        paidPayment.markPaid(new BigDecimal("129.99"), NOW);
        when(paymentRecordRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(paidPayment));

        assertThatThrownBy(
                        () ->
                                paymentRecordService.updatePaymentRecord(
                                        PAYMENT_ID,
                                        new UpdatePaymentRecordCommand(
                                                DUE_DATE, new BigDecimal("129.99"))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment record validation failed");
    }

    @Test
    void rejectsOwnershipNotBelongingToCustomer() throws Exception {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Payer");
        Customer otherCustomer = customer(OTHER_CUSTOMER_ID, "Ben", "Other");
        ProductOwnership ownership = ownership(otherCustomer);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productOwnershipRepository.findById(OWNERSHIP_ID)).thenReturn(Optional.of(ownership));

        assertThatThrownBy(
                        () ->
                                paymentRecordService.createPaymentRecord(
                                        new CreatePaymentRecordCommand(
                                                CUSTOMER_ID,
                                                OWNERSHIP_ID,
                                                DUE_DATE,
                                                new BigDecimal("50.00"))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment record validation failed");
    }

    @Test
    void rejectsSoftDeletedCustomersWhenCreatingPaymentRecord() throws Exception {
        Customer deletedCustomer = customer(CUSTOMER_ID, "Ada", "Payer");
        deletedCustomer.markDeleted();
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(deletedCustomer));

        assertThatThrownBy(
                        () ->
                                paymentRecordService.createPaymentRecord(
                                        new CreatePaymentRecordCommand(
                                                CUSTOMER_ID,
                                                OWNERSHIP_ID,
                                                DUE_DATE,
                                                new BigDecimal("50.00"))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer was not found: " + CUSTOMER_ID);
    }

    @Test
    void marksPaymentPaidUsingClockWhenPaidAtIsOmitted() throws Exception {
        PaymentRecord payment = duePayment();
        when(paymentRecordRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(paymentRecordRepository.save(payment)).thenReturn(payment);

        PaymentRecordView view =
                paymentRecordService.markPaid(
                        PAYMENT_ID, new MarkPaymentPaidCommand(new BigDecimal("129.99"), null));

        assertThat(view.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(view.amountPaid()).isEqualByComparingTo("129.99");
        assertThat(view.paidAt()).isEqualTo(NOW);
        verify(paymentRecordRepository).save(payment);
        verify(auditService)
                .logUpdate(
                        eq((UUID) null),
                        eq("payment_records"),
                        eq(PAYMENT_ID),
                        any(Map.class),
                        any(Map.class));
    }

    @Test
    void marksPaymentPaidAndAuditsStatusTransitionFromDueToPaid() throws Exception {
        PaymentRecord payment = duePayment();
        when(paymentRecordRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(paymentRecordRepository.save(payment)).thenReturn(payment);

        PaymentRecordView view =
                paymentRecordService.markPaid(
                        PAYMENT_ID, new MarkPaymentPaidCommand(new BigDecimal("129.99"), NOW));

        assertThat(view.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(view.amountPaid()).isEqualByComparingTo("129.99");
        assertThat(view.paidAt()).isEqualTo(NOW);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> oldValueCaptor = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> newValueCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService)
                .logUpdate(
                        eq((UUID) null),
                        eq("payment_records"),
                        eq(PAYMENT_ID),
                        oldValueCaptor.capture(),
                        newValueCaptor.capture());

        Map<String, Object> oldValue = castAuditPayload(oldValueCaptor.getValue());
        Map<String, Object> newValue = castAuditPayload(newValueCaptor.getValue());

        assertThat(oldValue)
                .containsEntry("status", "DUE")
                .doesNotContainKey("amountPaid")
                .doesNotContainKey("paidAt");
        assertThat(newValue)
                .containsEntry("status", "PAID")
                .containsEntry("amountPaid", new BigDecimal("129.99"))
                .containsEntry("paidAt", NOW.toString());
    }

    @Test
    void marksPaymentPaidWithExplicitPaidAt() throws Exception {
        PaymentRecord payment = duePayment();
        Instant paidAt = Instant.parse("2026-07-10T09:30:00Z");
        when(paymentRecordRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(paymentRecordRepository.save(payment)).thenReturn(payment);

        PaymentRecordView view =
                paymentRecordService.markPaid(
                        PAYMENT_ID, new MarkPaymentPaidCommand(new BigDecimal("100.00"), paidAt));

        assertThat(view.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(view.amountPaid()).isEqualByComparingTo("100.00");
        assertThat(view.paidAt()).isEqualTo(paidAt);
    }

    @Test
    void marksPaymentOverdueAndAuditsUpdate() throws Exception {
        PaymentRecord payment = duePayment();
        when(paymentRecordRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(paymentRecordRepository.save(payment)).thenReturn(payment);

        PaymentRecordView view = paymentRecordService.markOverdue(PAYMENT_ID);

        assertThat(view.status()).isEqualTo(PaymentStatus.OVERDUE);
        verify(paymentRecordRepository).save(payment);
        verify(auditService)
                .logUpdate(
                        eq((UUID) null),
                        eq("payment_records"),
                        eq(PAYMENT_ID),
                        any(Map.class),
                        any(Map.class));
    }

    @Test
    void incrementsReminderAndMarksDefaultRiskAfterThirdReminder() throws Exception {
        PaymentRecord payment = duePayment();
        when(paymentRecordRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(paymentRecordRepository.save(payment)).thenReturn(payment);

        paymentRecordService.incrementReminder(PAYMENT_ID);
        paymentRecordService.incrementReminder(PAYMENT_ID);
        PaymentRecordView view = paymentRecordService.incrementReminder(PAYMENT_ID);

        assertThat(view.reminderCount()).isEqualTo(3);
        assertThat(view.status()).isEqualTo(PaymentStatus.DEFAULT_RISK);
        assertThat(view.defaultRisk()).isTrue();
    }

    @Test
    void rejectsOperationsOnAlreadyPaidPayments() throws Exception {
        PaymentRecord paidPayment = duePayment();
        paidPayment.markPaid(new BigDecimal("129.99"), NOW);
        when(paymentRecordRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(paidPayment));

        assertThatThrownBy(
                        () ->
                                paymentRecordService.markPaid(
                                        PAYMENT_ID,
                                        new MarkPaymentPaidCommand(new BigDecimal("129.99"), NOW)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment record validation failed");

        assertThatThrownBy(() -> paymentRecordService.markOverdue(PAYMENT_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment record validation failed");

        assertThatThrownBy(() -> paymentRecordService.incrementReminder(PAYMENT_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment record validation failed");
    }

    @Test
    void findsDueAndOverduePaymentsFromRepository() throws Exception {
        PaymentRecord duePayment = duePayment();
        PaymentRecord overduePayment = duePayment();
        overduePayment.markOverdue();
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of(overduePayment));

        List<PaymentRecordView> dueViews = paymentRecordService.findDuePayments();
        List<PaymentRecordView> overdueViews = paymentRecordService.findOverduePayments();

        assertThat(dueViews).hasSize(1);
        assertThat(dueViews.get(0).status()).isEqualTo(PaymentStatus.DUE);
        assertThat(overdueViews).hasSize(1);
        assertThat(overdueViews.get(0).status()).isEqualTo(PaymentStatus.OVERDUE);
        verify(paymentRecordRepository).findDuePayments();
        verify(paymentRecordRepository).findOverduePayments();
    }

    @Test
    void listsCustomerPaymentsInRepositoryOrder() throws Exception {
        PaymentRecord first = duePayment();
        PaymentRecord second = duePayment();
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(first, second));

        List<PaymentRecordView> views = paymentRecordService.listCustomerPayments(CUSTOMER_ID);

        assertThat(views).hasSize(2);
        assertThat(views.get(0).customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(views.get(1).customerId()).isEqualTo(CUSTOMER_ID);
        verify(paymentRecordRepository).findByCustomerId(CUSTOMER_ID);
    }

    @Test
    void searchesPaymentsWithKbFilters() throws Exception {
        PaymentRecord duePayment = duePayment();
        PaymentRecord paidPayment = duePayment();
        paidPayment.markPaid(new BigDecimal("129.99"), NOW);
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(duePayment, paidPayment));

        List<PaymentRecordView> views =
                paymentRecordService.searchPayments(
                        new PaymentRecordSearchCriteria(CUSTOMER_ID, PaymentStatus.DUE));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).status()).isEqualTo(PaymentStatus.DUE);
        verify(paymentRecordRepository).findByCustomerId(CUSTOMER_ID);
    }

    @Test
    void searchesDuePaymentsByStatusWithoutCustomerFilter() throws Exception {
        PaymentRecord duePayment = duePayment();
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));

        List<PaymentRecordView> views =
                paymentRecordService.searchPayments(
                        new PaymentRecordSearchCriteria(null, PaymentStatus.DUE));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).status()).isEqualTo(PaymentStatus.DUE);
        verify(paymentRecordRepository).findDuePayments();
    }

    @Test
    void rejectsMissingPaymentCustomerOrMarkPaidInputs() throws Exception {
        UUID missingPaymentId = UUID.fromString("42000000-0000-0000-0000-000000000099");
        when(paymentRecordRepository.findById(missingPaymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentRecordService.markPaid(null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment record validation failed");

        assertThatThrownBy(
                        () ->
                                paymentRecordService.markPaid(
                                        PAYMENT_ID, new MarkPaymentPaidCommand(null, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment record validation failed");

        assertThatThrownBy(() -> paymentRecordService.listCustomerPayments(null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Payment record validation failed");

        assertThatThrownBy(() -> paymentRecordService.markOverdue(missingPaymentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment record was not found: " + missingPaymentId);
    }

    private static void assertPreAuthorize(String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = PaymentRecordService.class.getMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
    }

    private static void assertPreAuthorizeWithExpression(
            String methodName, Class<?>[] parameterTypes, String expectedExpression)
            throws Exception {
        Method method = PaymentRecordService.class.getMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class)).isTrue();
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expectedExpression);
    }

    private static Customer customer(UUID id, String firstName, String lastName) throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, firstName, lastName);
        setId(customer, id);
        return customer;
    }

    private static Product activeProduct() throws Exception {
        Product product =
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("129.99"),
                        24);
        setId(product, PRODUCT_ID);
        return product;
    }

    private static ProductOwnership ownership(Customer customer) throws Exception {
        Product product = activeProduct();
        LocalDate startDate = LocalDate.parse("2026-01-15");
        ProductOwnership ownership =
                ProductOwnership.create(customer, product, startDate, startDate.plusMonths(12));
        setOwnershipId(ownership, OWNERSHIP_ID);
        return ownership;
    }

    private PaymentRecord duePayment() throws Exception {
        Customer customer = customer(CUSTOMER_ID, "Ada", "Payer");
        ProductOwnership ownership = ownership(customer);
        PaymentRecord payment =
                PaymentRecord.create(customer, ownership, DUE_DATE, new BigDecimal("129.99"));
        setPaymentId(payment, PAYMENT_ID);
        return payment;
    }

    private static void setId(Customer customer, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(customer, id);
    }

    private static void setId(Product product, UUID id) throws Exception {
        Field idField = BaseEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(product, id);
    }

    private static void setOwnershipId(ProductOwnership ownership, UUID id) throws Exception {
        Field idField = ProductOwnership.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(ownership, id);
    }

    private static void setPaymentId(PaymentRecord payment, UUID id) throws Exception {
        Field idField = PaymentRecord.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(payment, id);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castAuditPayload(Map<String, ?> payload) {
        return (Map<String, Object>) payload;
    }
}
