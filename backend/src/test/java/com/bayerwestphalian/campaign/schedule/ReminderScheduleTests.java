package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

class ReminderScheduleTests {

    @Test
    void mapsKbReminderSchedulesTableAsJpaEntity() throws Exception {
        assertThat(ReminderSchedule.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(ReminderSchedule.class.getAnnotation(Table.class).name())
                .isEqualTo("reminder_schedules");

        assertRelationship("customer", "customer_id");
        assertRelationship("product", "product_id");
        assertNamedEnum("reminderType", "reminder_type");
        assertNamedEnum("reminderLevel", "reminder_level");
        assertNamedEnum("status", "reminder_status");
        assertThat(field("scheduledDate").getAnnotation(Column.class).name())
                .isEqualTo("scheduled_date");
        assertThat(field("sentAt").getAnnotation(Column.class).name()).isEqualTo("sent_at");
    }

    @Test
    void initializesWithKbRequiredFieldsAndPendingStatus() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Mina", "Khan");
        Product product =
                Product.create(
                        "Education Plan", ProductType.INVESTMENT_FUND, BigDecimal.valueOf(100), 12);
        LocalDate scheduledDate = LocalDate.now().plusDays(3);

        ReminderSchedule reminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.GREEN,
                        scheduledDate);

        assertThat(reminder.getCustomer()).isSameAs(customer);
        assertThat(reminder.getProduct()).isSameAs(product);
        assertThat(reminder.getReminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(reminder.getReminderLevel()).isEqualTo(ReminderLevel.GREEN);
        assertThat(reminder.getScheduledDate()).isEqualTo(scheduledDate);
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(reminder.getSentAt()).isNull();
    }

    @Test
    void rejectsMissingRequiredFields() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Mina", "Khan");
        Product product =
                Product.create(
                        "Education Plan", ProductType.INVESTMENT_FUND, BigDecimal.valueOf(100), 12);
        LocalDate scheduledDate = LocalDate.now();

        assertThatThrownBy(
                        () ->
                                new ReminderSchedule(
                                        null,
                                        product,
                                        ReminderType.PAYMENT_DUE,
                                        ReminderLevel.GREEN,
                                        scheduledDate))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                new ReminderSchedule(
                                        customer,
                                        null,
                                        ReminderType.PAYMENT_DUE,
                                        ReminderLevel.GREEN,
                                        scheduledDate))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                new ReminderSchedule(
                                        customer,
                                        product,
                                        null,
                                        ReminderLevel.GREEN,
                                        scheduledDate))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                new ReminderSchedule(
                                        customer,
                                        product,
                                        ReminderType.PAYMENT_DUE,
                                        null,
                                        scheduledDate))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                new ReminderSchedule(
                                        customer,
                                        product,
                                        ReminderType.PAYMENT_DUE,
                                        ReminderLevel.GREEN,
                                        null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void marksSentFailedAndCancelledForReminderLifecycle() {
        ReminderSchedule reminder = reminder(LocalDate.now());

        reminder.markSent();
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.SENT);
        assertThat(reminder.getSentAt()).isNotNull();
        assertThat(reminder.isDue()).isFalse();

        reminder.markFailed();
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.FAILED);
        assertThat(reminder.getSentAt()).isNull();

        reminder.cancel();
        assertThat(reminder.getStatus()).isEqualTo(ReminderStatus.CANCELLED);
        assertThat(reminder.getSentAt()).isNull();
    }

    @Test
    void isDueOnlyForPendingRemindersScheduledTodayOrEarlier() {
        assertThat(reminder(LocalDate.now().minusDays(1)).isDue()).isTrue();
        assertThat(reminder(LocalDate.now()).isDue()).isTrue();
        assertThat(reminder(LocalDate.now().plusDays(1)).isDue()).isFalse();

        ReminderSchedule sentReminder = reminder(LocalDate.now().minusDays(1));
        sentReminder.markSent();
        assertThat(sentReminder.isDue()).isFalse();
    }

    @Test
    void enumsMatchKbReminderSchemaValues() {
        assertThat(ReminderType.values())
                .containsExactly(ReminderType.PAYMENT_DUE, ReminderType.PRODUCT_EXPIRATION);
        // KB BR-020–BR-022 escalation order: Green (first), Yellow (second), Red (third).
        assertThat(ReminderLevel.values())
                .containsExactly(ReminderLevel.GREEN, ReminderLevel.YELLOW, ReminderLevel.RED);
        assertThat(ReminderLevel.values()[0]).isEqualTo(ReminderLevel.GREEN);
        assertThat(ReminderLevel.values()[1]).isEqualTo(ReminderLevel.YELLOW);
        assertThat(ReminderLevel.values()[2]).isEqualTo(ReminderLevel.RED);
        assertThat(PaymentReminderLevelRules.isFirstReminder(ReminderLevel.GREEN)).isTrue();
        assertThat(PaymentReminderLevelRules.isSecondReminder(ReminderLevel.YELLOW)).isTrue();
        assertThat(PaymentReminderLevelRules.isThirdReminder(ReminderLevel.RED)).isTrue();
        assertThat(ReminderStatus.values())
                .containsExactly(
                        ReminderStatus.PENDING,
                        ReminderStatus.SENT,
                        ReminderStatus.FAILED,
                        ReminderStatus.CANCELLED);
    }

    private static ReminderSchedule reminder(LocalDate scheduledDate) {
        return new ReminderSchedule(
                Customer.create(CustomerType.CUSTOMER, "Mina", "Khan"),
                Product.create(
                        "Education Plan", ProductType.INVESTMENT_FUND, BigDecimal.valueOf(100), 12),
                ReminderType.PAYMENT_DUE,
                ReminderLevel.GREEN,
                scheduledDate);
    }

    private static void assertRelationship(String fieldName, String columnName) throws Exception {
        ManyToOne manyToOne = field(fieldName).getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = field(fieldName).getAnnotation(JoinColumn.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo(columnName);
        assertThat(joinColumn.nullable()).isFalse();
    }

    private static void assertNamedEnum(String fieldName, String columnDefinition)
            throws Exception {
        assertThat(field(fieldName).getAnnotation(Enumerated.class).value())
                .isEqualTo(EnumType.STRING);
        assertThat(field(fieldName).getAnnotation(JdbcTypeCode.class).value())
                .isEqualTo(SqlTypes.NAMED_ENUM);
        assertThat(field(fieldName).getAnnotation(Column.class).columnDefinition())
                .isEqualTo(columnDefinition);
    }

    private static java.lang.reflect.Field field(String name) throws Exception {
        java.lang.reflect.Field field = ReminderSchedule.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
