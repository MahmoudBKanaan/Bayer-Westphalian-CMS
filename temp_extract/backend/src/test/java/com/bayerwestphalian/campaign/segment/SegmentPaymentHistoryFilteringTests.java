package com.bayerwestphalian.campaign.segment;

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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for KB FR-074 payment-history field helpers used by segment criteria matching.
 */
class SegmentPaymentHistoryFilteringTests {

    @Test
    void recognizesKbPaymentHistoryFieldNames() {
        assertThat(SegmentPaymentHistorySupport.isPaymentHistoryField("payment_status")).isTrue();
        assertThat(SegmentPaymentHistorySupport.isPaymentHistoryField("PAYMENT_STATUS")).isTrue();
        assertThat(SegmentPaymentHistorySupport.isPaymentHistoryField("payment_history")).isTrue();
        assertThat(SegmentPaymentHistorySupport.isPaymentHistoryField("reminder_count")).isTrue();
        assertThat(SegmentPaymentHistorySupport.isPaymentHistoryField("days_overdue")).isTrue();
        assertThat(SegmentPaymentHistorySupport.isPaymentHistoryField("default_risk")).isTrue();
        assertThat(SegmentPaymentHistorySupport.isPaymentHistoryField("product_type")).isFalse();
    }

    @Test
    void canonicalizesPaymentHistoryAliasesToKbFieldNames() {
        assertThat(SegmentPaymentHistorySupport.canonicalizeFieldName("paymentstatus"))
                .isEqualTo("payment_status");
        assertThat(SegmentPaymentHistorySupport.canonicalizeFieldName("payment_history"))
                .isEqualTo("payment_status");
        assertThat(SegmentPaymentHistorySupport.canonicalizeFieldName("remindercount"))
                .isEqualTo("reminder_count");
        assertThat(SegmentPaymentHistorySupport.canonicalizeFieldName("daysoverdue"))
                .isEqualTo("days_overdue");
        assertThat(SegmentPaymentHistorySupport.canonicalizeFieldName("defaultrisk"))
                .isEqualTo("default_risk");
    }

    @Test
    void normalizesPaymentHistoryFilterValues() {
        assertThat(
                        SegmentPaymentHistorySupport.normalizeFilterValue(
                                SegmentOperator.EQUALS, "payment_status", "overdue"))
                .isEqualTo("OVERDUE");
        assertThat(
                        SegmentPaymentHistorySupport.normalizeFilterValue(
                                SegmentOperator.IN,
                                "payment_history",
                                "due, default_risk"))
                .isEqualTo("DUE,DEFAULT_RISK");
        assertThat(
                        SegmentPaymentHistorySupport.normalizeFilterValue(
                                SegmentOperator.EQUALS, "default_risk", "YES"))
                .isEqualTo("true");
        assertThat(
                        SegmentPaymentHistorySupport.normalizeFilterValue(
                                SegmentOperator.BETWEEN, "reminder_count", "1,3"))
                .isEqualTo("1,3");
    }

