package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.PaymentRecord;
import com.bayerwestphalian.campaign.product.PaymentStatus;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit coverage for {@link PaymentReminderLevelRules} (KB BR-020–BR-022 / item 405 Green/Yellow/Red
 * reminder rules). Items 394–396 cover first/second/third acceptance cases.
 */
class PaymentReminderLevelRulesTests {

    @Test
    void greenReminderIsFirstReminderForZeroReminderCount() {
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(0))
                .isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.isFirstReminder(ReminderLevel.GREEN)).isTrue();
    }

    @Test
    void yellowReminderIsSecondReminderForOneReminderCount() {
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(1))
                .isEqualTo(ReminderLevel.YELLOW);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(
                        PaymentReminderLevelRules.SECOND_REMINDER_COUNT))
                .isEqualTo(ReminderLevel.YELLOW);
        assertThat(PaymentReminderLevelRules.isSecondReminder(ReminderLevel.YELLOW)).isTrue();
        assertThat(PaymentReminderLevelRules.isSecondReminder(ReminderLevel.GREEN)).isFalse();
        assertThat(PaymentReminderLevelRules.isSecondReminder(ReminderLevel.RED)).isFalse();
    }

    @Test
    void redReminderIsThirdReminderForTwoOrMoreReminderCount() {
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(2))
                .isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(
                        PaymentReminderLevelRules.THIRD_REMINDER_COUNT))
                .isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(3))
                .isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.isThirdReminder(ReminderLevel.RED)).isTrue();
        assertThat(PaymentReminderLevelRules.isThirdReminder(ReminderLevel.GREEN)).isFalse();
        assertThat(PaymentReminderLevelRules.isThirdReminder(ReminderLevel.YELLOW)).isFalse();
    }

    @Test
    void newPaymentRecordResolvesToGreenFirstReminder() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Green");
        Product product =
                Product.create(
                        "Auto Cover", ProductType.AUTO_INSURANCE, BigDecimal.valueOf(50), 12);
        ProductOwnership ownership =
                ProductOwnership.create(customer, product, LocalDate.of(2026, 1, 1), null);
        PaymentRecord payment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(50));

        assertThat(payment.getReminderCount()).isZero();
        assertThat(PaymentReminderLevelRules.resolve(payment)).isEqualTo(ReminderLevel.GREEN);
    }

    @Test
    void paymentWithOnePriorReminderResolvesToYellowSecondReminder() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Yellow");
        Product product =
                Product.create(
                        "Auto Cover", ProductType.AUTO_INSURANCE, BigDecimal.valueOf(50), 12);
        ProductOwnership ownership =
                ProductOwnership.create(customer, product, LocalDate.of(2026, 1, 1), null);
        PaymentRecord payment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(50));
        payment.incrementReminder();

        assertThat(payment.getReminderCount()).isEqualTo(1);
        assertThat(PaymentReminderLevelRules.resolve(payment)).isEqualTo(ReminderLevel.YELLOW);
        assertThat(PaymentReminderLevelRules.isSecondReminder(
                        PaymentReminderLevelRules.resolve(payment)))
                .isTrue();
    }

    @Test
    void paymentWithTwoPriorRemindersResolvesToRedThirdReminder() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Red");
        Product product =
                Product.create(
                        "Auto Cover", ProductType.AUTO_INSURANCE, BigDecimal.valueOf(50), 12);
        ProductOwnership ownership =
                ProductOwnership.create(customer, product, LocalDate.of(2026, 1, 1), null);
        PaymentRecord payment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(50));
        payment.incrementReminder();
        payment.incrementReminder();

        assertThat(payment.getReminderCount()).isEqualTo(2);
        assertThat(PaymentReminderLevelRules.resolve(payment)).isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.isThirdReminder(
                        PaymentReminderLevelRules.resolve(payment)))
                .isTrue();
    }

    @Test
    void escalationOrderIsGreenThenYellowThenRed() {
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(0))
                .isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(1))
                .isEqualTo(ReminderLevel.YELLOW);
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(2))
                .isEqualTo(ReminderLevel.RED);
    }

    @Test
    void defaultRiskPaymentAlwaysResolvesToRedRegardlessOfReminderCount() {
        // KB BR-022 / item 405: DEFAULT_RISK indicates likely default → Red.
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "DefaultRisk");
        Product product =
                Product.create(
                        "Auto Cover", ProductType.AUTO_INSURANCE, BigDecimal.valueOf(50), 12);
        ProductOwnership ownership =
                ProductOwnership.create(customer, product, LocalDate.of(2026, 1, 1), null);
        PaymentRecord payment =
                PaymentRecord.create(
                        customer, ownership, LocalDate.of(2026, 7, 1), BigDecimal.valueOf(50));
        ReflectionTestUtils.setField(payment, "reminderCount", 0);
        ReflectionTestUtils.setField(payment, "status", PaymentStatus.DEFAULT_RISK);

        assertThat(payment.getReminderCount()).isZero();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DEFAULT_RISK);
        assertThat(PaymentReminderLevelRules.resolve(payment)).isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.isThirdReminder(
                        PaymentReminderLevelRules.resolve(payment)))
                .isTrue();
    }

    @Test
    void negativeReminderCountDefensivelyResolvesToGreenFirstReminder() {
        assertThat(PaymentReminderLevelRules.resolveFromReminderCount(-1))
                .isEqualTo(ReminderLevel.GREEN);
        assertThat(PaymentReminderLevelRules.isFirstReminder(ReminderLevel.GREEN)).isTrue();
    }

    @Test
    void resolveRejectsNullPayment() {
        assertThatThrownBy(() -> PaymentReminderLevelRules.resolve(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("payment is required");
    }

    @Test
    void reminderCountThresholdConstantsMatchKbEscalationSteps() {
        assertThat(PaymentReminderLevelRules.FIRST_REMINDER_COUNT).isEqualTo(0);
        assertThat(PaymentReminderLevelRules.SECOND_REMINDER_COUNT).isEqualTo(1);
        assertThat(PaymentReminderLevelRules.THIRD_REMINDER_COUNT).isEqualTo(2);
    }
}
