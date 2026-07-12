package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

class PaymentRecordTests {

    @Test
    void mapsKbPaymentRecordsTableAsJpaEntity() {
        assertThat(PaymentRecord.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(PaymentRecord.class.getAnnotation(Table.class).name())
                .isEqualTo("payment_records");
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<PaymentRecord> constructor = PaymentRecord.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsKbPaymentRecordColumnsAndValidationRules() throws Exception {
        assertColumn("id", "id", false, 255);
        assertColumn("dueDate", "due_date", false, 255);
        assertColumn("paidAt", "paid_at", true, 255);
        assertColumn("amountDue", "amount_due", false, 255);
        assertColumn("amountPaid", "amount_paid", true, 255);
        assertColumn("status", "status", false, 255);
        assertColumn("reminderCount", "reminder_count", false, 255);

        assertThat(field("customer").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("productOwnership").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("dueDate").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("amountDue").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("amountDue").getAnnotation(DecimalMin.class).value()).isEqualTo("0.00");
        assertThat(field("amountDue").getAnnotation(Digits.class).integer()).isEqualTo(10);
        assertThat(field("amountDue").getAnnotation(Digits.class).fraction()).isEqualTo(2);
        assertThat(field("amountPaid").getAnnotation(DecimalMin.class).value()).isEqualTo("0.00");
        assertThat(field("amountPaid").getAnnotation(Digits.class).integer()).isEqualTo(10);
        assertThat(field("amountPaid").getAnnotation(Digits.class).fraction()).isEqualTo(2);
        assertThat(field("status").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("reminderCount").isAnnotationPresent(PositiveOrZero.class)).isTrue();
        assertThat(field("id").getAnnotation(Column.class).updatable()).isFalse();
    }

    @Test
    void mapsCustomerAndProductOwnershipRelationships() throws Exception {
        assertRelationship("customer", Customer.class, "customer_id");
        assertRelationship("productOwnership", ProductOwnership.class, "product_ownership_id");
    }

    @Test
    void mapsPaymentStatusToKbPostgreSqlEnum() throws Exception {
        Field status = field("status");
        Column column = status.getAnnotation(Column.class);
        Enumerated enumerated = status.getAnnotation(Enumerated.class);
        JdbcTypeCode jdbcTypeCode = status.getAnnotation(JdbcTypeCode.class);

        assertThat(column.columnDefinition()).isEqualTo("payment_status");
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
        assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.NAMED_ENUM);
    }

    @Test
    void declaresKbPaymentStatusValues() {
        assertThat(PaymentStatus.values())
                .containsExactly(
                        PaymentStatus.DUE,
                        PaymentStatus.PAID,
                        PaymentStatus.OVERDUE,
                        PaymentStatus.DEFAULT_RISK);
    }

    @Test
    void createsDuePaymentRecordWithKbFields() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Payer");
        ProductOwnership ownership = ownershipFor(customer);
        LocalDate dueDate = LocalDate.now().plusDays(10);

        PaymentRecord payment =
                PaymentRecord.create(customer, ownership, dueDate, new BigDecimal("120.00"));

        assertThat(payment.getCustomer()).isSameAs(customer);
        assertThat(payment.getProductOwnership()).isSameAs(ownership);
        assertThat(payment.getDueDate()).isEqualTo(dueDate);
        assertThat(payment.getAmountDue()).isEqualByComparingTo("120.00");
        assertThat(payment.getAmountPaid()).isNull();
        assertThat(payment.getPaidAt()).isNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DUE);
        assertThat(payment.getReminderCount()).isZero();
        assertThat(payment.isDefaultRisk()).isFalse();
    }

    @Test
    void supportsKbPaymentReminderOverduePaidAndDefaultRiskLifecycle() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ben", "Payer");
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        ownershipFor(customer),
                        LocalDate.now().minusDays(5),
                        new BigDecimal("99.50"));

        payment.markOverdue();
        payment.incrementReminder();
        payment.incrementReminder();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.OVERDUE);
        assertThat(payment.getReminderCount()).isEqualTo(2);
        assertThat(payment.calculateDaysOverdue()).isEqualTo(5);
        assertThat(payment.isDefaultRisk()).isFalse();

        payment.incrementReminder();

        assertThat(payment.getReminderCount()).isEqualTo(3);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DEFAULT_RISK);
        assertThat(payment.isDefaultRisk()).isTrue();

        Instant paidAt = Instant.parse("2026-07-07T10:15:30Z");
        payment.markPaid(new BigDecimal("99.50"), paidAt);
        payment.incrementReminder();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getAmountPaid()).isEqualByComparingTo("99.50");
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
        assertThat(payment.getReminderCount()).isEqualTo(3);
        assertThat(payment.calculateDaysOverdue()).isZero();
    }

    @Test
    void updatesEditablePaymentRecordDetails() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Dana", "Payer");
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        ownershipFor(customer),
                        LocalDate.of(2026, 7, 15),
                        new BigDecimal("99.50"));

        payment.updateDetails(LocalDate.of(2026, 8, 1), new BigDecimal("120.25"));

        assertThat(payment.getDueDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(payment.getAmountDue()).isEqualByComparingTo("120.25");
    }

    @Test
    void prePersistCreatesIdForKbPrimaryKey() throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Clara", "Payer");
        PaymentRecord payment =
                PaymentRecord.create(
                        customer, ownershipFor(customer), LocalDate.now(), new BigDecimal("10.00"));
        Method onCreate = PaymentRecord.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);

        onCreate.invoke(payment);

        assertThat(payment.getId()).isNotNull();
    }

    private static ProductOwnership ownershipFor(Customer customer) {
        return ProductOwnership.create(
                customer,
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("129.99"),
                        24),
                LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(11));
    }

    private static void assertRelationship(
            String fieldName, Class<?> relationshipType, String columnName) throws Exception {
        Field field = field(fieldName);
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);

        assertThat(field.getType()).isEqualTo(relationshipType);
        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo(columnName);
        assertThat(joinColumn.nullable()).isFalse();
    }

    private static void assertColumn(
            String fieldName, String columnName, boolean nullable, int length) throws Exception {
        Column column = field(fieldName).getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(column.length()).isEqualTo(length);
    }

    private static Field field(String fieldName) throws Exception {
        return PaymentRecord.class.getDeclaredField(fieldName);
    }
}
