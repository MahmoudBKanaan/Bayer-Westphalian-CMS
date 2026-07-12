package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.support.ControllerTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ReminderDtoTests {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private static final UUID CUSTOMER_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000372");
    private static final UUID PRODUCT_ID =
            UUID.fromString("41000000-0000-0000-0000-000000000372");

    @Test
    void validatesPaymentReminderRequestFieldsFromKb() throws Exception {
        assertRequired(CreatePaymentReminderRequest.class, "customerId");
        assertRequired(CreatePaymentReminderRequest.class, "productId");
        assertRequired(CreatePaymentReminderRequest.class, "reminderLevel");
        assertRequired(CreatePaymentReminderRequest.class, "scheduledDate");
    }

    @Test
    void validatesProductExpirationReminderRequestFieldsFromKb() throws Exception {
        assertRequired(CreateProductExpirationReminderRequest.class, "customerId");
        assertRequired(CreateProductExpirationReminderRequest.class, "productId");
        assertRequired(CreateProductExpirationReminderRequest.class, "reminderLevel");
        assertRequired(CreateProductExpirationReminderRequest.class, "scheduledDate");
    }

    @Test
    void mapsCreateRequestsToReminderScheduleCommands() {
        LocalDate scheduledDate = LocalDate.parse("2026-09-15");

        ReminderScheduleCommand paymentCommand =
                new CreatePaymentReminderRequest(
                                CUSTOMER_ID, PRODUCT_ID, ReminderLevel.YELLOW, scheduledDate)
                        .toCommand();
        ReminderScheduleCommand expirationCommand =
                new CreateProductExpirationReminderRequest(
                                CUSTOMER_ID, PRODUCT_ID, ReminderLevel.GREEN, scheduledDate)
                        .toCommand();

        assertThat(paymentCommand.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(paymentCommand.productId()).isEqualTo(PRODUCT_ID);
        assertThat(paymentCommand.reminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(paymentCommand.reminderLevel()).isEqualTo(ReminderLevel.YELLOW);
        assertThat(paymentCommand.scheduledDate()).isEqualTo(scheduledDate);

        assertThat(expirationCommand.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(expirationCommand.productId()).isEqualTo(PRODUCT_ID);
        assertThat(expirationCommand.reminderType()).isEqualTo(ReminderType.PRODUCT_EXPIRATION);
        assertThat(expirationCommand.reminderLevel()).isEqualTo(ReminderLevel.GREEN);
        assertThat(expirationCommand.scheduledDate()).isEqualTo(scheduledDate);
    }

    @Test
    void rejectsCreateReminderRequestsWithoutRequiredKbFields() {
        CreatePaymentReminderRequest paymentRequest =
                new CreatePaymentReminderRequest(null, null, null, null);
        CreateProductExpirationReminderRequest expirationRequest =
                new CreateProductExpirationReminderRequest(null, null, null, null);

        assertThat(invalidFields(paymentRequest))
                .contains("customerId", "productId", "reminderLevel", "scheduledDate");
        assertThat(invalidFields(expirationRequest))
                .contains("customerId", "productId", "reminderLevel", "scheduledDate");
    }

    @Test
    void mapsSearchRequestToCriteria() {
        LocalDate dueOnOrBefore = LocalDate.parse("2026-09-30");

        ReminderScheduleSearchCriteria criteria =
                new ReminderScheduleSearchRequest(
                                CUSTOMER_ID, ReminderStatus.PENDING, dueOnOrBefore)
                        .toCriteria();

        assertThat(criteria.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(criteria.status()).isEqualTo(ReminderStatus.PENDING);
        assertThat(criteria.dueOnOrBefore()).isEqualTo(dueOnOrBefore);
    }

    @Test
    void mapsReminderScheduleEntityToView() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Reminder");
        ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);
        Product product =
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        BigDecimal.valueOf(100),
                        12);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        ReminderSchedule reminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PAYMENT_DUE,
                        ReminderLevel.RED,
                        LocalDate.now());
        UUID reminderId = UUID.fromString("96000000-0000-0000-0000-000000000372");
        Instant createdAt = Instant.parse("2026-07-10T12:00:00Z");
        ReflectionTestUtils.setField(reminder, "id", reminderId);
        ReflectionTestUtils.setField(reminder, "createdAt", createdAt);

        ReminderScheduleView view = ReminderScheduleView.from(reminder);

        assertThat(view.id()).isEqualTo(reminderId);
        assertThat(view.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(view.customerFullName()).isEqualTo("Ada Reminder");
        assertThat(view.productId()).isEqualTo(PRODUCT_ID);
        assertThat(view.productName()).isEqualTo("Life Protection");
        assertThat(view.productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(view.reminderType()).isEqualTo(ReminderType.PAYMENT_DUE);
        assertThat(view.reminderLevel()).isEqualTo(ReminderLevel.RED);
        assertThat(view.scheduledDate()).isEqualTo(LocalDate.now());
        assertThat(view.status()).isEqualTo(ReminderStatus.PENDING);
        assertThat(view.createdAt()).isEqualTo(createdAt);
        assertThat(view.sentAt()).isNull();
        assertThat(view.due()).isTrue();
    }

    @Test
    void serializesReminderScheduleViewWithIsoDatesAndEnums() throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Reminder");
        Product product =
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        BigDecimal.valueOf(100),
                        12);
        ReminderSchedule reminder =
                new ReminderSchedule(
                        customer,
                        product,
                        ReminderType.PRODUCT_EXPIRATION,
                        ReminderLevel.GREEN,
                        LocalDate.parse("2026-09-15"));
        ReflectionTestUtils.setField(reminder, "createdAt", Instant.parse("2026-07-10T12:00:00Z"));
        reminder.markSent();

        JsonNode json =
                ControllerTestSupport.apiObjectMapper()
                        .valueToTree(ReminderScheduleView.from(reminder));

        assertThat(json.get("reminderType").asText()).isEqualTo("PRODUCT_EXPIRATION");
        assertThat(json.get("reminderLevel").asText()).isEqualTo("GREEN");
        assertThat(json.get("scheduledDate").asText()).isEqualTo("2026-09-15");
        assertThat(json.get("status").asText()).isEqualTo("SENT");
        assertThat(json.get("createdAt").asText()).isEqualTo("2026-07-10T12:00:00Z");
        assertThat(json.get("sentAt").asText()).isNotBlank();
        assertThat(json.get("due").asBoolean()).isFalse();
    }

    private static void assertRequired(Class<?> type, String fieldName) throws Exception {
        assertThat(field(type, fieldName).isAnnotationPresent(NotNull.class)).isTrue();
    }

    private static Field field(Class<?> type, String fieldName) throws Exception {
        return type.getDeclaredField(fieldName);
    }

    private static Set<String> invalidFields(Object request) {
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
