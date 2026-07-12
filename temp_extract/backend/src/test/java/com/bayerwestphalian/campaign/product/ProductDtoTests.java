package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ProductDtoTests {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validatesCreateProductRequestFieldsFromKb() throws Exception {
        assertThat(field(CreateProductRequest.class, "name").isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(CreateProductRequest.class, "name").getAnnotation(Size.class).max())
                .isEqualTo(255);
        assertThat(
                        field(CreateProductRequest.class, "productType")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(field(CreateProductRequest.class, "price").getAnnotation(DecimalMin.class).value())
                .isEqualTo("0.00");
        assertThat(field(CreateProductRequest.class, "price").getAnnotation(Digits.class).integer())
                .isEqualTo(10);
        assertThat(field(CreateProductRequest.class, "price").getAnnotation(Digits.class).fraction())
                .isEqualTo(2);
        assertThat(
                        field(CreateProductRequest.class, "durationMonths")
                                .isAnnotationPresent(Positive.class))
                .isTrue();
        assertThat(
                        field(CreateProductRequest.class, "expirationPolicy")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(100);
    }

    @Test
    void validatesUpdateAndSearchProductRequestFieldsFromKb() throws Exception {
        assertThat(field(UpdateProductRequest.class, "name").isAnnotationPresent(NotBlank.class))
                .isTrue();
        assertThat(field(UpdateProductRequest.class, "name").getAnnotation(Size.class).max())
                .isEqualTo(255);
        assertThat(
                        field(UpdateProductRequest.class, "productType")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(field(UpdateProductRequest.class, "price").getAnnotation(DecimalMin.class).value())
                .isEqualTo("0.00");
        assertThat(field(UpdateProductRequest.class, "price").getAnnotation(Digits.class).integer())
                .isEqualTo(10);
        assertThat(field(UpdateProductRequest.class, "price").getAnnotation(Digits.class).fraction())
                .isEqualTo(2);
        assertThat(
                        field(UpdateProductRequest.class, "durationMonths")
                                .isAnnotationPresent(Positive.class))
                .isTrue();
        assertThat(
                        field(UpdateProductRequest.class, "expirationPolicy")
                                .getAnnotation(Size.class)
                                .max())
                .isEqualTo(100);

        assertThat(field(ProductSearchRequest.class, "term").getAnnotation(Size.class).max())
                .isEqualTo(255);
    }

    @Test
    void mapsCreateAndUpdateRequestsToCommands() {
        CreateProductCommand createCommand =
                new CreateProductRequest(
                                "Life Protection",
                                ProductType.LIFE_INSURANCE,
                                "Coverage for beneficiaries",
                                new BigDecimal("129.99"),
                                24,
                                "EXPIRES_AT_TERM_END")
                        .toCommand();
        UpdateProductCommand updateCommand =
                new UpdateProductRequest(
                                "Life Protection Plus",
                                ProductType.LIFE_INSURANCE,
                                "Expanded beneficiary coverage",
                                new BigDecimal("149.50"),
                                36,
                                "AUTO_RENEW",
                                false)
                        .toCommand();

        assertThat(createCommand.name()).isEqualTo("Life Protection");
        assertThat(createCommand.productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(createCommand.description()).isEqualTo("Coverage for beneficiaries");
        assertThat(createCommand.price()).isEqualByComparingTo("129.99");
        assertThat(createCommand.durationMonths()).isEqualTo(24);
        assertThat(createCommand.expirationPolicy()).isEqualTo("EXPIRES_AT_TERM_END");
        assertThat(updateCommand.name()).isEqualTo("Life Protection Plus");
        assertThat(updateCommand.description()).isEqualTo("Expanded beneficiary coverage");
        assertThat(updateCommand.price()).isEqualByComparingTo("149.50");
        assertThat(updateCommand.durationMonths()).isEqualTo(36);
        assertThat(updateCommand.expirationPolicy()).isEqualTo("AUTO_RENEW");
        assertThat(updateCommand.active()).isFalse();
    }

    @Test
    void rejectsCreateProductRequestWithoutRequiredKbFields() {
        CreateProductRequest request =
                new CreateProductRequest(" ", null, null, null, null, null);

        Set<String> invalidFields = invalidFields(request);

        assertThat(invalidFields).contains("name", "productType");
    }

    @Test
    void rejectsUpdateProductRequestWithoutRequiredKbFields() {
        UpdateProductRequest request =
                new UpdateProductRequest(" ", null, null, null, null, null, null);

        Set<String> invalidFields = invalidFields(request);

        assertThat(invalidFields).contains("name", "productType");
    }

    @Test
    void rejectsInvalidProductPricingAndDurationValues() {
        CreateProductRequest createRequest =
                new CreateProductRequest(
                        "Starter Cover",
                        ProductType.HOMEOWNER_INSURANCE,
                        null,
                        new BigDecimal("-1.00"),
                        0,
                        "X".repeat(101));
        UpdateProductRequest updateRequest =
                new UpdateProductRequest(
                        "Starter Cover",
                        ProductType.HOMEOWNER_INSURANCE,
                        null,
                        new BigDecimal("-5.00"),
                        -12,
                        "X".repeat(101),
                        true);

        assertThat(invalidFields(createRequest))
                .contains("price", "durationMonths", "expirationPolicy");
        assertThat(invalidFields(updateRequest))
                .contains("price", "durationMonths", "expirationPolicy");
    }

    @Test
    void normalizesProductSearchCriteriaForRepositoryFilters() {
        ProductSearchCriteria criteria =
                new ProductSearchRequest("  life insurance  ", ProductType.LIFE_INSURANCE, true)
                        .toCriteria();
        ProductSearchCriteria blankCriteria =
                new ProductSearchRequest("   ", null, null).toCriteria();

        assertThat(criteria.term()).isEqualTo("life insurance");
        assertThat(criteria.productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(criteria.active()).isTrue();
        assertThat(blankCriteria.term()).isNull();
        assertThat(blankCriteria.productType()).isNull();
        assertThat(blankCriteria.active()).isNull();
    }

    @Test
    void mapsProductEntityToView() {
        Product product =
                Product.create(
                        "Investment Growth Fund",
                        ProductType.INVESTMENT_FUND,
                        new BigDecimal("250.00"),
                        12);
        product.updateDetails(
                "Investment Growth Fund",
                ProductType.INVESTMENT_FUND,
                "Long-term investment product",
                18,
                "EXPIRES_AT_TERM_END");
        product.deactivate();
        product.softDelete();

        ProductView view = ProductView.from(product);

        assertThat(view.name()).isEqualTo("Investment Growth Fund");
        assertThat(view.productType()).isEqualTo(ProductType.INVESTMENT_FUND);
        assertThat(view.description()).isEqualTo("Long-term investment product");
        assertThat(view.price()).isEqualByComparingTo("250.00");
        assertThat(view.durationMonths()).isEqualTo(18);
        assertThat(view.expirationPolicy()).isEqualTo("EXPIRES_AT_TERM_END");
        assertThat(view.active()).isFalse();
        assertThat(view.deleted()).isTrue();
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