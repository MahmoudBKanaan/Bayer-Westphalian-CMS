package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 401 / BR-011 / FR-034 / FR-092: reminder respects consent and contact limits.
 *
 * <p>All reminder create, generate, and send paths must evaluate the recipient through {@link
 * EligibilityService#evaluateForReminder} with marketing-email consent. Invalid consent, marketing
 * opt-out, and monthly contact-limit failures block scheduling, skip generation, and cancel due
 * send instead of marking the reminder SENT.
 */
class ReminderRespectsConsentAndContactLimitsTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000401");
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000401");
    private static final UUID REMINDER_ID = UUID.fromString("90000000-0000-0000-0000-000000000401");
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

        customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Eligible");
        product =
                Product.create(
                        "Life Protection", ProductType.LIFE_INSURANCE, BigDecimal.valueOf(100), 12);
        ownership =
                ProductOwnership.create(
                        customer, product, LocalDate.of(2025, 7, 1), LocalDate.of(2027, 7, 11));

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

    @ParameterizedTest(name = "createPaymentReminders rejects {0}")
    @MethodSource("consentAndContactLimitExclusions")
    void createPaymentRemindersRejectsConsentOrContactLimitFailures(
            String scenario, EligibilityExclusionReason reason) {
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(EligibilityDecision.excluded(reason));

        ReminderScheduleCommand command =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        AS_OF_DATE);

        assertThatThrownBy(() -> reminderService.createPaymentReminders(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue(
                        "code", ReminderService.REMINDER_RECIPIENT_INELIGIBLE)
                .hasMessageContaining(reason.explanation());

        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
        verify(eligibilityService).evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL);
    }

    @ParameterizedTest(name = "createExpirationReminders rejects {0}")
    @MethodSource("consentAndContactLimitExclusions")
    void createExpirationRemindersRejectsConsentOrContactLimitFailures(
            String scenario, EligibilityExclusionReason reason) {
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(EligibilityDecision.excluded(reason));

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
                        "code", ReminderService.REMINDER_RECIPIENT_INELIGIBLE)
                .hasMessageContaining(reason.explanation());

        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
        verify(eligibilityService).evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL);
    }

    @ParameterizedTest(name = "generatePaymentDueReminders skips {0}")
    @MethodSource("consentAndContactLimitExclusions")
    void generatePaymentDueRemindersSkipsConsentOrContactLimitFailures(
            String scenario, EligibilityExclusionReason reason) {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(EligibilityDecision.excluded(reason));

        List<ReminderScheduleView> views =
                reminderService.generatePaymentDueReminders(AS_OF_DATE);

        assertThat(views).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
        verify(eligibilityService).evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL);
    }

    @ParameterizedTest(name = "generate expiration reminders skip {0}")
    @MethodSource("consentAndContactLimitExclusions")
    void generateProductExpirationRemindersSkipConsentOrContactLimitFailures(
            String scenario, EligibilityExclusionReason reason) {
        LocalDate windowEnd = ProductExpirationReminderRules.threeMonthWindowEnd(AS_OF_DATE);
        ProductOwnership expiring =
                ProductOwnership.create(customer, product, LocalDate.of(2025, 7, 1), windowEnd);
        ReflectionTestUtils.setField(expiring, "id", UUID.randomUUID());
        when(productOwnershipRepository.findExpiringBetween(AS_OF_DATE, windowEnd))
                .thenReturn(List.of(expiring));
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(EligibilityDecision.excluded(reason));

        List<ReminderScheduleView> views =
                reminderService.generateThreeMonthExpirationReminders(AS_OF_DATE);

        assertThat(views).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
        verify(eligibilityService).evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL);
    }

    @ParameterizedTest(name = "sendDueReminders cancels when {0}")
    @MethodSource("consentAndContactLimitExclusions")
    void sendDueRemindersCancelsWhenConsentOrContactLimitFails(
            String scenario, EligibilityExclusionReason reason) {
        ReminderSchedule pending =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 1));
        when(reminderRepository.findDueReminders(AS_OF_DATE)).thenReturn(List.of(pending));
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(EligibilityDecision.excluded(reason));

        List<ReminderScheduleView> views = reminderService.sendDueReminders(AS_OF_DATE);

        assertThat(pending.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(pending.getSentAt()).isNull();
        assertThat(views).hasSize(1);
        assertThat(views.get(0).status()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(views.get(0).sentAt()).isNull();
        verify(reminderRepository).save(pending);
        verify(eligibilityService).evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL);
    }

    @ParameterizedTest(name = "markSent cancels when {0}")
    @MethodSource("consentAndContactLimitExclusions")
    void markSentCancelsWhenConsentOrContactLimitFails(
            String scenario, EligibilityExclusionReason reason) {
        ReminderSchedule pending =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PRODUCT_EXPIRATION,
                        ReminderLevel.GREEN,
                        AS_OF_DATE);
        when(reminderRepository.findById(REMINDER_ID)).thenReturn(Optional.of(pending));
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(EligibilityDecision.excluded(reason));

        ReminderScheduleView view = reminderService.markSent(REMINDER_ID);

        assertThat(pending.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(pending.getSentAt()).isNull();
        assertThat(view.status()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(view.sentAt()).isNull();
        verify(reminderRepository).save(pending);
        verify(eligibilityService).evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL);
    }

    @Test
    void eligibleRecipientIsAllowedThroughCreateGenerateAndSend() {
        PaymentRecord duePayment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(100));
        when(paymentRecordRepository.findDuePayments()).thenReturn(List.of(duePayment));
        when(paymentRecordRepository.findOverduePayments()).thenReturn(List.of());

        ReminderScheduleCommand createCommand =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        AS_OF_DATE);
        ReminderScheduleView created = reminderService.createPaymentReminders(createCommand);
        assertThat(created.status()).isEqualTo(ReminderStatus.PENDING);

        List<ReminderScheduleView> generated =
                reminderService.generatePaymentDueReminders(AS_OF_DATE);
        assertThat(generated).hasSize(1);
        assertThat(generated.get(0).reminderType()).isEqualTo(ReminderType.PAYMENT_DUE);

        ReminderSchedule pending =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 1));
        when(reminderRepository.findDueReminders(AS_OF_DATE)).thenReturn(List.of(pending));
        List<ReminderScheduleView> sent = reminderService.sendDueReminders(AS_OF_DATE);
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).status()).isEqualTo(ReminderStatus.SENT);
        assertThat(pending.getStatus()).isEqualTo(ReminderStatus.SENT);

        // Create + generate candidate check + send each evaluate marketing-email consent/limits.
        verify(eligibilityService, times(3))
                .evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL);
    }

    @Test
    void reminderEligibilityUsesMarketingEmailConsentType() {
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(
                        EligibilityDecision.excluded(
                                EligibilityExclusionReason.INVALID_CONSENT));

        ReminderScheduleCommand command =
                new ReminderScheduleCommand(
                        CUSTOMER_ID,
                        PRODUCT_ID,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        AS_OF_DATE);

        assertThatThrownBy(() -> reminderService.createPaymentReminders(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue(
                        "code", ReminderService.REMINDER_RECIPIENT_INELIGIBLE);

        verify(eligibilityService)
                .evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL);
        verify(eligibilityService, never())
                .evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_SMS);
        verify(eligibilityService, never())
                .evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_PHONE);
    }

    static Stream<Arguments> consentAndContactLimitExclusions() {
        return Stream.of(
                Arguments.of("invalid consent", EligibilityExclusionReason.INVALID_CONSENT),
                Arguments.of("marketing opt-out", EligibilityExclusionReason.MARKETING_OPT_OUT),
                Arguments.of(
                        "monthly contact limit", EligibilityExclusionReason.MONTHLY_CONTACT_LIMIT),
                Arguments.of("do-not-contact", EligibilityExclusionReason.DO_NOT_CONTACT));
    }
}
