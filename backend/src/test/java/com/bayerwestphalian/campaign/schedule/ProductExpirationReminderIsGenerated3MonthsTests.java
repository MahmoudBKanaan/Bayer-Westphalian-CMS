package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bayerwestphalian.campaign.campaign.EligibilityDecision;
import com.bayerwestphalian.campaign.campaign.EligibilityExclusionReason;
import com.bayerwestphalian.campaign.campaign.EligibilityService;
import com.bayerwestphalian.campaign.consent.ConsentType;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerRepository;
import com.bayerwestphalian.campaign.customer.CustomerType;
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
 * KB item 398 / BR-023: product-expiration reminder is generated 3 months before expiration via
 * {@link ReminderService#generateThreeMonthExpirationReminders(LocalDate)}.
 */
class ProductExpirationReminderIsGenerated3MonthsTests {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000398");
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000398");
    private static final LocalDate AS_OF_DATE = LocalDate.of(2026, 7, 11);
    private static final LocalDate THREE_MONTH_WINDOW_END = LocalDate.of(2026, 10, 11);

    private ReminderRepository reminderRepository;
    private ProductOwnershipRepository productOwnershipRepository;
    private EligibilityService eligibilityService;
    private ReminderService reminderService;

    private Customer customer;
    private Product product;

    @BeforeEach
    void setUp() {
        reminderRepository = Mockito.mock(ReminderRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        PaymentRecordRepository paymentRecordRepository = Mockito.mock(PaymentRecordRepository.class);
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

        customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Expiring");
        product =
                Product.create(
                        "Life Protection", ProductType.LIFE_INSURANCE, BigDecimal.valueOf(100), 12);
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);

        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(reminderRepository.save(any(ReminderSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reminderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(EligibilityDecision.included());
    }

    @Test
    void productExpirationReminderIsGeneratedThreeMonthsBeforeExpiration() {
        ProductOwnership expiringInThreeMonths =
                ProductOwnership.create(
                        customer, product, LocalDate.of(2025, 7, 1), THREE_MONTH_WINDOW_END);
        ReflectionTestUtils.setField(expiringInThreeMonths, "id", UUID.randomUUID());

        when(productOwnershipRepository.findExpiringBetween(AS_OF_DATE, THREE_MONTH_WINDOW_END))
                .thenReturn(List.of(expiringInThreeMonths));

        List<ReminderScheduleView> views =
                reminderService.generateThreeMonthExpirationReminders(AS_OF_DATE);

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);
        verify(reminderRepository).save(captor.capture());
        ReminderSchedule saved = captor.getValue();

        assertThat(ProductExpirationReminderRules.THREE_MONTH_WINDOW).isEqualTo(3);
        assertThat(ProductExpirationReminderRules.threeMonthWindowEnd(AS_OF_DATE))
                .isEqualTo(THREE_MONTH_WINDOW_END);
        assertThat(saved.getReminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(saved.getReminderLevel())
                .isEqualTo(ProductExpirationReminderRules.threeMonthReminderLevel())
                .isEqualTo(ReminderLevel.RED);
        assertThat(saved.getScheduledDate()).isEqualTo(AS_OF_DATE);
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getProduct()).isSameAs(product);
        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.RED);
        verify(productOwnershipRepository).findExpiringBetween(AS_OF_DATE, THREE_MONTH_WINDOW_END);
    }

    @Test
    void threeMonthGenerationUsesExactThreeMonthSearchWindow() {
        when(productOwnershipRepository.findExpiringBetween(AS_OF_DATE, THREE_MONTH_WINDOW_END))
                .thenReturn(List.of());

        reminderService.generateThreeMonthExpirationReminders(AS_OF_DATE);

        verify(productOwnershipRepository)
                .findExpiringBetween(
                        eq(AS_OF_DATE),
                        eq(
                                ProductExpirationReminderRules.windowEnd(
                                        AS_OF_DATE,
                                        ProductExpirationReminderRules.THREE_MONTH_WINDOW)));
        verify(productOwnershipRepository, never())
                .findExpiringBetween(AS_OF_DATE, AS_OF_DATE.plusMonths(6));
        verify(productOwnershipRepository, never())
                .findExpiringBetween(AS_OF_DATE, AS_OF_DATE.plusMonths(12));
    }

    @Test
    void threeMonthGenerationUsesTodayWhenAsOfDateIsNull() {
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = ProductExpirationReminderRules.threeMonthWindowEnd(today);
        ProductOwnership ownership =
                ProductOwnership.create(customer, product, today.minusYears(1), windowEnd);
        ReflectionTestUtils.setField(ownership, "id", UUID.randomUUID());
        when(productOwnershipRepository.findExpiringBetween(today, windowEnd))
                .thenReturn(List.of(ownership));

        List<ReminderScheduleView> views =
                reminderService.generateThreeMonthExpirationReminders(null);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).reminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(views.get(0).reminderLevel()).isEqualTo(ReminderLevel.RED);
        assertThat(views.get(0).scheduledDate()).isEqualTo(today);
        verify(productOwnershipRepository).findExpiringBetween(today, windowEnd);
    }

    @Test
    void threeMonthGenerationSkipsDuplicateExistingRedExpirationReminder() {
        ProductOwnership ownership =
                ProductOwnership.create(
                        customer, product, LocalDate.of(2025, 7, 1), THREE_MONTH_WINDOW_END);
        ReminderSchedule existing =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PRODUCT_EXPIRATION,
                        ReminderLevel.RED,
                        AS_OF_DATE);
        when(productOwnershipRepository.findExpiringBetween(AS_OF_DATE, THREE_MONTH_WINDOW_END))
                .thenReturn(List.of(ownership));
        when(reminderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(existing));

        List<ReminderScheduleView> views =
                reminderService.generateThreeMonthExpirationReminders(AS_OF_DATE);

        assertThat(views).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }

    @Test
    void threeMonthGenerationSkipsIneligibleCustomers() {
        ProductOwnership ownership =
                ProductOwnership.create(
                        customer, product, LocalDate.of(2025, 7, 1), THREE_MONTH_WINDOW_END);
        ReflectionTestUtils.setField(ownership, "id", UUID.randomUUID());
        when(productOwnershipRepository.findExpiringBetween(AS_OF_DATE, THREE_MONTH_WINDOW_END))
                .thenReturn(List.of(ownership));
        when(eligibilityService.evaluateForReminder(CUSTOMER_ID, ConsentType.MARKETING_EMAIL))
                .thenReturn(EligibilityDecision.excluded(EligibilityExclusionReason.DO_NOT_CONTACT));

        List<ReminderScheduleView> views =
                reminderService.generateThreeMonthExpirationReminders(AS_OF_DATE);

        assertThat(views).isEmpty();
        verify(reminderRepository, never()).save(any(ReminderSchedule.class));
    }
}
