package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
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
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 397 / BR-024: Payment reminder is not sent if payment is completed.
 *
 * <p>Completed (paid) payments must not receive payment-due reminders: generation skips them,
 * manual scheduling is rejected, and due-send / mark-sent paths cancel instead of marking SENT.
 *
 * <p>Sprint 16 critical restatement: item <b>660</b> — {@link
 * PaymentReminderIsNotSentIfPaymentIsCompletedTests}.
 */
class PaymentReminderNotSentIfPaymentCompletedTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000397");
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000397");
    private static final UUID REMINDER_ID = UUID.fromString("90000000-0000-0000-0000-000000000397");
    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 7, 11);

    private ReminderRepository reminderRepository;
    private PaymentRecordRepository paymentRecordRepository;
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
        ProductOwnershipRepository productOwnershipRepository =
                Mockito.mock(ProductOwnershipRepository.class);
        EligibilityService eligibilityService = Mockito.mock(EligibilityService.class);

        reminderService =
                new ReminderService(
                        reminderRepository,
                        customerRepository,
                        productRepository,
                        paymentRecordRepository,
                        productOwnershipRepository,
                        eligibilityService);

        customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Paid");
        product =
                Product.create(
                        "Life Protection", ProductType.LIFE_INSURANCE, BigDecimal.valueOf(100), 12);
        ownership =
                ProductOwnership.create(
                        customer, product, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1));

        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        ReflectionTestUtils.setField(ownership, "id", UUID.randomUUID());

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(reminderRepository.save(any(ReminderSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reminderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(EligibilityDecision.included());
    }

    @Test
    void paymentReminderIsNotSentWhenPaymentIsCompletedDuringDueSend() {
        PaymentRecord paidPayment = completedPayment();
        ReminderSchedule pendingReminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 1));

        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(paidPayment));
        when(reminderRepository.findDueReminders(AS_OF_DATE)).thenReturn(List.of(pendingReminder));

        List<ReminderScheduleView> views = reminderService.sendDueReminders(AS_OF_DATE);

        assertThat(pendingReminder.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(pendingReminder.getSentAt()).isNull();
        assertThat(views).hasSize(1);
        assertThat(views.get(0).status()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(views.get(0).sentAt()).isNull();
        assertThat(views.get(0).reminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        verify(reminderRepository).save(pendingReminder);
    }

    @Test
    void paymentReminderIsNotSentWhenMarkSentIsCalledAfterPaymentCompleted() {
        PaymentRecord paidPayment = completedPayment();
        ReminderSchedule pendingReminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.YELLOW,
                        LocalDate.of(2026, 7, 1));
        ReflectionTestUtils.setField(pendingReminder, "id", REMINDER_ID);

        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(paidPayment));
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(pendingReminder));

        ReminderScheduleView view = reminderService.markSent(REMINDER_ID);

        assertThat(pendingReminder.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(pendingReminder.getSentAt()).isNull();
        assertThat(view.status()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(view.sentAt()).isNull();
        verify(reminderRepository).save(pendingReminder);
    }

    @Test
    void unpaidPaymentReminderIsStillSent() {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        assertThat(duePayment.getStatus()).isEqualTo(PaymentStatus.DUE);

        ReminderSchedule pendingReminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 1));

        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(duePayment));
        when(reminderRepository.findDueReminders(AS_OF_DATE)).thenReturn(List.of(pendingReminder));

        List<ReminderScheduleView> views = reminderService.sendDueReminders(AS_OF_DATE);

        assertThat(pendingReminder.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(pendingReminder.getSentAt()).isNotNull();
        assertThat(views).hasSize(1);
        assertThat(views.get(0).status()).isEqualTo(ReminderStatus.SENT);
        verify(reminderRepository).save(pendingReminder);
    }

    @Test
    void generateDoesNotCreatePaymentReminderForCompletedPayment() {
        PaymentRecord paidPayment = completedPayment();
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(paidPayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());

        List<ReminderScheduleView> views = reminderService.generatePaymentDueReminders(AS_OF_DATE);

        assertThat(views).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }

    @Test
    void createPaymentReminderRejectsCompletedPayment() {
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(completedPayment()));

        ReminderScheduleCommand command =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        AS_OF_DATE);

        assertThatThrownBy(() -> reminderService.createPaymentReminders(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "PAYMENT_REMINDER_PAYMENT_COMPLETED")
                .hasMessageContaining("completed");
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }

    @Test
    void productExpirationReminderIsStillSentWhenPaymentIsCompleted() {
        // BR-024 applies to payment reminders only.
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(completedPayment()));
        ReminderSchedule expirationReminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PRODUCT_EXPIRATION,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 1));
        when(reminderRepository.findDueReminders(AS_OF_DATE))
                .thenReturn(List.of(expirationReminder));

        List<ReminderScheduleView> views = reminderService.sendDueReminders(AS_OF_DATE);

        assertThat(expirationReminder.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(expirationReminder.getSentAt()).isNotNull();
        assertThat(views.get(0).status()).isEqualTo(ReminderStatus.SENT);
    }

    private PaymentRecord completedPayment() {
        PaymentRecord paidPayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        paidPayment.markPaid(BigDecimal.valueOf(100), Instant.parse("2026-07-10T09:00:00Z"));
        assertThat(paidPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        return paidPayment;
    }
}
