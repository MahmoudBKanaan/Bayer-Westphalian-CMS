package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductType;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for KB FR-073 product-ownership field helpers used by segment criteria matching.
 */
class SegmentProductOwnershipFilteringTests {

    private static final UUID PRODUCT_ID = UUID.fromString("41000000-0000-0000-0000-000000000201");

    @Test
    void recognizesKbProductOwnershipFieldNames() {
        assertThat(SegmentProductOwnershipSupport.isProductOwnershipField("product_type")).isTrue();
        assertThat(SegmentProductOwnershipSupport.isProductOwnershipField("PRODUCT_TYPE")).isTrue();
        assertThat(SegmentProductOwnershipSupport.isProductOwnershipField("producttype")).isTrue();
        assertThat(SegmentProductOwnershipSupport.isProductOwnershipField("owned_product_type"))
                .isTrue();
        assertThat(SegmentProductOwnershipSupport.isProductOwnershipField("product_id")).isTrue();
        assertThat(SegmentProductOwnershipSupport.isProductOwnershipField("productid")).isTrue();
        assertThat(SegmentProductOwnershipSupport.isProductOwnershipField("ownership_status"))
                .isTrue();
        assertThat(SegmentProductOwnershipSupport.isProductOwnershipField("product_ownership"))
                .isTrue();
        assertThat(SegmentProductOwnershipSupport.isProductOwnershipField("city")).isFalse();
    }

    @Test
    void canonicalizesProductOwnershipAliasesToKbFieldNames() {
        assertThat(SegmentProductOwnershipSupport.canonicalizeFieldName("producttype"))
                .isEqualTo("product_type");
        assertThat(SegmentProductOwnershipSupport.canonicalizeFieldName("owned_product_type"))
                .isEqualTo("product_type");
        assertThat(SegmentProductOwnershipSupport.canonicalizeFieldName("productid"))
                .isEqualTo("product_id");
        assertThat(SegmentProductOwnershipSupport.canonicalizeFieldName("ownershipstatus"))
                .isEqualTo("ownership_status");
        assertThat(SegmentProductOwnershipSupport.canonicalizeFieldName("product_ownership"))
                .isEqualTo("product_type");
    }

    @Test
    void normalizesProductOwnershipFilterValues() {
        assertThat(
                        SegmentProductOwnershipSupport.normalizeFilterValue(
                                SegmentOperator.EQUALS, "product_type", "homeowner_insurance"))
                .isEqualTo("HOMEOWNER_INSURANCE");
        assertThat(
                        SegmentProductOwnershipSupport.normalizeFilterValue(
                                SegmentOperator.IN,
                                "product_type",
                                "life_insurance, investment_fund"))
                .isEqualTo("LIFE_INSURANCE,INVESTMENT_FUND");
        assertThat(
                        SegmentProductOwnershipSupport.normalizeFilterValue(
                                SegmentOperator.EQUALS, "product_id", PRODUCT_ID.toString()))
                .isEqualTo(PRODUCT_ID.toString());
    }

