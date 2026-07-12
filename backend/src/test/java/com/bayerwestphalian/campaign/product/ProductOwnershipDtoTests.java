package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ProductOwnershipDtoTests {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validatesCreateProductOwnershipRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(CreateProductOwnershipRequest.class, "customerId")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreateProductOwnershipRequest.class, "productId")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreateProductOwnershipRequest.class, "startDate")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreateProductOwnershipRequest.class, "policyNumber")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(100);
    }

    @Test
    void validatesUpdateProductOwnershipRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(UpdateProductOwnershipRequest.class, "policyNumber")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(100);
    }

    @Test
    void mapsCreateAndUpdateRequestsToCommands() {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDate startDate = LocalDate.parse("2026-01-15");
        LocalDate expirationDate = LocalDate.parse("2027-01-15");

        CreateProductOwnershipCommand createCommand =
                new CreateProductOwnershipRequest(
                                customerId, productId, startDate, expirationDate, "POL-1000")
                        .toCommand();
        UpdateProductOwnershipCommand updateCommand =
                new UpdateProductOwnershipRequest(LocalDate.parse("2028-06-30"), "POL-1000-REVISED")
                        .toCommand();

        assertThat(createCommand.customerId()).isEqualTo(customerId);
        assertThat(createCommand.productId()).isEqualTo(productId);
        assertThat(createCommand.startDate()).isEqualTo(startDate);
        assertThat(createCommand.expirationDate()).isEqualTo(expirationDate);
        assertThat(createCommand.policyNumber()).isEqualTo("POL-1000");
        assertThat(updateCommand.expirationDate()).isEqualTo(LocalDate.parse("2028-06-30"));
        assertThat(updateCommand.policyNumber()).isEqualTo("POL-1000-REVISED");
    }

    @Test
    void rejectsCreateProductOwnershipRequestWithoutRequiredKbFields() {
        CreateProductOwnershipRequest request =
                new CreateProductOwnershipRequest(null, null, null, null, null);

        Set<String> invalidFields = invalidFields(request);

        assertThat(invalidFields).contains("customerId", "productId", "startDate");
    }

    @Test
    void rejectsInvalidPolicyNumberLength() {
        CreateProductOwnershipRequest createRequest =
                new CreateProductOwnershipRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        LocalDate.now(),
                        null,
                        "X".repeat(101));
        UpdateProductOwnershipRequest updateRequest =
                new UpdateProductOwnershipRequest(null, "X".repeat(101));

        assertThat(invalidFields(createRequest)).contains("policyNumber");
        assertThat(invalidFields(updateRequest)).contains("policyNumber");
    }

    @Test
    void mapsSearchRequestToCriteria() {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDate expiringFrom = LocalDate.parse("2026-07-01");
        LocalDate expiringTo = LocalDate.parse("2026-10-01");

        ProductOwnershipSearchCriteria criteria =
                new ProductOwnershipSearchRequest(
                                customerId,
                                productId,
                                OwnershipStatus.ACTIVE,
                                expiringFrom,
                                expiringTo)
                        .toCriteria();

        assertThat(criteria.customerId()).isEqualTo(customerId);
        assertThat(criteria.productId()).isEqualTo(productId);
        assertThat(criteria.status()).isEqualTo(OwnershipStatus.ACTIVE);
        assertThat(criteria.expiringFrom()).isEqualTo(expiringFrom);
        assertThat(criteria.expiringTo()).isEqualTo(expiringTo);
    }

    @Test
    void mapsProductOwnershipEntityToView() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Owner");
        Product product =
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("129.99"),
                        24);
        LocalDate startDate = LocalDate.parse("2026-01-01");
        LocalDate expirationDate = LocalDate.parse("2027-01-01");

        ProductOwnership ownership =
                ProductOwnership.create(customer, product, startDate, expirationDate);
        ownership.recordPolicyNumber("POL-ACTIVE-001");

        ProductOwnershipView view = ProductOwnershipView.from(ownership);

        assertThat(view.customerId()).isEqualTo(customer.getId());
        assertThat(view.customerFullName()).isEqualTo("Ada Owner");
        assertThat(view.productId()).isEqualTo(product.getId());
        assertThat(view.productName()).isEqualTo("Life Protection");
        assertThat(view.productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(view.policyNumber()).isEqualTo("POL-ACTIVE-001");
        assertThat(view.startDate()).isEqualTo(startDate);
        assertThat(view.expirationDate()).isEqualTo(expirationDate);
        assertThat(view.status()).isEqualTo(OwnershipStatus.ACTIVE);
        assertThat(view.active()).isTrue();
    }

    @Test
    void mapsExpiredProductOwnershipAsInactiveInView() {
        LocalDate startDate = LocalDate.now().minusYears(2);
        LocalDate expirationDate = LocalDate.now().minusMonths(1);
        ProductOwnership ownership =
                ProductOwnership.create(
                        Customer.create(CustomerType.CUSTOMER, "Ben", "Owner"),
                        Product.create(
                                "Home Protection",
                                ProductType.HOMEOWNER_INSURANCE,
                                new BigDecimal("89.00"),
                                12),
                        startDate,
                        expirationDate);
        ownership.expire();

        ProductOwnershipView view = ProductOwnershipView.from(ownership);

        assertThat(view.status()).isEqualTo(OwnershipStatus.EXPIRED);
        assertThat(view.active()).isFalse();
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
