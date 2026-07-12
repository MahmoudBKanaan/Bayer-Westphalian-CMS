package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
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
 * KB item 395 / BR-021: Yellow reminder is the second reminder.
 *
 * <p>When a payment has {@code reminder_count == 1} (one prior payment reminder), generation and
 * level resolution must produce {@link ReminderLevel#YELLOW}.
 */
class YellowReminderIsSecondReminderTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000395");
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000395");
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

        customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Second");
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
    void yellowIsSecondReminderLevelInKbEscalationOrder() {
        assertThat(ReminderLevel.values())
                .containsExactly(ReminderLevel.GREEN, ReminderLevel.YELLOW, ReminderLevel.RED);
        assertThat(ReminderLevel.values()[1]).isEqualTo(ReminderLevel.YELLOW);
        assertThat(PaymentReminderLevelRules.isSecondReminder(ReminderLevel.YELLOW)).isTrue();
        assertThat(PaymentReminderLevelRules.isSecondReminder(ReminderLevel.GREEN)).isFalse();
        assertThat(PaymentReminderLevelRules.isSecondReminder(ReminderLevel.RED)).isFalse();
        assertThat(PaymentReminderLevelRules.isFirstReminder(ReminderLevel.YELLOW)).isFalse();
    }

    @Test
    void secondReminderCountConstantIsOnePerKb() {
        assertThat(PaymentReminderLevelRules.SECOND_REMINDER_COUNT).isEqualTo(1);
    }

    @Test
    void resolveFromReminderCountMapsOneToYellowAsSecondReminder() {
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(1))
                .isEqualTo(ReminderLevel.YELLOW);
        assertThat(
                        PaymentReminderLevelRules.resolveFromReminderCount(
                                PaymentReminderLevelRules.SECOND_REMINDER_COUNT))
                .isEqualTo(ReminderLevel.YELLOW);
        assertThat(
                        PaymentReminderLevelRules.isSecondReminder(
                                PaymentReminderLevelRules.resolveFromReminderCount(1)))
                .isTrue();
    }

    @Test
    void resolveFromReminderCountDoesNotMapFirstOrThirdToYellow() {
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(0))
                .isNotEqualTo(ReminderLevel.YELLOW)
                .isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(2))
                .isNotEqualTo(ReminderLevel.YELLOW)
                .isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(3))
                .isEqualTo(ReminderLevel.RED);
    }

    @Test
    void resolveMapsPaymentWithOnePriorReminderToYellow() {
        PaymentRecord secondPayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        secondPayment.incrementReminder();

        assertThat(secondPayment.getReminderCount())
                .isEqualTo(PaymentReminderLevelRules.SECOND_REMINDER_COUNT);
        assertThat(PaymentReminderLevelRules.resolve(secondPayment)).isEqualTo(ReminderLevel.YELLOW);
        assertThat(
                        PaymentReminderLevelRules.isSecondReminder(
                                PaymentReminderLevelRules.resolve(secondPayment)))
                .isTrue();
    }

    @Test
    void yellowReminderIsSecondReminderWhenPaymentDueReminderIsGenerated() {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        duePayment.incrementReminder();
        assertThat(duePayment.getReminderCount())
                .isEqualTo(PaymentReminderLevelRules.SECOND_REMINDER_COUNT);

        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());

        List<ReminderScheduleView> views = reminderService.generatePaymentDueReminders(AS_OF_DATE);

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());
        ReminderSchedule saved = captor.getValue();

        assertThat(saved.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(saved.getReminderLevel()).isEqualTo(ReminderLevel.YELLOW);
        assertThat(PaymentReminderLevelRules.isSecondReminder(saved.getReminderLevel())).isTrue();
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.YELLOW);
        assertThat(views.get(0).reminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
    }

    @Test
    void secondManualPaymentReminderCanBeScheduledAsYellow() {
        ReminderScheduleCommand secondReminder =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.YELLOW,
                        AS_OF_DATE);

        ReminderScheduleView view = reminderService.createPaymentReminders(secondReminder);

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());

        assertThat(captor.getValue().getReminderLevel()).isEqualTo(ReminderLevel.YELLOW);
        assertThat(
                        PaymentReminderLevelRules.isSecondReminder(
                                captor.getValue().getReminderLevel()))
                .isTrue();
        assertThat(view.reminderLevel()).isEqualTo(ReminderLevel.YELLOW);
        assertThat(view.reminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(view.status()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    void escalationFromFirstGreenToSecondYellowFollowsKbOrder() {
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(0))
                .isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(1))
                .isEqualTo(ReminderLevel.YELLOW);
        assertThat(PaymentReminderLevelRules.isFirstReminder(ReminderLevel.GREEN)).isTrue();
        assertThat(PaymentReminderLevelRules.isSecondReminder(ReminderLevel.YELLOW)).isTrue();
    }
}
