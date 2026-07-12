package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.common.exception.BusinessRuleException;
import com.bayerwestphalian.campaign.common.exception.ValidationException;
import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityExclusionReason;
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
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

class ReminderServiceTests {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID REMINDER_ID = UUID.randomUUID();

    private ReminderRepository reminderRepository;
    private CustomerRepository customerRepository;
    private ProductRepository productRepository;
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
        customerRepository = Mockito.mock(CustomerRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);
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

        customer = Customer.create(CustomerType.PROSPECT, "Ada", "Lovelace");
        product = Product.create("Car Insurance", ProductType.AUTO_INSURANCE, BigDecimal.TEN, 12);
        ownership = ProductOwnership.create(customer, product, LocalDate.now().minusMonths(1), null);

        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        ReflectionTestUtils.setField(ownership, "id", UUID.randomUUID());

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(reminderRepository.save(any(ReminderSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reminderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());
        when(productOwnershipRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(ownership));
        when(eligibilityService.evaluateForReminder(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(EligibilityDecision.included());
    }

    @Test
    void createPaymentRemindersSavesPendingPaymentReminder() {
        ReminderScheduleCommand command =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 8, 1));

        ReminderScheduleView view = reminderService.createPaymentReminders(command);

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());
        ReminderSchedule saved = captor.getValue();

        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getProduct()).isSameAs(product);
        assertThat(saved.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(saved.getReminderLevel()).isEqualTo(ReminderLevel.GREEN);
        assertThat(saved.getScheduledDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(view.status()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    void createPaymentRemindersRejectsRecipientBlockedByEligibilityRules() {
        // KB item 401: consent and contact-limit failures reject manual scheduling.
        when(eligibilityService.evaluateForReminder(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(
                        EligibilityDecision.excluded(
                                EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT));
        ReminderScheduleCommand command =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> reminderService.createPaymentReminders(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue(
                        "code", ReminderService.REMINDER_RECIPIENT_INELIGIBLE)
                .hasMessageContaining("monthly marketing contact limit");
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }

    @Test
    void generateThreeMonthExpirationRemindersCreatesRedProductExpirationReminder() {
        // KB BR-023 / item 398: product-expiration reminder is generated 3 months before expiration.
        LocalDate asOf = LocalDate.of(2026, 7, 11);
        LocalDate windowEnd = ProductExpirationReminderRules.threeMonthWindowEnd(asOf);
        assertThat(windowEnd).isEqualTo(LocalDate.of(2026, 10, 11));
        assertThat(ProductExpirationReminderRules.THREE_MONTH_WINDOW).isEqualTo(3);

        ProductOwnership expiringOwnership =
                ProductOwnership.create(customer, product, LocalDate.of(2025, 7, 1), windowEnd);
        ReflectionTestUtils.setField(expiringOwnership, "id", UUID.randomUUID());
        when(productOwnershipRepository.findExpiringBetween(asOf, windowEnd))
                .thenReturn(List.of(expiringOwnership));

        List<ReminderScheduleView> views =
                reminderService.generateThreeMonthExpirationReminders(asOf);

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());
        ReminderSchedule saved = captor.getValue();

        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getProduct()).isSameAs(product);
        assertThat(saved.getReminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(saved.getReminderLevel())
                .isEqualTo(ProductExpirationReminderRules.threeMonthReminderLevel())
                .isEqualTo(ReminderLevel.RED);
        assertThat(saved.getScheduledDate()).isEqualTo(asOf);
        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.RED);
        verify(productOwnershipRepository).findExpiringBetween(asOf, windowEnd);
    }

    @Test
    void generateThreeMonthExpirationRemindersSkipsExistingRedExpirationReminder() {
        ProductOwnership expiringOwnership =
                ProductOwnership.create(
                        customer,
                        product,
                        LocalDate.of(2025, 7, 1),
                        LocalDate.of(2026, 10, 11));
        ReminderSchedule existing =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PRODUCT_EXPIRATION,
                        ReminderLevel.RED,
                        LocalDate.of(2026, 7, 11));

        when(productOwnershipRepository.findExpiringBetween(
                        LocalDate.of(2026, 7, 11), LocalDate.of(2026, 10, 11)))
                .thenReturn(List.of(expiringOwnership));
        when(reminderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(existing));

        List<ReminderScheduleView> views =
                reminderService.generateThreeMonthExpirationReminders(LocalDate.of(2026, 7, 11));

        assertThat(views).isEmpty();
    }

    @Test
    void generateExpirationRemindersSkipCustomersBlockedByEligibilityRules() {
        ProductOwnership expiringOwnership =
                ProductOwnership.create(
                        customer,
                        product,
                        LocalDate.of(2025, 7, 1),
                        LocalDate.of(2026, 10, 11));
        ReflectionTestUtils.setField(expiringOwnership, "id", UUID.randomUUID());
        when(productOwnershipRepository.findExpiringBetween(
                        LocalDate.of(2026, 7, 11), LocalDate.of(2026, 10, 11)))
                .thenReturn(List.of(expiringOwnership));
        when(eligibilityService.evaluateForReminder(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(
                        EligibilityDecision.excluded(
                                EligibilityExclusionReason.INVALID_CONSENT));

        List<ReminderScheduleView> views =
                reminderService.generateThreeMonthExpirationReminders(LocalDate.of(2026, 7, 11));

        assertThat(views).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }

    @Test
    void generateSixMonthExpirationRemindersCreatesYellowProductExpirationReminder() {
        // KB BR-023 / item 399: product-expiration reminder is generated 6 months before expiration.
        LocalDate asOf = LocalDate.of(2026, 7, 11);
        LocalDate windowEnd = ProductExpirationReminderRules.sixMonthWindowEnd(asOf);
        assertThat(windowEnd).isEqualTo(LocalDate.of(2027, 1, 11));
        assertThat(ProductExpirationReminderRules.SIX_MONTH_WINDOW).isEqualTo(6);

        ProductOwnership expiringOwnership =
                ProductOwnership.create(customer, product, LocalDate.of(2025, 7, 1), windowEnd);
        ReflectionTestUtils.setField(expiringOwnership, "id", UUID.randomUUID());
        when(productOwnershipRepository.findExpiringBetween(asOf, windowEnd))
                .thenReturn(List.of(expiringOwnership));

        List<ReminderScheduleView> views =
                reminderService.generateSixMonthExpirationReminders(asOf);

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());
        ReminderSchedule saved = captor.getValue();

        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getProduct()).isSameAs(product);
        assertThat(saved.getReminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(saved.getReminderLevel())
                .isEqualTo(ProductExpirationReminderRules.sixMonthReminderLevel())
                .isEqualTo(ReminderLevel.YELLOW);
        assertThat(saved.getScheduledDate()).isEqualTo(asOf);
        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.YELLOW);
        verify(productOwnershipRepository).findExpiringBetween(asOf, windowEnd);
    }

    @Test
    void generateSixMonthExpirationRemindersSkipsExistingYellowExpirationReminder() {
        ProductOwnership expiringOwnership =
                ProductOwnership.create(
                        customer,
                        product,
                        LocalDate.of(2025, 7, 1),
                        LocalDate.of(2027, 1, 11));
        ReminderSchedule existing =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PRODUCT_EXPIRATION,
                        ReminderLevel.YELLOW,
                        LocalDate.of(2026, 7, 11));

        when(productOwnershipRepository.findExpiringBetween(
                        LocalDate.of(2026, 7, 11), LocalDate.of(2027, 1, 11)))
                .thenReturn(List.of(expiringOwnership));
        when(reminderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(existing));

        List<ReminderScheduleView> views =
                reminderService.generateSixMonthExpirationReminders(LocalDate.of(2026, 7, 11));

        assertThat(views).isEmpty();
    }

    @Test
    void generateTwelveMonthExpirationRemindersCreatesGreenProductExpirationReminder() {
        LocalDate asOf = LocalDate.of(2026, 7, 11);
        LocalDate windowEnd = ProductExpirationReminderRules.twelveMonthWindowEnd(asOf);
        ProductOwnership expiringOwnership =
                ProductOwnership.create(
                        customer, product, LocalDate.of(2025, 7, 1), windowEnd);
        ReflectionTestUtils.setField(expiringOwnership, "id", UUID.randomUUID());
        when(productOwnershipRepository.findExpiringBetween(asOf, windowEnd))
                .thenReturn(List.of(expiringOwnership));

        List<ReminderScheduleView> views =
                reminderService.generateTwelveMonthExpirationReminders(asOf);

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());
        ReminderSchedule saved = captor.getValue();

        assertThat(ProductExpirationReminderRules.TWELVE_MONTH_WINDOW).isEqualTo(12);
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getProduct()).isSameAs(product);
        assertThat(saved.getReminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(saved.getReminderLevel())
                .isEqualTo(ProductExpirationReminderRules.twelveMonthReminderLevel())
                .isEqualTo(ReminderLevel.GREEN);
        assertThat(saved.getScheduledDate()).isEqualTo(asOf);
        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.GREEN);
        verify(productOwnershipRepository).findExpiringBetween(asOf, windowEnd);
    }

