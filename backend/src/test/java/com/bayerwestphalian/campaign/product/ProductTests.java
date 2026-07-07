package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.common.domain.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

class ProductTests {

    @Test
    void mapsKbProductsTableAsJpaEntity() {
        assertThat(Product.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(Product.class.getAnnotation(Table.class).name()).isEqualTo("products");
        assertThat(SoftDeletableEntity.class).isAssignableFrom(Product.class);
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<Product> constructor = Product.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsKbProductColumnsAndValidationRules() throws Exception {
        assertColumn("name", "name", false, 255);
        assertColumn("productType", "product_type", false, 255);
        assertColumn("description", "description", true, 255);
        assertColumn("price", "price", true, 255);
        assertColumn("durationMonths", "duration_months", true, 255);
        assertColumn("expirationPolicy", "expiration_policy", true, 100);
        assertColumn("active", "active", false, 255);

        assertThat(field("name").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("name").getAnnotation(Size.class).max()).isEqualTo(255);
        assertThat(field("productType").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("price").getAnnotation(DecimalMin.class).value()).isEqualTo("0.00");
        assertThat(field("price").getAnnotation(Digits.class).integer()).isEqualTo(10);
        assertThat(field("price").getAnnotation(Digits.class).fraction()).isEqualTo(2);
        assertThat(field("durationMonths").isAnnotationPresent(Positive.class)).isTrue();
        assertThat(field("expirationPolicy").getAnnotation(Size.class).max()).isEqualTo(100);
    }

    @Test
    void mapsProductTypeToKbPostgreSqlEnum() throws Exception {
        Field productType = field("productType");
        Column column = productType.getAnnotation(Column.class);
        Enumerated enumerated = productType.getAnnotation(Enumerated.class);
        JdbcTypeCode jdbcTypeCode = productType.getAnnotation(JdbcTypeCode.class);

        assertThat(column.columnDefinition()).isEqualTo("product_type");
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
        assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.NAMED_ENUM);
    }

    @Test
    void declaresKbProductTypeValues() {
        assertThat(ProductType.values())
                .containsExactly(
                        ProductType.HOMEOWNER_INSURANCE,
                        ProductType.LIFE_INSURANCE,
                        ProductType.INVESTMENT_FUND,
                        ProductType.HEALTH_INSURANCE,
                        ProductType.AUTO_INSURANCE,
                        ProductType.OTHER);
    }

    @Test
    void createsActiveProductWithKbFields() {
        Product product =
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("129.99"),
                        24);

        assertThat(product.getName()).isEqualTo("Life Protection");
        assertThat(product.getProductType()).isEqualTo(ProductType.LIFE_INSURANCE);
        assertThat(product.getPrice()).isEqualByComparingTo("129.99");
        assertThat(product.getDurationMonths()).isEqualTo(24);
        assertThat(product.isActive()).isTrue();
        assertThat(product.isDeleted()).isFalse();
    }

    @Test
    void supportsKbProductDetailPricingStatusAndSoftDeleteLifecycle() {
        Product product =
                Product.create(
                        "Starter Cover",
                        ProductType.HOMEOWNER_INSURANCE,
                        new BigDecimal("59.00"),
                        12);

        product.updateDetails(
                "Starter Cover Plus",
                ProductType.HEALTH_INSURANCE,
                "Expanded health protection",
                18,
                "EXPIRES_AT_TERM_END");
        product.updatePricing(new BigDecimal("79.50"));
        product.deactivate();

        assertThat(product.getName()).isEqualTo("Starter Cover Plus");
        assertThat(product.getProductType()).isEqualTo(ProductType.HEALTH_INSURANCE);
        assertThat(product.getDescription()).isEqualTo("Expanded health protection");
        assertThat(product.getDurationMonths()).isEqualTo(18);
        assertThat(product.getExpirationPolicy()).isEqualTo("EXPIRES_AT_TERM_END");
        assertThat(product.getPrice()).isEqualByComparingTo("79.50");
        assertThat(product.isActive()).isFalse();

        product.activate();
        product.softDelete();

        assertThat(product.isDeleted()).isTrue();
        assertThat(product.isActive()).isFalse();
    }

    private static void assertColumn(
            String fieldName, String columnName, boolean nullable, int length) throws Exception {
        Column column = field(fieldName).getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(column.length()).isEqualTo(length);
    }

    private static Field field(String fieldName) throws Exception {
        return Product.class.getDeclaredField(fieldName);
    }
}