    @Test
    void matchesCustomersWithOwnedProductTypeUsingKbOperators() {
        ProductOwnership homeownerOwnership = activeOwnership(ProductType.HOMEOWNER_INSURANCE);
        ProductOwnership lifeOwnership = activeOwnership(ProductType.LIFE_INSURANCE);

        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(homeownerOwnership),
                                SegmentOperator.EQUALS,
                                "product_type",
                                "HOMEOWNER_INSURANCE"))
                .isTrue();
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(homeownerOwnership),
                                SegmentOperator.NOT_EQUALS,
                                "product_type",
                                "HOMEOWNER_INSURANCE"))
                .isFalse();
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(homeownerOwnership),
                                SegmentOperator.NOT_EQUALS,
                                "product_type",
                                "LIFE_INSURANCE"))
                .isTrue();
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(homeownerOwnership, lifeOwnership),
                                SegmentOperator.IN,
                                "product_type",
                                "LIFE_INSURANCE,INVESTMENT_FUND"))
                .isTrue();
    }

    @Test
    void matchesCustomersWithOwnedProductId() {
        ProductOwnership ownership = activeOwnership(ProductType.LIFE_INSURANCE);

        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(ownership),
                                SegmentOperator.EQUALS,
                                "product_id",
                                PRODUCT_ID.toString()))
                .isTrue();
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(ownership),
                                SegmentOperator.NOT_EQUALS,
                                "product_id",
                                UUID.randomUUID().toString()))
                .isTrue();
    }

    @Test
    void matchesCustomersWithOwnershipStatus() {
        ProductOwnership activeOwnership = activeOwnership(ProductType.LIFE_INSURANCE);
        ProductOwnership cancelledOwnership =
                ProductOwnership.create(
                        Customer.create(CustomerType.CUSTOMER, "Cancelled", "Owner"),
                        product(ProductType.AUTO_INSURANCE),
                        LocalDate.now().minusYears(1),
                        LocalDate.now().plusYears(1));
        cancelledOwnership.cancel();

        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(activeOwnership),
                                SegmentOperator.EQUALS,
                                "ownership_status",
                                "ACTIVE"))
                .isTrue();
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(cancelledOwnership),
                                SegmentOperator.EQUALS,
                                "ownership_status",
                                "CANCELLED"))
                .isTrue();
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(activeOwnership),
                                SegmentOperator.NOT_EQUALS,
                                "ownership_status",
                                "CANCELLED"))
                .isTrue();
    }

    @Test
    void ignoresInactiveOwnershipsForProductTypeMatching() {
        ProductOwnership expiredOwnership =
                ProductOwnership.create(
                        Customer.create(CustomerType.CUSTOMER, "Expired", "Owner"),
                        product(ProductType.HOMEOWNER_INSURANCE),
                        LocalDate.now().minusYears(2),
                        LocalDate.now().minusMonths(1));
        expiredOwnership.expire();

        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(expiredOwnership),
                                SegmentOperator.EQUALS,
                                "product_type",
                                "HOMEOWNER_INSURANCE"))
                .isFalse();
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(expiredOwnership),
                                SegmentOperator.NOT_EQUALS,
                                "product_type",
                                "HOMEOWNER_INSURANCE"))
                .isTrue();
    }

    @Test
    void rejectsUnsupportedProductOwnershipFilterValues() {
        assertThatThrownBy(
                        () ->
                                SegmentProductOwnershipSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "product_type", "TRAVEL_INSURANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be one of HOMEOWNER_INSURANCE");

        assertThatThrownBy(
                        () ->
                                SegmentProductOwnershipSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "product_id", "not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a valid product_id UUID");

        assertThatThrownBy(
                        () ->
                                SegmentProductOwnershipSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "ownership_status", "PENDING"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be one of ACTIVE, EXPIRED, or CANCELLED");

        assertThatThrownBy(
                        () ->
                                SegmentProductOwnershipSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "product_type", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid product_type");
    }

    @Test
    void matchesExpiredOwnershipStatusAndIgnoresExpiredForProductType() {
        ProductOwnership expiredOwnership =
                ProductOwnership.create(
                        Customer.create(CustomerType.CUSTOMER, "Expired", "Owner"),
                        product(ProductType.LIFE_INSURANCE),
                        LocalDate.now().minusYears(2),
                        LocalDate.now().minusMonths(1));
        expiredOwnership.expire();

        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(expiredOwnership),
                                SegmentOperator.EQUALS,
                                "ownership_status",
                                "EXPIRED"))
                .isTrue();
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(expiredOwnership),
                                SegmentOperator.EQUALS,
                                "product_type",
                                "LIFE_INSURANCE"))
                .isFalse();
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(expiredOwnership),
                                SegmentOperator.EQUALS,
                                "product_id",
                                PRODUCT_ID.toString()))
                .isFalse();
    }

    @Test
    void emptyOwnershipListMatchesNotEqualsButNotEqualsProductType() {
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(),
                                SegmentOperator.EQUALS,
                                "product_type",
                                "LIFE_INSURANCE"))
                .isFalse();
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(),
                                SegmentOperator.NOT_EQUALS,
                                "product_type",
                                "LIFE_INSURANCE"))
                .isTrue();
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                null, SegmentOperator.EQUALS, "ownership_status", "ACTIVE"))
                .isFalse();
    }

    @Test
    void matchesProductIdInList() {
        ProductOwnership ownership = activeOwnership(ProductType.LIFE_INSURANCE);
        UUID otherId = UUID.fromString("41000000-0000-0000-0000-000000000299");

        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(ownership),
                                SegmentOperator.IN,
                                "product_id",
                                otherId + "," + PRODUCT_ID))
                .isTrue();
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(ownership),
                                SegmentOperator.IN,
                                "product_id",
                                otherId.toString()))
                .isFalse();
    }

    @Test
    void ownershipStatusInOperatorMatchesAnyListedStatus() {
        ProductOwnership cancelledOwnership =
                ProductOwnership.create(
                        Customer.create(CustomerType.CUSTOMER, "Cancelled", "Owner"),
                        product(ProductType.AUTO_INSURANCE),
                        LocalDate.now().minusYears(1),
                        LocalDate.now().plusYears(1));
        cancelledOwnership.cancel();

        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(cancelledOwnership),
                                SegmentOperator.IN,
                                "ownership_status",
                                "EXPIRED,CANCELLED"))
                .isTrue();
        assertThat(
                        SegmentProductOwnershipSupport.matchesCustomerOwnerships(
                                List.of(cancelledOwnership),
                                SegmentOperator.IN,
                                "ownership_status",
                                "ACTIVE,EXPIRED"))
                .isFalse();
    }

    private static ProductOwnership activeOwnership(ProductType productType) {
        Customer customer = Customer.create(CustomerType.CUSTOMER, "Ada", "Owner");
        Product product = product(productType);
        return ProductOwnership.create(
                customer, product, LocalDate.now().minusMonths(6), LocalDate.now().plusYears(1));
    }

    private static Product product(ProductType productType) {
        Product product =
                Product.create("Segment Product", productType, new BigDecimal("99.00"), 12);
        setEntityId(product, PRODUCT_ID);
        return product;
    }

    private static void setEntityId(BaseEntity entity, UUID id) {
        try {
            Field idField = BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Failed to assign entity id for test fixture", exception);
        }
    }
}