    @Test
    void generateTwelveMonthExpirationRemindersSkipsExistingGreenExpirationReminder() {
        LocalDate asOf = LocalDate.of(2026, 7, 11);
        LocalDate windowEnd = ProductExpirationReminderRules.twelveMonthWindowEnd(asOf);
        ProductOwnership expiringOwnership =
                ProductOwnership.create(customer, product, LocalDate.of(2025, 7, 1), windowEnd);
        ReminderSchedule existing =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PRODUCT_EXPIRATION,
                        ReminderLevel.GREEN,
                        asOf);

        when(productOwnershipRepository.findExpiringBetween(asOf, windowEnd))
                .thenReturn(List.of(expiringOwnership));
        when(reminderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(existing));

        List<ReminderScheduleView> views =
                reminderService.generateTwelveMonthExpirationReminders(asOf);

        assertThat(views).isEmpty();
    }

    @Test
    void paymentDueReminderIsGenerated() {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());

        List<ReminderScheduleView> views =
                reminderService.generatePaymentDueReminders(LocalDate.of(2026, 7, 11));

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());
        ReminderSchedule saved = captor.getValue();

        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getProduct()).isSameAs(product);
        assertThat(saved.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(saved.getReminderLevel()).isEqualTo(ReminderLevel.GREEN);
        assertThat(saved.getScheduledDate()).isEqualTo(LocalDate.of(2026, 7, 11));
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.GREEN);
        assertThat(views.get(0).status()).isEqualTo(ReminderStatus.PENDING);
        assertThat(views.get(0).customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(views.get(0).productId()).isEqualTo(PRODUCT_ID);
        assertThat(views.get(0).scheduledDate()).isEqualTo(LocalDate.of(2026, 7, 11));
    }

    @Test
    void generatePaymentDueRemindersCreatesGreenFirstReminderWhenReminderCountIsZero() {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        assertThat(duePayment.getReminderCount())
                .isEqualTo(PaymentReminderLevelRules.FIRST_REMINDER_COUNT);
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());

        List<ReminderScheduleView> views =
                reminderService.generatePaymentDueReminders(LocalDate.of(2026, 7, 11));

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());
        ReminderSchedule saved = captor.getValue();

        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getProduct()).isSameAs(product);
        assertThat(saved.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        // KB BR-020: Green is the first reminder.
        assertThat(saved.getReminderLevel()).isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.isFirstReminder(saved.getReminderLevel())).isTrue();
        assertThat(saved.getScheduledDate()).isEqualTo(LocalDate.of(2026, 7, 11));
        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.GREEN);
    }

    @Test
    void generatePaymentDueRemindersSkipsDoNotContactCustomers() {
        customer.markDoNotContact();
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());

        List<ReminderScheduleView> views =
                reminderService.generatePaymentDueReminders(LocalDate.of(2026, 7, 11));

        assertThat(views).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }

    @Test
    void generatePaymentDueRemindersUsesTodayWhenAsOfDateIsNull() {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.now().minusDays(1), BigDecimal.valueOf(100));
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());

        List<ReminderScheduleView> views = reminderService.generatePaymentDueReminders(null);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(views.get(0).scheduledDate()).isEqualTo(LocalDate.now());
        verify(reminderRepository).save(any(ReminderSchedule.class));
    }

    @Test
    void generatePaymentDueRemindersCreatesYellowSecondReminderWhenReminderCountIsOne() {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        duePayment.incrementReminder();
        assertThat(duePayment.getReminderCount())
                .isEqualTo(PaymentReminderLevelRules.SECOND_REMINDER_COUNT);
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());

        List<ReminderScheduleView> views =
                reminderService.generatePaymentDueReminders(LocalDate.of(2026, 7, 11));

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());
        ReminderSchedule saved = captor.getValue();

        // KB BR-021: Yellow is the second reminder.
        assertThat(saved.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(saved.getReminderLevel()).isEqualTo(ReminderLevel.YELLOW);
        assertThat(PaymentReminderLevelRules.isSecondReminder(saved.getReminderLevel())).isTrue();
        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.YELLOW);
    }

    @Test
    void generatePaymentDueRemindersCreatesRedThirdReminderWhenReminderCountIsTwo() {
        PaymentRecord overduePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        overduePayment.incrementReminder();
        overduePayment.incrementReminder();
        assertThat(overduePayment.getReminderCount())
                .isEqualTo(PaymentReminderLevelRules.THIRD_REMINDER_COUNT);
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of());
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of(overduePayment));

        List<ReminderScheduleView> views =
                reminderService.generatePaymentDueReminders(LocalDate.of(2026, 7, 11));

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());
        ReminderSchedule saved = captor.getValue();

        // KB BR-022: Red is the third reminder.
        assertThat(saved.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(saved.getReminderLevel()).isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.isThirdReminder(saved.getReminderLevel())).isTrue();
        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.RED);
    }

    @Test
    void generatePaymentDueRemindersMarksLikelyDefaultRiskAfterRedReminder() {
        PaymentRecord overduePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        overduePayment.incrementReminder();
        overduePayment.incrementReminder();
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of());
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of(overduePayment));
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(overduePayment));

        List<ReminderScheduleView> views =
                reminderService.generatePaymentDueReminders(LocalDate.of(2026, 7, 11));

        assertThat(views).hasSize(1);
        // KB BR-022: Red is the third reminder and indicates likely default risk.
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.isThirdReminder(views.get(0).reminderLevel())).isTrue();
        assertThat(overduePayment.getStatus()).isEqualTo(PaymentStatus.DEFAULT_RISK);
        assertThat(overduePayment.isDefaultRisk()).isTrue();
        verify(paymentRecordRepository).save(overduePayment);
    }

    @Test
    void generatePaymentDueRemindersCreatesRedReminderForDefaultRiskPayment() {
        PaymentRecord overduePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        overduePayment.incrementReminder();
        overduePayment.incrementReminder();
        overduePayment.incrementReminder();
        assertThat(overduePayment.getStatus()).isEqualTo(PaymentStatus.DEFAULT_RISK);
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of());
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of(overduePayment));

        List<ReminderScheduleView> views =
                reminderService.generatePaymentDueReminders(LocalDate.of(2026, 7, 11));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.isThirdReminder(views.get(0).reminderLevel())).isTrue();
    }

    @Test
    void generatePaymentDueRemindersSkipsPaidAndFuturePayments() {
        // KB BR-024: completed payments never generate payment-due reminders.
        PaymentRecord paidPayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        paidPayment.markPaid(BigDecimal.valueOf(100), java.time.Instant.now());
        assertThat(paidPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        PaymentRecord futurePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 8, 1), BigDecimal.valueOf(100));
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(paidPayment, futurePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());

        List<ReminderScheduleView> views =
                reminderService.generatePaymentDueReminders(LocalDate.of(2026, 7, 11));

        assertThat(views).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }

    @Test
    void generatePaymentDueRemindersSkipsCustomersBlockedByEligibilityRules() {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());
        when(eligibilityService.evaluateForReminder(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(
                        EligibilityDecision.excluded(
                                EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT));

        List<ReminderScheduleView> views =
                reminderService.generatePaymentDueReminders(LocalDate.of(2026, 7, 11));

        assertThat(views).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }

    @Test
    void generatePaymentDueRemindersSkipsExistingReminderForSamePaymentLevelAndDate() {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        ReminderSchedule existing =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 11));
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());
        when(reminderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(existing));

        List<ReminderScheduleView> views =
                reminderService.generatePaymentDueReminders(LocalDate.of(2026, 7, 11));

        assertThat(views).isEmpty();
    }

    @Test
    void createPaymentRemindersRejectsCompletedPayment() {
        // KB BR-024: completed payments cannot be scheduled for payment reminders.
        PaymentRecord paidPayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        paidPayment.markPaid(BigDecimal.valueOf(100), java.time.Instant.now());
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(paidPayment));

        ReminderScheduleCommand command =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.YELLOW,
                        LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> reminderService.createPaymentReminders(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("completed")
                .hasFieldOrPropertyWithValue("code", "PAYMENT_REMINDER_PAYMENT_COMPLETED");
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }

    @Test
    void createExpirationRemindersRequiresActiveOwnershipWithExpirationDate() {
        when(productOwnershipRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());
        ReminderScheduleCommand command =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PRODUCT_EXPIRATION,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> reminderService.createExpirationReminders(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue(
                        "code", ReminderService.EXPIRATION_REMINDER_REQUIRES_ACTIVE_OWNERSHIP)
                .hasMessageContaining("active ownership")
                .hasMessageContaining("expiration date");
    }

    @Test
    void sendDueRemindersMarksSendableReminderSent() {
        ReminderSchedule reminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PRODUCT_EXPIRATION,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 1));
        when(reminderRepository.findDueReminders(LocalDate.of(2026, 7, 11)))
                .thenReturn(List.of(reminder));

        List<ReminderScheduleView> views =
                reminderService.sendDueReminders(LocalDate.of(2026, 7, 11));

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(reminder.getSentAt()).isNotNull();
        assertThat(views).hasSize(1);
        verify(reminderRepository).save(reminder);
    }

    @Test
    void paymentReminderIsNotSentIfPaymentIsCompleted() {
        // KB BR-024 / item 397
        PaymentRecord paidPayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        paidPayment.markPaid(BigDecimal.valueOf(100), java.time.Instant.now());
        ReminderSchedule reminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.RED,
                        LocalDate.of(2026, 7, 1));

        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(paidPayment));
        when(reminderRepository.findDueReminders(LocalDate.of(2026, 7, 11)))
                .thenReturn(List.of(reminder));

        List<ReminderScheduleView> views =
                reminderService.sendDueReminders(LocalDate.of(2026, 7, 11));

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(reminder.getSentAt()).isNull();
        assertThat(views).hasSize(1);
        assertThat(views.get(0).status()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(views.get(0).sentAt()).isNull();
        verify(reminderRepository).save(reminder);
    }

    @Test
    void sendDueRemindersCancelsCompletedPaymentReminder() {
        PaymentRecord paidPayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        paidPayment.markPaid(BigDecimal.valueOf(100), java.time.Instant.now());
        ReminderSchedule reminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.RED,
                        LocalDate.of(2026, 7, 1));

        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(paidPayment));
        when(reminderRepository.findDueReminders(LocalDate.of(2026, 7, 11)))
                .thenReturn(List.of(reminder));

        reminderService.sendDueReminders(LocalDate.of(2026, 7, 11));

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(reminder.getSentAt()).isNull();
        verify(reminderRepository).save(reminder);
    }

    @Test
    void markSentDoesNotSendPaymentReminderWhenPaymentIsCompleted() {
        // KB BR-024: mark-sent path must cancel rather than set SENT.
        PaymentRecord paidPayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        paidPayment.markPaid(BigDecimal.valueOf(100), java.time.Instant.now());
        ReminderSchedule reminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 1));
        when(paymentRecordRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(paidPayment));
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(reminder));

        ReminderScheduleView view = reminderService.markSent(REMINDER_ID);

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(reminder.getSentAt()).isNull();
        assertThat(view.status()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(view.sentAt()).isNull();
        verify(reminderRepository).save(reminder);
    }

    @Test
    void sendDueRemindersCancelsReminderWhenEligibilityOrContactFrequencyFails() {
        ReminderSchedule reminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 1));
        when(reminderRepository.findDueReminders(LocalDate.of(2026, 7, 11)))
                .thenReturn(List.of(reminder));
        when(eligibilityService.evaluateForReminder(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(
                        EligibilityDecision.excluded(
                                EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT));

        List<ReminderScheduleView> views =
                reminderService.sendDueReminders(LocalDate.of(2026, 7, 11));

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(reminder.getSentAt()).isNull();
        assertThat(views).hasSize(1);
        assertThat(views.get(0).status()).isEqualTo(ReminderStatus.CANCELLED);
        verify(reminderRepository).save(reminder);
    }

    @Test
    void markSentUpdatesReminderStatus() {
        ReminderSchedule reminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.now());
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(reminder));

        ReminderScheduleView view = reminderService.markSent(REMINDER_ID);

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(view.status()).isEqualTo(ReminderStatus.SENT);
        verify(reminderRepository).save(reminder);
    }

    @Test
    void markSentCancelsReminderWhenEligibilityOrContactFrequencyFails() {
        ReminderSchedule reminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.now());
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(reminder));
        when(eligibilityService.evaluateForReminder(
                        CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(
                        EligibilityDecision.excluded(
                                EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT));

        ReminderScheduleView view = reminderService.markSent(REMINDER_ID);

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(view.status()).isEqualTo(ReminderStatus.CANCELLED);
        verify(reminderRepository).save(reminder);
    }

    @Test
    void cancelReminderUpdatesReminderStatus() {
        ReminderSchedule reminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.now());
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(reminder));

        ReminderScheduleView view = reminderService.cancelReminder(REMINDER_ID);

        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(view.status()).isEqualTo(ReminderStatus.CANCELLED);
        verify(reminderRepository).save(reminder);
    }

    @Test
    void searchRemindersFiltersRepositoryResultsByCriteria() {
        ReminderSchedule matching =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 1));
        matching.markSent();
        ReminderSchedule pending =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.YELLOW,
                        LocalDate.of(2026, 7, 1));

        when(reminderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(matching, pending));

        List<ReminderScheduleView> views =
                reminderService.searchReminders(
                        new ReminderScheduleSearchCriteria(
                                CUSTOMER_ID, ReminderStatus.SENT, null));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).status()).isEqualTo(ReminderStatus.SENT);
    }

    @Test
    void createReminderValidatesRequiredFieldsAndType() {
        ReminderScheduleCommand command =
                new ReminderScheduleCommand(
                        null,
                        PRODUCT_ID,
                        ReminderType.PRODUCT_EXPIRATION,
                        ReminderLevel.GREEN,
                        LocalDate.now());

        assertThatThrownBy(() -> reminderService.createPaymentReminders(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("invalid");
    }

    @Test
    void serviceMethodsDeclareKbAuthorization() throws Exception {
        assertPreAuthorizeWithExpression(
                "createPaymentReminders",
                new Class<?>[] {ReminderScheduleCommand.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')");
        assertPreAuthorizeWithExpression(
                "createExpirationReminders",
                new Class<?>[] {ReminderScheduleCommand.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')");
        assertPreAuthorizeWithExpression(
                "generatePaymentDueReminders",
                new Class<?>[] {LocalDate.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')");
        assertPreAuthorizeWithExpression(
                "generateThreeMonthExpirationReminders",
                new Class<?>[] {LocalDate.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')");
        assertPreAuthorizeWithExpression(
                "generateSixMonthExpirationReminders",
                new Class<?>[] {LocalDate.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')");
        assertPreAuthorizeWithExpression(
                "generateTwelveMonthExpirationReminders",
                new Class<?>[] {LocalDate.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT')");
        assertPreAuthorizeWithExpression(
                "sendDueReminders",
                new Class<?>[] {LocalDate.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')");
        assertPreAuthorizeWithExpression(
                "markSent",
                new Class<?>[] {UUID.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')");
        assertPreAuthorizeWithExpression(
                "cancelReminder",
                new Class<?>[] {UUID.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')");
        assertPreAuthorizeWithExpression(
                "searchReminders",
                new Class<?>[] {ReminderScheduleSearchCriteria.class},
                "@authz.hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'CUSTOMER_SERVICE_AGENT', 'SALES_AGENT', 'COMPLIANCE_OFFICER')");
    }

    private static void assertPreAuthorizeWithExpression(
            String methodName, Class<?>[] parameterTypes, String expectedExpression)
            throws Exception {
        Method method = ReminderService.class.getMethod(methodName, parameterTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expectedExpression);
    }
}
