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
import com.bayerwestphalian.campaign.product.PaymentStatus;
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
 * KB item 396 / BR-022: Red reminder is the third reminder.
 *
 * <p>When a payment has {@code reminder_count >= 2} (two prior payment reminders), generation and
 * level resolution must produce {@link ReminderLevel#RED}. Red also indicates likely default risk.
 */
class RedReminderIsThirdReminderTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000396");
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000396");
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

        customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Third");
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
    void redIsThirdReminderLevelInKbEscalationOrder() {
        assertThat(ReminderLevel.values())
                .containsExactly(ReminderLevel.GREEN, ReminderLevel.YELLOW, ReminderLevel.RED);
        assertThat(ReminderLevel.values()[2]).isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.isThirdReminder(ReminderLevel.RED)).isTrue();
        assertThat(PaymentReminderLevelRules.isThirdReminder(ReminderLevel.GREEN)).isFalse();
        assertThat(PaymentReminderLevelRules.isThirdReminder(ReminderLevel.YELLOW)).isFalse();
        assertThat(PaymentReminderLevelRules.isFirstReminder(ReminderLevel.RED)).isFalse();
        assertThat(PaymentReminderLevelRules.isSecondReminder(ReminderLevel.RED)).isFalse();
    }

    @Test
    void thirdReminderCountConstantIsTwoPerKb() {
        assertThat(PaymentReminderLevelRules.THIRD_REMINDER_COUNT).isEqualTo(2);
    }

    @Test
    void resolveFromReminderCountMapsTwoOrMoreToRedAsThirdReminder() {
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(2))
                .isEqualTo(ReminderLevel.RED);
        assertThat(
                        PaymentReminderLevelRules.resolveFromReminderCount(
                                PaymentReminderLevelRules.THIRD_REMINDER_COUNT))
                .isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(3))
                .isEqualTo(ReminderLevel.RED);
        assertThat(
                        PaymentReminderLevelRules.isThirdReminder(
                                PaymentReminderLevelRules.resolveFromReminderCount(2)))
                .isTrue();
    }

    @Test
    void resolveFromReminderCountDoesNotMapFirstOrSecondToRed() {
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(0))
                .isNotEqualTo(ReminderLevel.RED)
                .isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(1))
                .isNotEqualTo(ReminderLevel.RED)
                .isEqualTo(ReminderLevel.YELLOW);
    }

    @Test
    void resolveMapsPaymentWithTwoPriorRemindersToRed() {
        PaymentRecord thirdPayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        thirdPayment.incrementReminder();
        thirdPayment.incrementReminder();

        assertThat(thirdPayment.getReminderCount())
                .isEqualTo(PaymentReminderLevelRules.THIRD_REMINDER_COUNT);
        assertThat(PaymentReminderLevelRules.resolve(thirdPayment)).isEqualTo(ReminderLevel.RED);
        assertThat(
                        PaymentReminderLevelRules.isThirdReminder(
                                PaymentReminderLevelRules.resolve(thirdPayment)))
                .isTrue();
    }

    @Test
    void resolveMapsDefaultRiskPaymentToRedRegardlessOfLowerCount() {
        PaymentRecord defaultRiskPayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        // Three increments marks DEFAULT_RISK (likely default risk after Red path).
        defaultRiskPayment.incrementReminder();
        defaultRiskPayment.incrementReminder();
        defaultRiskPayment.incrementReminder();

        assertThat(defaultRiskPayment.getStatus()).isEqualTo(PaymentStatus.DEFAULT_RISK);
        assertThat(defaultRiskPayment.isDefaultRisk()).isTrue();
        assertThat(PaymentReminderLevelRules.resolve(defaultRiskPayment))
                .isEqualTo(ReminderLevel.RED);
        assertThat(
                        PaymentReminderLevelRules.isThirdReminder(
                                PaymentReminderLevelRules.resolve(defaultRiskPayment)))
                .isTrue();
    }

    @Test
    void redReminderIsThirdReminderWhenPaymentDueReminderIsGenerated() {
        PaymentRecord overduePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        overduePayment.incrementReminder();
        overduePayment.incrementReminder();
        assertThat(overduePayment.getReminderCount())
                .isEqualTo(PaymentReminderLevelRules.THIRD_REMINDER_COUNT);

        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of());
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of(overduePayment));

        List<ReminderScheduleView> views = reminderService.generatePaymentDueReminders(AS_OF_DATE);

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());
        ReminderSchedule saved = captor.getValue();

        assertThat(saved.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(saved.getReminderLevel()).isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.isThirdReminder(saved.getReminderLevel())).isTrue();
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.RED);
        assertThat(views.get(0).reminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
    }

    @Test
    void thirdManualPaymentReminderCanBeScheduledAsRed() {
        ReminderScheduleCommand thirdReminder =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.RED,
                        AS_OF_DATE);

        ReminderScheduleView view = reminderService.createPaymentReminders(thirdReminder);

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());

        assertThat(captor.getValue().getReminderLevel()).isEqualTo(ReminderLevel.RED);
        assertThat(
                        PaymentReminderLevelRules.isThirdReminder(
                                captor.getValue().getReminderLevel()))
                .isTrue();
        assertThat(view.reminderLevel()).isEqualTo(ReminderLevel.RED);
        assertThat(view.reminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(view.status()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    void escalationFromGreenThroughYellowToRedFollowsKbOrder() {
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(0))
                .isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(1))
                .isEqualTo(ReminderLevel.YELLOW);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(2))
                .isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.isFirstReminder(ReminderLevel.GREEN)).isTrue();
        assertThat(PaymentReminderLevelRules.isSecondReminder(ReminderLevel.YELLOW)).isTrue();
        assertThat(PaymentReminderLevelRules.isThirdReminder(ReminderLevel.RED)).isTrue();
    }
}
