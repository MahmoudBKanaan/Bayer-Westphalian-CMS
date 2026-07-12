package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.PaymentRecord;
import com.bayerwestphalian.campaign.product.PaymentRecordRepository;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductOwnershipRepository;
import com.bayerwestphalian.campaign.product.ProductRepository;
import com.bayerwestphalian.campaign.product.ProductType;
import java.math.BigDecimal;
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
 * KB item 394 / BR-020: Green reminder is the first reminder.
 *
 * <p>When a payment has {@code reminder_count == 0} (no prior payment reminders), generation and
 * level resolution must produce {@link ReminderLevel#GREEN}.
 */
class GreenReminderIsFirstReminderTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000394");
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000394");
    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 7, 11);

    private ReminderRepository reminderRepository;
    private PaymentRecordRepository paymentRecordRepository;
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
        ProductOwnershipRepository productOwnershipRepository =
                Mockito.mock(ProductOwnershipRepository.class);
        eligibilityService = Mockito.mock(EligibilityService.class);

        reminderService =
                new ReminderService(
                        reminderRepository,
                        customerRepository,
                        productRepository,
                        paymentRecordRepository,
                        productOwnershipRepository,
                        eligibilityService);

        customer = Customer.create(CustomerType.CUSTOMER, "Ada", "First");
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
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(EligibilityDecision.included());
    }

    @Test
    void greenIsFirstReminderLevelInKbEscalationOrder() {
        assertThat(ReminderLevel.values())
                .containsExactly(ReminderLevel.GREEN, ReminderLevel.YELLOW, ReminderLevel.RED);
        assertThat(ReminderLevel.values()[0]).isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.isFirstReminder(ReminderLevel.GREEN)).isTrue();
        assertThat(PaymentReminderLevelRules.isFirstReminder(ReminderLevel.YELLOW)).isFalse();
        assertThat(PaymentReminderLevelRules.isFirstReminder(ReminderLevel.RED)).isFalse();
    }

    @Test
    void firstReminderCountConstantIsZeroPerKb() {
        assertThat(PaymentReminderLevelRules.FIRST_REMINDER_COUNT).isZero();
    }

    @Test
    void resolveFromReminderCountMapsZeroToGreenAsFirstReminder() {
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(0))
                .isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(
                        PaymentReminderLevelRules.FIRST_REMINDER_COUNT))
                .isEqualTo(ReminderLevel.GREEN);
    }

    @Test
    void resolveFromReminderCountDoesNotMapSecondOrThirdToGreen() {
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(1))
                .isNotEqualTo(ReminderLevel.GREEN)
                .isEqualTo(ReminderLevel.YELLOW);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(2))
                .isNotEqualTo(ReminderLevel.GREEN)
                .isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(3))
                .isEqualTo(ReminderLevel.RED);
    }

    @Test
    void resolveMapsNewPaymentWithZeroReminderCountToGreen() {
        PaymentRecord firstPayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));

        assertThat(firstPayment.getReminderCount()).isEqualTo(0);
        assertThat(PaymentReminderLevelRules.resolve(firstPayment)).isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.isFirstReminder(
                        PaymentReminderLevelRules.resolve(firstPayment)))
                .isTrue();
    }

    @Test
    void resolveRejectsNullPayment() {
        assertThatThrownBy(() -> PaymentReminderLevelRules.resolve(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("payment is required");
    }

    @Test
    void greenReminderIsFirstReminderWhenPaymentDueReminderIsGenerated() {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        assertThat(duePayment.getReminderCount())
                .isEqualTo(PaymentReminderLevelRules.FIRST_REMINDER_COUNT);

        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());

        List<ReminderScheduleView> views =
                reminderService.generatePaymentDueReminders(AS_OF_DATE);

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());
        ReminderSchedule saved = captor.getValue();

        assertThat(saved.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(saved.getReminderLevel()).isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.isFirstReminder(saved.getReminderLevel())).isTrue();
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.GREEN);
        assertThat(views.get(0).reminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
    }

    @Test
    void firstManualPaymentReminderCanBeScheduledAsGreen() {
        ReminderScheduleCommand firstReminder =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        AS_OF_DATE);

        ReminderScheduleView view = reminderService.createPaymentReminders(firstReminder);

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());

        assertThat(captor.getValue().getReminderLevel()).isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.isFirstReminder(captor.getValue().getReminderLevel()))
                .isTrue();
        assertThat(view.reminderLevel()).isEqualTo(ReminderLevel.GREEN);
        assertThat(view.reminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(view.status()).isEqualTo(ReminderStatus.PENDING);
    }
}
