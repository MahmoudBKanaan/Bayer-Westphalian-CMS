package com.bayerwestphalian.campaign.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

class ProductOwnershipTests {

    @Test
    void mapsKbProductOwnershipsTableAsJpaEntity() {
        Table table = ProductOwnership.class.getAnnotation(Table.class);

        assertThat(ProductOwnership.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(table.name()).isEqualTo("product_ownerships");
        assertThat(table.uniqueConstraints())
                .extracting(UniqueConstraint::name)
                .contains("product_ownerships_policy_number_unique");
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<ProductOwnership> constructor =
                ProductOwnership.class.getDeclaredConstructor();

        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsKbProductOwnershipColumnsAndValidationRules() throws Exception {
        assertColumn("id", "id", false, 255);
        assertColumn("policyNumber", "policy_number", true, 100);
        assertColumn("startDate", "start_date", false, 255);
        assertColumn("expirationDate", "expiration_date", true, 255);
        assertColumn("status", "status", false, 255);
        assertColumn("createdAt", "created_at", false, 255);

        assertThat(field("customer").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("product").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("policyNumber").getAnnotation(Size.class).max()).isEqualTo(100);
        assertThat(field("policyNumber").getAnnotation(Column.class).unique()).isTrue();
        assertThat(field("startDate").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("status").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("id").getAnnotation(Column.class).updatable()).isFalse();
        assertThat(field("createdAt").getAnnotation(Column.class).updatable()).isFalse();
    }

    @Test
    void mapsCustomerAndProductRelationships() throws Exception {
        assertRelationship("customer", Customer.class, "customer_id");
        assertRelationship("product", Product.class, "product_id");
    }

    @Test
    void mapsOwnershipStatusToKbPostgreSqlEnum() throws Exception {
        Field status = field("status");
        Column column = status.getAnnotation(Column.class);
        Enumerated enumerated = status.getAnnotation(Enumerated.class);
        JdbcTypeCode jdbcTypeCode = status.getAnnotation(JdbcTypeCode.class);

        assertThat(column.columnDefinition()).isEqualTo("ownership_status");
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
        assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.NAMED_ENUM);
    }

    @Test
    void declaresKbOwnershipStatusValues() {
        assertThat(OwnershipStatus.values())
                .containsExactly(
                        OwnershipStatus.ACTIVE,
                        OwnershipStatus.EXPIRED,
                        OwnershipStatus.CANCELLED);
    }

    @Test
    void createsActiveProductOwnershipWithKbFields() {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Owner");
        Product product =
                Product.create(
                        "Life Protection",
                        ProductType.LIFE_INSURANCE,
                        new BigDecimal("129.99"),
                        24);
        LocalDate startDate = LocalDate.now();
        LocalDate expirationDate = startDate.plusMonths(6);

        ProductOwnership ownership =
                ProductOwnership.create(customer, product, startDate, expirationDate);
        ownership.recordPolicyNumber("POL-1000");

        assertThat(ownership.getCustomer()).isSameAs(customer);
        assertThat(ownership.getProduct()).isSameAs(product);
        assertThat(ownership.getPolicyNumber()).isEqualTo("POL-1000");
        assertThat(ownership.getStartDate()).isEqualTo(startDate);
        assertThat(ownership.getExpirationDate()).isEqualTo(expirationDate);
        assertThat(ownership.getStatus()).isEqualTo(OwnershipStatus.ACTIVE);
        assertThat(ownership.isActive()).isTrue();
    }

    @Test
    void supportsKbExpirationWindowExpireAndCancelLifecycle() {
        LocalDate startDate = LocalDate.now();
        ProductOwnership ownership =
                ProductOwnership.create(
                        Customer.create(CustomerType.CUSTOMER, "Ben", "Owner"),
                        Product.create(
                                "Home Protection",
                                ProductType.HOMEOWNER_INSURANCE,
                                new BigDecimal("89.00"),
                                12),
                        startDate,
                        startDate.plusMonths(3));

        assertThat(ownership.isExpiringWithinMonths(2)).isFalse();
        assertThat(ownership.isExpiringWithinMonths(3)).isTrue();

        ownership.expire();

        assertThat(ownership.getStatus()).isEqualTo(OwnershipStatus.EXPIRED);
        assertThat(ownership.isActive()).isFalse();
        assertThat(ownership.isExpiringWithinMonths(3)).isFalse();

        ownership.cancel();

        assertThat(ownership.getStatus()).isEqualTo(OwnershipStatus.CANCELLED);
        assertThat(ownership.isActive()).isFalse();
    }

    @Test
    void updatesSavedExpirationDateForKbExpirationCampaignSupport() {
        LocalDate startDate = LocalDate.parse("2026-01-01");
        LocalDate initialExpiration = startDate.plusMonths(6);
        ProductOwnership ownership =
                ProductOwnership.create(
                        Customer.create(CustomerType.CUSTOMER, "Eva", "Owner"),
                        Product.create(
                                "Life Protection",
                                ProductType.LIFE_INSURANCE,
                                new BigDecimal("129.99"),
                                24),
                        startDate,
                        initialExpiration);

        assertThat(ownership.getExpirationDate()).isEqualTo(initialExpiration);

        LocalDate revisedExpiration = LocalDate.parse("2028-12-31");
        ownership.updateExpirationDate(revisedExpiration);

        assertThat(ownership.getExpirationDate()).isEqualTo(revisedExpiration);
    }

    @Test
    void rejectsExpirationDateBeforeStartDateToMatchKbDatabaseConstraint() {
        LocalDate startDate = LocalDate.parse("2026-01-01");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(
                        () ->
                                ProductOwnership.create(
                                        Customer.create(CustomerType.CUSTOMER, "Clara", "Owner"),
                                        Product.create(
                                                "Investment Fund",
                                                ProductType.INVESTMENT_FUND,
                                                new BigDecimal("500.00"),
                                                36),
                                        startDate,
                                        startDate.minusDays(1)))
                .withMessageContaining("Expiration date must be on or after start date");
    }

    @Test
    void prePersistCreatesIdAndCreatedAtForKbCreatedAtColumn() throws Exception {
        ProductOwnership ownership =
                ProductOwnership.create(
                        Customer.create(CustomerType.CUSTOMER, "Dana", "Owner"),
                        Product.create(
                                "Auto Cover",
                                ProductType.AUTO_INSURANCE,
                                new BigDecimal("49.00"),
                                12),
                        LocalDate.now(),
                        null);
        Method onCreate = ProductOwnership.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);

        onCreate.invoke(ownership);

        assertThat(ownership.getId()).isNotNull();
        assertThat(ownership.getCreatedAt()).isNotNull();
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
        return ProductOwnership.class.getDeclaredField(fieldName);
    }
}
