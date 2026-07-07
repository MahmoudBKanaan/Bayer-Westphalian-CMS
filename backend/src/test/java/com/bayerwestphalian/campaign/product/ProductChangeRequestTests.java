package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

class ProductChangeRequestTests {

    @Test
    void mapsKbProductChangeRequestsTableAsJpaEntity() {
        assertThat(ProductChangeRequest.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(ProductChangeRequest.class.getAnnotation(Table.class).name())
                .isEqualTo("product_change_requests");
        assertThat(BaseEntity.class).isAssignableFrom(ProductChangeRequest.class);
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<ProductChangeRequest> constructor =
                ProductChangeRequest.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsKbProductChangeRequestColumnsAndValidationRules() throws Exception {
        assertColumn("requestType", "request_type", false, 255);
        assertColumn("description", "description", false, 255);
        assertColumn("status", "status", false, 255);

        assertThat(field("product").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("requestType").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("description").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("description").getAnnotation(Column.class).columnDefinition())
                .isEqualTo("text");
        assertThat(field("status").isAnnotationPresent(NotNull.class)).isTrue();
    }

    @Test
    void mapsProductAndRequesterRelationships() throws Exception {
        assertRelationship("product", Product.class, "product_id", false);
        assertRelationship("requestedBy", User.class, "requested_by", true);
    }

    @Test
    void mapsKbPostgreSqlEnums() throws Exception {
        assertNativeEnumColumn("requestType", "product_change_type");
        assertNativeEnumColumn("status", "product_change_status");
    }

    @Test
    void declaresKbProductChangeTypeValues() {
        assertThat(ProductChangeType.values())
                .containsExactly(
                        ProductChangeType.PRICE_CHANGE,
                        ProductChangeType.DURATION_CHANGE,
                        ProductChangeType.EXPIRATION_RULE_CHANGE,
                        ProductChangeType.STATUS_CHANGE);
    }

    @Test
    void declaresKbProductChangeStatusValues() {
        assertThat(ProductChangeStatus.values())
                .containsExactly(
                        ProductChangeStatus.OPEN,
                        ProductChangeStatus.APPROVED,
                        ProductChangeStatus.REJECTED,
                        ProductChangeStatus.IMPLEMENTED);
    }

    @Test
    void createsOpenProductChangeRequestWithKbFields() {
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

        assertThat(request.getProduct()).isSameAs(product);
        assertThat(request.getRequestedBy()).isSameAs(requester);
        assertThat(request.getRequestType()).isEqualTo(ProductChangeType.PRICE_CHANGE);
        assertThat(request.getDescription())
                .isEqualTo("Adjust monthly price for the new tariff.");
        assertThat(request.getStatus()).isEqualTo(ProductChangeStatus.OPEN);
    }

    @Test
    void supportsKbDescriptionAndWorkflowTransitions() {
        ProductChangeRequest request =
                ProductChangeRequest.create(
                        Product.create(
                                "Home Protection",
                                ProductType.HOMEOWNER_INSURANCE,
                                new BigDecimal("89.00"),
                                12),
                        null,
                        ProductChangeType.EXPIRATION_RULE_CHANGE,
                        "Initial expiration policy update.");

        request.updateDescription("Use the updated 6-month expiration reminder policy.");
        request.approve();

        assertThat(request.getRequestedBy()).isNull();
        assertThat(request.getDescription())
                .isEqualTo("Use the updated 6-month expiration reminder policy.");
        assertThat(request.getStatus()).isEqualTo(ProductChangeStatus.APPROVED);

        request.markImplemented();

        assertThat(request.getStatus()).isEqualTo(ProductChangeStatus.IMPLEMENTED);

        request.reject();

        assertThat(request.getStatus()).isEqualTo(ProductChangeStatus.REJECTED);
    }

    private static void assertNativeEnumColumn(String fieldName, String columnDefinition)
            throws Exception {
        Field enumField = field(fieldName);
        Column column = enumField.getAnnotation(Column.class);
        Enumerated enumerated = enumField.getAnnotation(Enumerated.class);
        JdbcTypeCode jdbcTypeCode = enumField.getAnnotation(JdbcTypeCode.class);

        assertThat(column.columnDefinition()).isEqualTo(columnDefinition);
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
        assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.NAMED_ENUM);
    }

    private static void assertRelationship(
            String fieldName, Class<?> relationshipType, String columnName, boolean optional)
            throws Exception {
        Field field = field(fieldName);
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);

        assertThat(field.getType()).isEqualTo(relationshipType);
        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isEqualTo(optional);
        assertThat(joinColumn.name()).isEqualTo(columnName);
        assertThat(joinColumn.nullable()).isEqualTo(optional);
    }

    private static void assertColumn(
            String fieldName, String columnName, boolean nullable, int length) throws Exception {
        Column column = field(fieldName).getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(column.length()).isEqualTo(length);
    }

    private static Field field(String fieldName) throws Exception {
        return ProductChangeRequest.class.getDeclaredField(fieldName);
    }
}
