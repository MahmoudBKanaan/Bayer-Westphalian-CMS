package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.support.ControllerTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PaymentRecordDtoTests {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validatesCreatePaymentRecordRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(CreatePaymentRecordRequest.class, "customerId")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreatePaymentRecordRequest.class, "productOwnershipId")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreatePaymentRecordRequest.class, "dueDate")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreatePaymentRecordRequest.class, "amountDue")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreatePaymentRecordRequest.class, "amountDue")
                                .getAnnotation(DecimalMin.class)
                                .value())
                .isEqualTo("0.00");
        assertThat(
                        field(CreatePaymentRecordRequest.class, "amountDue")
                                .getAnnotation(Digits.class)
                                .integer())
                .isEqualTo(10);
        assertThat(
                        field(CreatePaymentRecordRequest.class, "amountDue")
                                .getAnnotation(Digits.class)
                                .fraction())
                .isEqualTo(2);
    }

    @Test
    void validatesMarkPaymentPaidRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(MarkPaymentPaidRequest.class, "amountPaid")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(MarkPaymentPaidRequest.class, "amountPaid")
                                .getAnnotation(DecimalMin.class)
                                .value())
                .isEqualTo("0.00");
        assertThat(
                        field(MarkPaymentPaidRequest.class, "amountPaid")
                                .getAnnotation(Digits.class)
                                .integer())
                .isEqualTo(10);
        assertThat(
                        field(MarkPaymentPaidRequest.class, "amountPaid")
                                .getAnnotation(Digits.class)
                                .fraction())
                .isEqualTo(2);
        assertThat(field(MarkPaymentPaidRequest.class, "paidAt").isAnnotationPresent(NotNull.class))
                .isFalse();
    }

    @Test
    void mapsCreateAndMarkPaidRequestsToCommands() {
        UUID customerId = UUID.randomUUID();
        UUID ownershipId = UUID.randomUUID();
        LocalDate dueDate = LocalDate.parse("2026-07-15");
        Instant paidAt = Instant.parse("2026-07-10T09:30:00Z");

        CreatePaymentRecordCommand createCommand =
                new CreatePaymentRecordRequest(
                                customerId, ownershipId, dueDate, new BigDecimal("129.99"))
                        .toCommand();
        MarkPaymentPaidCommand markPaidCommand =
                new MarkPaymentPaidRequest(new BigDecimal("100.00"), paidAt).toCommand();
        MarkPaymentPaidCommand markPaidWithoutTimestampCommand =
                new MarkPaymentPaidRequest(new BigDecimal("50.00"), null).toCommand();

        assertThat(createCommand.customerId()).isEqualTo(customerId);
        assertThat(createCommand.productOwnershipId()).isEqualTo(ownershipId);
        assertThat(createCommand.dueDate()).isEqualTo(dueDate);
        assertThat(createCommand.amountDue()).isEqualByComparingTo("129.99");
        assertThat(markPaidCommand.amountPaid()).isEqualByComparingTo("100.00");
        assertThat(markPaidCommand.paidAt()).isEqualTo(paidAt);
        assertThat(markPaidWithoutTimestampCommand.amountPaid()).isEqualByComparingTo("50.00");
        assertThat(markPaidWithoutTimestampCommand.paidAt()).isNull();
    }

    @Test
    void rejectsCreatePaymentRecordRequestWithoutRequiredKbFields() {
        CreatePaymentRecordRequest request = new CreatePaymentRecordRequest(null, null, null, null);

        Set<String> invalidFields = invalidFields(request);

        assertThat(invalidFields)
                .contains("customerId", "productOwnershipId", "dueDate", "amountDue");
    }

    @Test
    void rejectsInvalidPaymentAmountsOnCreateAndMarkPaidRequests() {
        CreatePaymentRecordRequest createRequest =
                new CreatePaymentRecordRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        LocalDate.now(),
                        new BigDecimal("-1.00"));
        MarkPaymentPaidRequest markPaidRequest =
                new MarkPaymentPaidRequest(new BigDecimal("-0.01"), null);

        assertThat(invalidFields(createRequest)).contains("amountDue");
        assertThat(invalidFields(markPaidRequest)).contains("amountPaid");
    }

    @Test
    void rejectsMarkPaymentPaidRequestWithoutAmountPaid() {
        MarkPaymentPaidRequest request = new MarkPaymentPaidRequest(null, null);

        Set<String> invalidFields = invalidFields(request);

        assertThat(invalidFields).contains("amountPaid");
    }

    @Test
    void mapsSearchRequestToCriteria() {
        UUID customerId = UUID.randomUUID();

        PaymentRecordSearchCriteria criteria =
                new PaymentRecordSearchRequest(customerId, PaymentStatus.OVERDUE).toCriteria();

        assertThat(criteria.customerId()).isEqualTo(customerId);
        assertThat(criteria.status()).isEqualTo(PaymentStatus.OVERDUE);
    }

    @Test
    void mapsPaymentRecordEntityToView() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Payer");
        Product product =
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("129.99"),
                        24);
        ProductOwnership ownership =
                ProductOwnership.create(
                        customer,
                        product,
                        LocalDate.parse("2026-01-01"),
                        LocalDate.parse("2027-01-01"));
        ownership.recordPolicyNumber("POL-1000");
        LocalDate dueDate = LocalDate.parse("2026-07-15");
        Instant paidAt = Instant.parse("2026-07-10T09:30:00Z");

        PaymentRecord payment =
                PaymentRecord.create(customer, ownership, dueDate, new BigDecimal("129.99"));
        payment.markPaid(new BigDecimal("129.99"), paidAt);

        PaymentRecordView view = PaymentRecordView.from(payment);

        assertThat(view.customerId()).isEqualTo(customer.getId());
        assertThat(view.customerFullName()).isEqualTo("Ada Payer");
        assertThat(view.productOwnershipId()).isEqualTo(ownership.getId());
        assertThat(view.productId()).isEqualTo(product.getId());
        assertThat(view.productName()).isEqualTo("Life Protection");
        assertThat(view.productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(view.dueDate()).isEqualTo(dueDate);
        assertThat(view.paidAt()).isEqualTo(paidAt);
        assertThat(view.amountDue()).isEqualByComparingTo("129.99");
        assertThat(view.amountPaid()).isEqualByComparingTo("129.99");
        assertThat(view.status()).isEqualTo(PaymentStatus.PAID);
        assertThat(view.reminderCount()).isZero();
        assertThat(view.defaultRisk()).isFalse();
    }

    @Test
    void serializesPaymentRecordViewWithIsoDatesAndNumericAmounts() throws Exception {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Payer");
        Product product =
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("129.99"),
                        24);
        ProductOwnership ownership =
                ProductOwnership.create(
                        customer,
                        product,
                        LocalDate.parse("2026-01-01"),
                        LocalDate.parse("2027-01-01"));
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        ownership,
                        LocalDate.parse("2026-07-15"),
                        new BigDecimal("129.99"));
        payment.markPaid(new BigDecimal("129.99"), Instant.parse("2026-07-10T09:30:00Z"));

        JsonNode json =
                ControllerTestSupport.apiObjectMapper()
                        .valueToTree(PaymentRecordView.from(payment));

        assertThat(json.get("dueDate").asText()).isEqualTo("2026-07-15");
        assertThat(json.get("paidAt").asText()).isEqualTo("2026-07-10T09:30:00Z");
        assertThat(json.get("amountDue").decimalValue()).isEqualByComparingTo("129.99");
        assertThat(json.get("amountPaid").decimalValue()).isEqualByComparingTo("129.99");
        assertThat(json.get("status").asText()).isEqualTo("PAID");
        assertThat(json.get("defaultRisk").asBoolean()).isFalse();
    }

    @Test
    void mapsOverduePaymentRecordWithDefaultRiskToView() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ben", "Payer");
        PaymentRecord payment =
                PaymentRecord.create(
                        customer,
                        ProductOwnership.create(
                                customer,
                                Product.create(
                                        "Home Protection",
                                        ProductType.HOMEOWNER_INSURANCE,
                                        new BigDecimal("89.00"),
                                        12),
                                LocalDate.now().minusYears(1),
                                LocalDate.now().plusMonths(6)),
                        LocalDate.now().minusDays(10),
                        new BigDecimal("89.00"));
        payment.markOverdue();
        payment.incrementReminder();
        payment.incrementReminder();
        payment.incrementReminder();

        PaymentRecordView view = PaymentRecordView.from(payment);

        assertThat(view.status()).isEqualTo(PaymentStatus.DEFAULT_RISK);
        assertThat(view.reminderCount()).isEqualTo(3);
        assertThat(view.defaultRisk()).isTrue();
        assertThat(view.daysOverdue()).isGreaterThanOrEqualTo(10);
        assertThat(view.paidAt()).isNull();
        assertThat(view.amountPaid()).isNull();
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
