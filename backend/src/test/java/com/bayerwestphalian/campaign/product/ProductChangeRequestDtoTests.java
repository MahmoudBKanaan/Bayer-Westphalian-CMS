package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ProductChangeRequestDtoTests {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validatesCreateProductChangeRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(CreateProductChangeRequestRequest.class, "productId")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreateProductChangeRequestRequest.class, "requestType")
                                .isAnnotationPresent(NotNull.class))
                .isTrue();
        assertThat(
                        field(CreateProductChangeRequestRequest.class, "description")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
    }

    @Test
    void validatesUpdateProductChangeRequestFieldsFromKb() throws Exception {
        assertThat(
                        field(UpdateProductChangeRequestRequest.class, "description")
                                .isAnnotationPresent(NotBlank.class))
                .isTrue();
    }

    @Test
    void mapsCreateAndUpdateRequestsToCommands() {
        UUID productId = UUID.randomUUID();

        CreateProductChangeRequestCommand createCommand =
                new CreateProductChangeRequestRequest(
                                productId,
                                ProductChangeType.PRICE_CHANGE,
                                "Adjust monthly price for the new tariff.")
                        .toCommand();
        UpdateProductChangeRequestCommand updateCommand =
                new UpdateProductChangeRequestRequest(
                                "Use the updated 6-month expiration reminder policy.")
                        .toCommand();

        assertThat(createCommand.productId()).isEqualTo(productId);
        assertThat(createCommand.requestType()).isEqualTo(ProductChangeType.PRICE_CHANGE);
        assertThat(createCommand.description())
                .isEqualTo("Adjust monthly price for the new tariff.");
        assertThat(updateCommand.description())
                .isEqualTo("Use the updated 6-month expiration reminder policy.");
    }

    @Test
    void rejectsCreateProductChangeRequestWithoutRequiredKbFields() {
        CreateProductChangeRequestRequest request =
                new CreateProductChangeRequestRequest(null, null, " ");

        Set<String> invalidFields = invalidFields(request);

        assertThat(invalidFields).contains("productId", "requestType", "description");
    }

    @Test
    void rejectsUpdateProductChangeRequestWithoutDescription() {
        UpdateProductChangeRequestRequest request = new UpdateProductChangeRequestRequest(" ");

        Set<String> invalidFields = invalidFields(request);

        assertThat(invalidFields).contains("description");
    }

    @Test
    void mapsSearchRequestToCriteria() {
        UUID productId = UUID.randomUUID();

        ProductChangeRequestSearchCriteria criteria =
                new ProductChangeRequestSearchRequest(productId, ProductChangeStatus.OPEN)
                        .toCriteria();

        assertThat(criteria.productId()).isEqualTo(productId);
        assertThat(criteria.status()).isEqualTo(ProductChangeStatus.OPEN);
    }

    @Test
    void mapsProductChangeRequestEntityToView() {
        Product product =
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("129.99"),
                        24);
        User requester =
                User.create(
                        "product.manager@bayer-westphalian.test",
                        "$2a$10$product-manager",
                        "Product Manager");
        ProductChangeRequest request =
                ProductChangeRequest.create(
                        product,
                        requester,
                        ProductChangeType.PRICE_CHANGE,
                        "Adjust monthly price for the new tariff.");
        request.approve();

        ProductChangeRequestView view = ProductChangeRequestView.from(request);

        assertThat(view.productId()).isEqualTo(product.getId());
        assertThat(view.productName()).isEqualTo("Life Protection");
        assertThat(view.productType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(view.requestedByUserId()).isEqualTo(requester.getId());
        assertThat(view.requestedByFullName()).isEqualTo("Product Manager");
        assertThat(view.requestType()).isEqualTo(ProductChangeType.PRICE_CHANGE);
        assertThat(view.description()).isEqualTo("Adjust monthly price for the new tariff.");
        assertThat(view.status()).isEqualTo(ProductChangeStatus.APPROVED);
    }

    @Test
    void mapsProductChangeRequestWithoutRequesterToView() {
        Product product =
                Product.create(
                        "Home Protection",
                        ProductType.HOMEOWNER_INSURANCE,
                        new BigDecimal("89.00"),
                        12);
        ProductChangeRequest request =
                ProductChangeRequest.create(
                        product,
                        null,
                        ProductChangeType.EXPIRATION_RULE_CHANGE,
                        "Initial expiration policy update.");

        ProductChangeRequestView view = ProductChangeRequestView.from(request);

        assertThat(view.productName()).isEqualTo("Home Protection");
        assertThat(view.requestedByUserId()).isNull();
        assertThat(view.requestedByFullName()).isNull();
        assertThat(view.requestType()).isEqualTo(ProductChangeType.EXPIRATION_RULE_CHANGE);
        assertThat(view.status()).isEqualTo(ProductChangeStatus.OPEN);
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