    @Test
    void matchesCustomersWithPaymentStatusUsingKbOperators() {
        PaymentRecord overduePayment = overduePayment();
        PaymentRecord paidPayment = paidPayment();

        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(overduePayment),
                                SegmentOperator.EQUALS,
                                "payment_status",
                                "OVERDUE"))
                .isTrue();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(overduePayment),
                                SegmentOperator.NOT_EQUALS,
                                "payment_status",
                                "OVERDUE"))
                .isFalse();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(paidPayment),
                                SegmentOperator.NOT_EQUALS,
                                "payment_history",
                                "OVERDUE"))
                .isTrue();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(overduePayment, paidPayment),
                                SegmentOperator.IN,
                                "payment_status",
                                "OVERDUE,DEFAULT_RISK"))
                .isTrue();
    }

    @Test
    void matchesCustomersWithReminderCountAndDaysOverdue() {
        PaymentRecord defaultRiskPayment = defaultRiskPayment();

        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(defaultRiskPayment),
                                SegmentOperator.EQUALS,
                                "reminder_count",
                                "3"))
                .isTrue();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(defaultRiskPayment),
                                SegmentOperator.AFTER,
                                "reminder_count",
                                "2"))
                .isTrue();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(defaultRiskPayment),
                                SegmentOperator.AFTER,
                                "days_overdue",
                                "0"))
                .isTrue();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(paidPayment()),
                                SegmentOperator.EQUALS,
                                "days_overdue",
                                "0"))
                .isTrue();
    }

    @Test
    void matchesCustomersWithDefaultRiskFlag() {
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(defaultRiskPayment()),
                                SegmentOperator.EQUALS,
                                "default_risk",
                                "true"))
                .isTrue();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(paidPayment()),
                                SegmentOperator.EQUALS,
                                "default_risk",
                                "false"))
                .isTrue();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(paidPayment()),
                                SegmentOperator.NOT_EQUALS,
                                "default_risk",
                                "true"))
                .isTrue();
    }

    @Test
    void treatsMissingPaymentsAsNonMatchingStatusAndZeroAggregates() {
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(),
                                SegmentOperator.EQUALS,
                                "payment_status",
                                "OVERDUE"))
                .isFalse();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(),
                                SegmentOperator.NOT_EQUALS,
                                "payment_status",
                                "OVERDUE"))
                .isTrue();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(),
                                SegmentOperator.EQUALS,
                                "reminder_count",
                                "0"))
                .isTrue();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(),
                                SegmentOperator.EQUALS,
                                "default_risk",
                                "false"))
                .isTrue();
    }

    @Test
    void rejectsUnsupportedPaymentHistoryFilterValues() {
        assertThatThrownBy(
                        () ->
                                SegmentPaymentHistorySupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "payment_status", "PENDING"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be one of DUE, PAID, OVERDUE, or DEFAULT_RISK");

        assertThatThrownBy(
                        () ->
                                SegmentPaymentHistorySupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "reminder_count", "-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative integer");

        assertThatThrownBy(
                        () ->
                                SegmentPaymentHistorySupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "default_risk", "maybe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("true, false");

        assertThatThrownBy(
                        () ->
                                SegmentPaymentHistorySupport.validateFilterValue(
                                        SegmentOperator.BETWEEN, "reminder_count", "1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lower and upper bounds");
    }

    @Test
    void matchesDuePaymentStatusAndMaxAggregatesAcrossMultiplePayments() {
        PaymentRecord due =
                PaymentRecord.create(
                        customer("Due", "Owner"),
                        ownership(ProductType.HEALTH_INSURANCE),
                        LocalDate.now().minusDays(2),
                        new BigDecimal("50.00"));
        assertThat(due.getStatus()).isEqualTo(PaymentStatus.DUE);

        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(due),
                                SegmentOperator.EQUALS,
                                "payment_status",
                                "DUE"))
                .isTrue();

        PaymentRecord lowReminders = overduePayment();
        lowReminders.incrementReminder(); // 1 reminder, still OVERDUE-ish path
        PaymentRecord highReminders = defaultRiskPayment(); // 3 reminders

        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(lowReminders, highReminders),
                                SegmentOperator.EQUALS,
                                "reminder_count",
                                "3"))
                .isTrue();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(lowReminders, highReminders),
                                SegmentOperator.BETWEEN,
                                "reminder_count",
                                "2,5"))
                .isTrue();
    }

    @Test
    void matchesDaysOverdueBeforeAndBetweenOperators() {
        PaymentRecord overdue = overduePayment();
        long days = overdue.calculateDaysOverdue();
        assertThat(days).isGreaterThan(0);

        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(overdue),
                                SegmentOperator.BEFORE,
                                "days_overdue",
                                String.valueOf(days + 10)))
                .isTrue();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(overdue),
                                SegmentOperator.BETWEEN,
                                "days_overdue",
                                "1," + (days + 5)))
                .isTrue();
        assertThat(
                        SegmentPaymentHistorySupport.matchesCustomerPayments(
                                List.of(paidPayment()),
                                SegmentOperator.EQUALS,
                                "days_overdue",
                                "0"))
                .isTrue();
    }

    @Test
    void normalizesDotDotBetweenBoundsForReminderCount() {
        assertThat(
                        SegmentPaymentHistorySupport.normalizeFilterValue(
                                SegmentOperator.BETWEEN, "reminder_count", "1..4"))
                .isEqualTo("1..4");
        SegmentPaymentHistorySupport.validateFilterValue(
                SegmentOperator.BETWEEN, "days_overdue", "0..30");
    }

    private static PaymentRecord overduePayment() {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer("Overdue", "Owner"),
                        ownership(ProductType.LIFE_INSURANCE),
                        LocalDate.now().minusDays(12),
                        new BigDecimal("150.00"));
        payment.markOverdue();
        return payment;
    }

    private static PaymentRecord paidPayment() {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer("Paid", "Owner"),
                        ownership(ProductType.AUTO_INSURANCE),
                        LocalDate.now().minusDays(3),
                        new BigDecimal("90.00"));
        payment.markPaid(new BigDecimal("90.00"), Instant.now());
        return payment;
    }

    private static PaymentRecord defaultRiskPayment() {
        PaymentRecord payment =
                PaymentRecord.create(
                        customer("Risk", "Owner"),
                        ownership(ProductType.HOMEOWNER_INSURANCE),
                        LocalDate.now().minusDays(40),
                        new BigDecimal("220.00"));
        payment.incrementReminder();
        payment.incrementReminder();
        payment.incrementReminder();
        return payment;
    }

    private static Customer customer(String firstName, String lastName) {
        return Customer.create(CustomerType.CUSTOMER, firstName, lastName);
    }

    private static ProductOwnership ownership(ProductType productType) {
        Product product =
                Product.create(
                        "Payment History Product",
                        productType,
                        new BigDecimal("99.00"),
                        12);
        return ProductOwnership.create(
                customer("Owned", "Customer"),
                product,
                LocalDate.now().minusMonths(6),
                LocalDate.now().plusYears(1));
    }
}
