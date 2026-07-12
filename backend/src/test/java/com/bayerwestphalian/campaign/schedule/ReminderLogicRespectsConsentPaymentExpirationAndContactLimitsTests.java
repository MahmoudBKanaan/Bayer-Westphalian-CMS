package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityExclusionReason;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.OwnershipStatus;
import com.bayerwestphalian.campaign.product.PaymentRecord;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.PaymentStatus;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.product.ProductType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Sprint 11 production gate (KB item 409): reminder logic must respect consent, payment status,
 * expiration dates, and contact frequency limits.
 *
 * <p>Combines BR-024 (payment status), BR-023 (expiration windows/dates), and item 401 (consent +
 * monthly contact limit / FR-092) across create, generate, and send paths.
 */
class ReminderLogicRespectsConsentPaymentExpirationAndContactLimitsTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000409");
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000409");
    private static final UUID REMINDER_ID = UUID.fromString("90000000-0000-0000-0000-000000000409");
    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 7, 11);

    private ReminderRepository reminderRepository;
    private PaymentRecordRepository paymentRecordRepository;
    private ProductOwnershipRepository productOwnershipRepository;
    private EligibilityService eligibilityService;
    private ReminderService reminderService;

    private Customer customer;
    private Product product;
    private ProductOwnership ownership;

    @BeforeEach
    void setUp() {
        reminderRepository = Mockito.mock(ReminderRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        paymentRecordRepository = Mockito.mock(PaymentRecordRepository.class);
        productOwnershipRepository = Mockito.mock(ProductOwnershipRepository.class);
        eligibilityService = Mockito.mock(EligibilityService.class);

        reminderService =
                new ReminderService(
                        reminderRepository,
                        customerRepository,
                        productRepository,
                        paymentRecordRepository,
                        productOwnershipRepository,
                        eligibilityService);

        customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Gate");
        product =
                Product.create(
                        "Life Protection", ProductType.LIFE_INSURANCE, BigDecimal.valueOf(100), 12);
        ownership =
                ProductOwnership.create(
                        customer, product, LocalDate.of(2025, 7, 1), LocalDate.of(2026, 10, 11));

        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        ReflectionTestUtils.setField(ownership, "id", UUID.randomUUID());

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productOwnershipRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(ownership));
        when(reminderRepository.save(any(ReminderSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reminderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(EligibilityDecision.included());
    }

    // --- Consent ---

    @Test
    void productionGate_consent_invalidConsentBlocksPaymentGenerateAndSend() {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.INVALID_CONSENT));

        assertThat(reminderService.generatePaymentDueReminders(AS_OF_DATE)).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));

        ReminderSchedule pending =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 1));
        when(reminderRepository.findDueReminders(AS_OF_DATE)).thenReturn(List.of(pending));

        List<ReminderScheduleView> sent = reminderService.sendDueReminders(AS_OF_DATE);
        assertThat(pending.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(pending.getSentAt()).isNull();
        assertThat(sent.get(0).status()).isEqualTo(ReminderStatus.CANCELLED);
    }

    @Test
    void productionGate_consent_createPaymentRejectsWhenIneligible() {
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(
                        EligibilityDecision.excluded(EligibilityExclusionReason.MARKETING_OPT_OUT));

        ReminderScheduleCommand command =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        AS_OF_DATE);

        assertThatThrownBy(() -> reminderService.createPaymentReminders(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", ReminderService.REMINDER_RECIPIENT_INELIGIBLE);
    }

    // --- Payment status (BR-024) ---

    @Test
    void productionGate_paymentStatus_paidPaymentsAreNotGenerated() {
        PaymentRecord paid =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        paid.markPaid(BigDecimal.valueOf(100), Instant.now());
        assertThat(paid.getStatus()).isEqualTo(PaymentStatus.PAID);

        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(paid));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());

        assertThat(reminderService.generatePaymentDueReminders(AS_OF_DATE)).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }

    @Test
    void productionGate_paymentStatus_createAndSendRejectOrCancelWhenPaid() {
        PaymentRecord paid =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        paid.markPaid(BigDecimal.valueOf(100), Instant.now());
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(paid));

        ReminderScheduleCommand command =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        AS_OF_DATE);
        assertThatThrownBy(() -> reminderService.createPaymentReminders(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "PAYMENT_REMINDER_PAYMENT_COMPLETED");

        ReminderSchedule pending =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 1));
        when(reminderRepository.findDueReminders(AS_OF_DATE)).thenReturn(List.of(pending));

        List<ReminderScheduleView> views = reminderService.sendDueReminders(AS_OF_DATE);
        assertThat(pending.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(views.get(0).sentAt()).isNull();
    }

    @Test
    void productionGate_paymentStatus_futureDueDateIsNotGenerated() {
        PaymentRecord future =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 8, 1), BigDecimal.valueOf(100));
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(future));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());

        assertThat(reminderService.generatePaymentDueReminders(AS_OF_DATE)).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }

    // --- Expiration dates (BR-023) ---

    @Test
    void productionGate_expirationDates_threeMonthWindowUsesAsOfThroughPlusThreeMonths() {
        LocalDate windowEnd = ProductExpirationReminderRules.threeMonthWindowEnd(AS_OF_DATE);
        ProductOwnership inWindow =
                ProductOwnership.create(customer, product, LocalDate.of(2025, 7, 1), windowEnd);
        ReflectionTestUtils.setField(inWindow, "id", UUID.randomUUID());
        when(productOwnershipRepository.findExpiringBetween(AS_OF_DATE, windowEnd))
                .thenReturn(List.of(inWindow));

        List<ReminderScheduleView> views =
                reminderService.generateThreeMonthExpirationReminders(AS_OF_DATE);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.RED);
        verify(productOwnershipRepository).findExpiringBetween(eq(AS_OF_DATE), eq(windowEnd));
    }

    @Test
    void productionGate_expirationDates_skipsAlreadyExpiredOwnershipBeforeAsOf() {
        ProductOwnership expired =
                ProductOwnership.create(
                        customer, product, LocalDate.of(2024, 1, 1), LocalDate.of(2026, 6, 1));
        ReflectionTestUtils.setField(expired, "id", UUID.randomUUID());
        // Repository may still return rows; candidate filter must drop past expirations.
        LocalDate windowEnd = ProductExpirationReminderRules.threeMonthWindowEnd(AS_OF_DATE);
        when(productOwnershipRepository.findExpiringBetween(AS_OF_DATE, windowEnd))
                .thenReturn(List.of(expired));

        assertThat(reminderService.generateThreeMonthExpirationReminders(AS_OF_DATE)).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }

    @Test
    void productionGate_expirationDates_skipsInactiveOwnershipAndNullExpiration() {
        LocalDate windowEnd = ProductExpirationReminderRules.sixMonthWindowEnd(AS_OF_DATE);
        ProductOwnership cancelled =
                ProductOwnership.create(customer, product, LocalDate.of(2025, 1, 1), windowEnd);
        ReflectionTestUtils.setField(cancelled, "status", OwnershipStatus.CANCELLED);
        ProductOwnership noExpiration =
                ProductOwnership.create(customer, product, LocalDate.of(2025, 1, 1), null);
        ReflectionTestUtils.setField(noExpiration, "id", UUID.randomUUID());
        when(productOwnershipRepository.findExpiringBetween(AS_OF_DATE, windowEnd))
                .thenReturn(List.of(cancelled, noExpiration));

        assertThat(reminderService.generateSixMonthExpirationReminders(AS_OF_DATE)).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }

    @Test
    void productionGate_expirationDates_createRequiresActiveOwnershipWithExpirationDate() {
        ProductOwnership withoutExpiration =
                ProductOwnership.create(customer, product, LocalDate.of(2025, 1, 1), null);
        when(productOwnershipRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(withoutExpiration));

        ReminderScheduleCommand command =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PRODUCT_EXPIRATION,
                        ReminderLevel.YELLOW,
                        AS_OF_DATE);

        assertThatThrownBy(() -> reminderService.createExpirationReminders(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue(
                        "code", ReminderService.EXPIRATION_REMINDER_REQUIRES_ACTIVE_OWNERSHIP)
                .hasMessageContaining("expiration date");
    }

    // --- Contact frequency limits ---

    @Test
    void productionGate_contactFrequency_monthlyLimitBlocksGenerateAndSend() {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(
                        EligibilityDecision.excluded(
                                EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT));

        assertThat(reminderService.generatePaymentDueReminders(AS_OF_DATE)).isEmpty();

        ReminderSchedule pending =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PRODUCT_EXPIRATION,
                        ReminderLevel.GREEN,
                        AS_OF_DATE);
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(pending));

        ReminderScheduleView view = reminderService.markSent(REMINDER_ID);
        assertThat(view.status()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(view.sentAt()).isNull();
        assertThat(pending.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
    }

    // --- Combined happy path under all gates ---

    @Test
    void productionGate_allowsGenerateWhenConsentPaymentExpirationAndLimitsAreSatisfied() {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());

        List<ReminderScheduleView> paymentViews =
                reminderService.generatePaymentDueReminders(AS_OF_DATE);
        assertThat(paymentViews).hasSize(1);
        assertThat(paymentViews.get(0).reminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(paymentViews.get(0).reminderLevel()).isEqualTo(ReminderLevel.GREEN);

        LocalDate windowEnd = ProductExpirationReminderRules.twelveMonthWindowEnd(AS_OF_DATE);
        ProductOwnership expiring =
                ProductOwnership.create(customer, product, LocalDate.of(2025, 7, 1), windowEnd);
        ReflectionTestUtils.setField(expiring, "id", UUID.randomUUID());
        when(productOwnershipRepository.findExpiringBetween(AS_OF_DATE, windowEnd))
                .thenReturn(List.of(expiring));

        List<ReminderScheduleView> expirationViews =
                reminderService.generateTwelveMonthExpirationReminders(AS_OF_DATE);
        assertThat(expirationViews).hasSize(1);
        assertThat(expirationViews.get(0).reminderLevel()).isEqualTo(ReminderLevel.GREEN);

        ReminderSchedule pending =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 1));
        when(reminderRepository.findDueReminders(AS_OF_DATE)).thenReturn(List.of(pending));
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(duePayment));

        List<ReminderScheduleView> sent = reminderService.sendDueReminders(AS_OF_DATE);
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).status()).isEqualTo(ReminderStatus.SENT);

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository, Mockito.atLeast(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(r -> r.getReminderType() == ReminderType.PAYMENT_DUE)
                .anyMatch(r -> r.getReminderType() == ReminderType.PRODUCT_EXPIRATION);
        verify(eligibilityService, Mockito.atLeast(3))
                .evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL);
    }
}
