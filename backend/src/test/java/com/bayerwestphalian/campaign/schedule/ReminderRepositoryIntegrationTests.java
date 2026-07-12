package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(
        properties = {
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.jpa.hibernate.ddl-auto=validate"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class ReminderRepositoryIntegrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bwc_reminder_repository_tests")
                    .withUsername("bwc_app")
                    .withPassword("bwc_app");

    @Autowired private TestEntityManager entityManager;
    @Autowired private ReminderRepository reminderRepository;

    private Customer customer;
    private Product product;

    @DynamicPropertySource
    static void registerPostgreSqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Reminder");
        entityManager.persistAndFlush(customer);

        product =
                Product.create(
                        "Reminder Product",
                        ProductType.LIFE_INSURANCE,
                        BigDecimal.valueOf(100),
                        12);
        entityManager.persistAndFlush(product);
    }

    @Test
    void paymentDueReminderIsGeneratedAndPersisted() {
        LocalDate scheduledDate = LocalDate.of(2026, 7, 11);
        ReminderSchedule generated =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        scheduledDate);

        ReminderSchedule saved = reminderRepository.saveAndFlush(generated);
        entityManager.clear();

        ReminderSchedule reloaded = reminderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(reloaded.getReminderLevel()).isEqualTo(ReminderLevel.GREEN);
        assertThat(reloaded.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(reloaded.getScheduledDate()).isEqualTo(scheduledDate);
        assertThat(reloaded.getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(reloaded.getProduct().getId()).isEqualTo(product.getId());
        assertThat(reminderRepository.findByCustomerId(customer.getId()))
                .extracting(ReminderSchedule::getId)
                .contains(saved.getId());
        assertThat(reminderRepository.findDueReminders(scheduledDate))
                .extracting(ReminderSchedule::getId)
                .contains(saved.getId());
    }

    @Test
    void greenFirstPaymentReminderIsPersistedAsGreenLevel() {
        // KB BR-020: Green reminder is the first reminder.
        LocalDate scheduledDate = LocalDate.of(2026, 7, 11);
        ReminderLevel firstLevel =
                PaymentReminderLevelRules.resolveFromReminderCount(
                        PaymentReminderLevelRules.FIRST_REMINDER_COUNT);
        assertThat(firstLevel).isEqualTo(ReminderLevel.GREEN);

        ReminderSchedule firstReminder =
                new ReminderSchedule(
                        customer, product, ReminderType.PAYMENT_DUE, firstLevel, scheduledDate);
        ReminderSchedule saved = reminderRepository.saveAndFlush(firstReminder);
        entityManager.clear();

        ReminderSchedule reloaded = reminderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getReminderLevel()).isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.isFirstReminder(reloaded.getReminderLevel())).isTrue();
        assertThat(reloaded.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(reloaded.getStatus()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    void yellowSecondPaymentReminderIsPersistedAsYellowLevel() {
        // KB BR-021: Yellow reminder is the second reminder.
        LocalDate scheduledDate = LocalDate.of(2026, 7, 11);
        ReminderLevel secondLevel =
                PaymentReminderLevelRules.resolveFromReminderCount(
                        PaymentReminderLevelRules.SECOND_REMINDER_COUNT);
        assertThat(secondLevel).isEqualTo(ReminderLevel.YELLOW);

        ReminderSchedule secondReminder =
                new ReminderSchedule(
                        customer, product, ReminderType.PAYMENT_DUE, secondLevel, scheduledDate);
        ReminderSchedule saved = reminderRepository.saveAndFlush(secondReminder);
        entityManager.clear();

        ReminderSchedule reloaded = reminderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getReminderLevel()).isEqualTo(ReminderLevel.YELLOW);
        assertThat(PaymentReminderLevelRules.isSecondReminder(reloaded.getReminderLevel())).isTrue();
        assertThat(reloaded.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(reloaded.getStatus()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    void redThirdPaymentReminderIsPersistedAsRedLevel() {
        // KB BR-022: Red reminder is the third reminder.
        LocalDate scheduledDate = LocalDate.of(2026, 7, 11);
        ReminderLevel thirdLevel =
                PaymentReminderLevelRules.resolveFromReminderCount(
                        PaymentReminderLevelRules.THIRD_REMINDER_COUNT);
        assertThat(thirdLevel).isEqualTo(ReminderLevel.RED);

        ReminderSchedule thirdReminder =
                new ReminderSchedule(
                        customer, product, ReminderType.PAYMENT_DUE, thirdLevel, scheduledDate);
        ReminderSchedule saved = reminderRepository.saveAndFlush(thirdReminder);
        entityManager.clear();

        ReminderSchedule reloaded = reminderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getReminderLevel()).isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.isThirdReminder(reloaded.getReminderLevel())).isTrue();
        assertThat(reloaded.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(reloaded.getStatus()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    void cancelledPaymentReminderIsNotSentWhenPaymentCompleted() {
        // KB BR-024: completed-payment path cancels the schedule without setting sent_at.
        ReminderSchedule paymentReminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        LocalDate.of(2026, 7, 1));
        ReminderSchedule saved = reminderRepository.saveAndFlush(paymentReminder);
        entityManager.clear();

        ReminderSchedule loaded = reminderRepository.findById(saved.getId()).orElseThrow();
        loaded.cancel();
        reminderRepository.saveAndFlush(loaded);
        entityManager.clear();

        ReminderSchedule reloaded = reminderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(reloaded.getSentAt()).isNull();
        assertThat(reloaded.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(reminderRepository.findDueReminders(LocalDate.of(2026, 7, 11)))
                .extracting(ReminderSchedule::getId)
                .doesNotContain(saved.getId());
    }

    @Test
    void threeMonthProductExpirationReminderIsPersisted() {
        // KB BR-023 / item 398: 3-month product-expiration reminder (RED urgency).
        LocalDate asOf = LocalDate.of(2026, 7, 11);
        ReminderLevel level = ProductExpirationReminderRules.threeMonthReminderLevel();
        assertThat(ProductExpirationReminderRules.THREE_MONTH_WINDOW).isEqualTo(3);
        assertThat(level).isEqualTo(ReminderLevel.RED);

        ReminderSchedule generated =
                new ReminderSchedule(
                        customer, product, ReminderType.PRODUCT_EXPIRATION, level, asOf);
        ReminderSchedule saved = reminderRepository.saveAndFlush(generated);
        entityManager.clear();

        ReminderSchedule reloaded = reminderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getReminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(reloaded.getReminderLevel()).isEqualTo(ReminderLevel.RED);
        assertThat(reloaded.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(reloaded.getScheduledDate()).isEqualTo(asOf);
        assertThat(ProductExpirationReminderRules.threeMonthWindowEnd(asOf))
                .isEqualTo(LocalDate.of(2026, 10, 11));
    }

    @Test
    void sixMonthProductExpirationReminderIsPersisted() {
        // KB BR-023 / item 399: 6-month product-expiration reminder (YELLOW urgency).
        LocalDate asOf = LocalDate.of(2026, 7, 11);
        ReminderLevel level = ProductExpirationReminderRules.sixMonthReminderLevel();
        assertThat(ProductExpirationReminderRules.SIX_MONTH_WINDOW).isEqualTo(6);
        assertThat(level).isEqualTo(ReminderLevel.YELLOW);

        ReminderSchedule generated =
                new ReminderSchedule(
                        customer, product, ReminderType.PRODUCT_EXPIRATION, level, asOf);
        ReminderSchedule saved = reminderRepository.saveAndFlush(generated);
        entityManager.clear();

        ReminderSchedule reloaded = reminderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getReminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(reloaded.getReminderLevel()).isEqualTo(ReminderLevel.YELLOW);
        assertThat(reloaded.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(reloaded.getScheduledDate()).isEqualTo(asOf);
        assertThat(ProductExpirationReminderRules.sixMonthWindowEnd(asOf))
                .isEqualTo(LocalDate.of(2027, 1, 11));
    }

    @Test
    void twelveMonthProductExpirationReminderIsPersisted() {
        // KB BR-023 / item 400: 12-month product-expiration reminder (GREEN urgency).
        LocalDate asOf = LocalDate.of(2026, 7, 11);
        ReminderLevel level = ProductExpirationReminderRules.twelveMonthReminderLevel();
        assertThat(ProductExpirationReminderRules.TWELVE_MONTH_WINDOW).isEqualTo(12);
        assertThat(level).isEqualTo(ReminderLevel.GREEN);

        ReminderSchedule generated =
                new ReminderSchedule(
                        customer, product, ReminderType.PRODUCT_EXPIRATION, level, asOf);
        ReminderSchedule saved = reminderRepository.saveAndFlush(generated);
        entityManager.clear();

        ReminderSchedule reloaded = reminderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getReminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(reloaded.getReminderLevel()).isEqualTo(ReminderLevel.GREEN);
        assertThat(reloaded.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(reloaded.getScheduledDate()).isEqualTo(asOf);
        assertThat(ProductExpirationReminderRules.twelveMonthWindowEnd(asOf))
                .isEqualTo(LocalDate.of(2027, 7, 11));
    }

    @Test
    void findsDuePendingRemindersByScheduledDate() {
        ReminderSchedule dueYesterday =
                reminder(ReminderLevel.GREEN, LocalDate.now().minusDays(1));
        ReminderSchedule dueToday = reminder(ReminderLevel.YELLOW, LocalDate.now());
        ReminderSchedule future = reminder(ReminderLevel.RED, LocalDate.now().plusDays(1));
        ReminderSchedule failed = reminder(ReminderLevel.GREEN, LocalDate.now().minusDays(2));
        failed.markFailed();

        entityManager.persist(dueYesterday);
        entityManager.persist(dueToday);
        entityManager.persist(future);
        entityManager.persist(failed);
        entityManager.flush();

        List<ReminderSchedule> dueReminders =
                reminderRepository.findDueReminders(LocalDate.now());

        assertThat(dueReminders).containsExactly(dueYesterday, dueToday);
    }

    @Test
    void findsRemindersByStatusAndCustomerId() {
        Customer otherCustomer = Customer.create(CustomerType.CUSTOMER, "Other", "Reminder");
        entityManager.persistAndFlush(otherCustomer);

        ReminderSchedule pending = reminder(ReminderLevel.GREEN, LocalDate.now().plusDays(3));
        ReminderSchedule sent = reminder(ReminderLevel.YELLOW, LocalDate.now().minusDays(1));
        sent.markSent();
        ReminderSchedule otherCustomerPending =
                new ReminderSchedule(
                        otherCustomer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.RED,
                        LocalDate.now().plusDays(5));

        entityManager.persist(pending);
        entityManager.persist(sent);
        entityManager.persist(otherCustomerPending);
        entityManager.flush();

        assertThat(reminderRepository.findByStatus(ReminderStatus.PENDING))
                .containsExactly(pending, otherCustomerPending);
        assertThat(reminderRepository.findByCustomerId(customer.getId()))
                .containsExactly(sent, pending);
    }

    private ReminderSchedule reminder(ReminderLevel level, LocalDate scheduledDate) {
        return new ReminderSchedule(
                customer, product, ReminderType.PAYMENT_DUE, level, scheduledDate);
    }
}
