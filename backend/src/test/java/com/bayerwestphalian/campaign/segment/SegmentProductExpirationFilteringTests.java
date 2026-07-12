package com.bayerwestphalian.campaign.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import com.bayerwestphalian.campaign.product.Product;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit coverage for KB FR-076 product-expiration field helpers (3/6/12-month windows). */
class SegmentProductExpirationFilteringTests {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 9);

    @Test
    void recognizesKbProductExpirationFieldNames() {
        assertThat(
                        SegmentProductExpirationSupport.isProductExpirationField(
                                "expiring_within_months"))
                .isTrue();
        assertThat(
                        SegmentProductExpirationSupport.isProductExpirationField(
                                "EXPIRING_WITHIN_MONTHS"))
                .isTrue();
        assertThat(SegmentProductExpirationSupport.isProductExpirationField("product_expiration"))
                .isTrue();
        assertThat(SegmentProductExpirationSupport.isProductExpirationField("expiration_date"))
                .isTrue();
        assertThat(SegmentProductExpirationSupport.isProductExpirationField("is_expiring"))
                .isTrue();
        assertThat(SegmentProductExpirationSupport.isProductExpirationField("product_expiring"))
                .isTrue();
        assertThat(SegmentProductExpirationSupport.isProductExpirationField("city")).isFalse();
        assertThat(SegmentProductExpirationSupport.isProductExpirationField("product_type"))
                .isFalse();
    }

    @Test
    void canonicalizesProductExpirationAliasesToKbFieldNames() {
        assertThat(SegmentProductExpirationSupport.canonicalizeFieldName("product_expiration"))
                .isEqualTo("expiring_within_months");
        assertThat(SegmentProductExpirationSupport.canonicalizeFieldName("expiring_within"))
                .isEqualTo("expiring_within_months");
        assertThat(
                        SegmentProductExpirationSupport.canonicalizeFieldName(
                                "product_expiration_months"))
                .isEqualTo("expiring_within_months");
        assertThat(SegmentProductExpirationSupport.canonicalizeFieldName("product_expiration_date"))
                .isEqualTo("expiration_date");
        assertThat(SegmentProductExpirationSupport.canonicalizeFieldName("product_expiring"))
                .isEqualTo("is_expiring");
    }

    @Test
    void normalizesProductExpirationFilterValues() {
        assertThat(
                        SegmentProductExpirationSupport.normalizeFilterValue(
                                SegmentOperator.EQUALS, "product_expiration", "6"))
                .isEqualTo("6");
        assertThat(
                        SegmentProductExpirationSupport.normalizeFilterValue(
                                SegmentOperator.IN, "expiring_within_months", "3, 6, 12"))
                .isEqualTo("3,6,12");
        assertThat(
                        SegmentProductExpirationSupport.normalizeFilterValue(
                                SegmentOperator.EQUALS, "expiration_date", "2026-10-01"))
                .isEqualTo("2026-10-01");
        assertThat(
                        SegmentProductExpirationSupport.normalizeFilterValue(
                                SegmentOperator.EQUALS, "is_expiring", "YES"))
                .isEqualTo("true");
    }

    @Test
    void matchesCustomersExpiringWithinKbMonthWindows() {
        ProductOwnership expiringInTwoMonths = ownershipExpiringInMonths(2);
        ProductOwnership expiringInEightMonths = ownershipExpiringInMonths(8);
        ProductOwnership expiringInEighteenMonths = ownershipExpiringInMonths(18);

        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(expiringInTwoMonths),
                                SegmentOperator.EQUALS,
                                "expiring_within_months",
                                "3",
                                TODAY))
                .isTrue();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(expiringInEightMonths),
                                SegmentOperator.EQUALS,
                                "product_expiration",
                                "6",
                                TODAY))
                .isFalse();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(expiringInEightMonths),
                                SegmentOperator.EQUALS,
                                "expiring_within",
                                "12",
                                TODAY))
                .isTrue();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(expiringInEighteenMonths),
                                SegmentOperator.EQUALS,
                                "expiring_within_months",
                                "12",
                                TODAY))
                .isFalse();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(expiringInTwoMonths, expiringInEighteenMonths),
                                SegmentOperator.IN,
                                "expiring_within_months",
                                "3,6,12",
                                TODAY))
                .isTrue();
    }

    @Test
    void matchesIsExpiringUsingTwelveMonthKbWindow() {
        ProductOwnership expiringSoon = ownershipExpiringInMonths(4);
        ProductOwnership farFuture = ownershipExpiringInMonths(18);

        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(expiringSoon),
                                SegmentOperator.EQUALS,
                                "is_expiring",
                                "true",
                                TODAY))
                .isTrue();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(farFuture),
                                SegmentOperator.EQUALS,
                                "product_expiring",
                                "true",
                                TODAY))
                .isFalse();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(farFuture),
                                SegmentOperator.EQUALS,
                                "is_expiring",
                                "false",
                                TODAY))
                .isTrue();
    }

    @Test
    void matchesAbsoluteExpirationDateWithComparisonOperators() {
        ProductOwnership ownership = ownershipExpiringOn(LocalDate.of(2026, 10, 15));

        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(ownership),
                                SegmentOperator.EQUALS,
                                "expiration_date",
                                "2026-10-15",
                                TODAY))
                .isTrue();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(ownership),
                                SegmentOperator.BEFORE,
                                "expiration_date",
                                "2026-12-01",
                                TODAY))
                .isTrue();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(ownership),
                                SegmentOperator.AFTER,
                                "expiration_date",
                                "2026-09-01",
                                TODAY))
                .isTrue();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(ownership),
                                SegmentOperator.BETWEEN,
                                "product_expiration_date",
                                "2026-10-01..2026-10-31",
                                TODAY))
                .isTrue();
    }

    @Test
    void ignoresCancelledOrExpiredOwnershipsForExpirationWindows() {
        ProductOwnership cancelled =
                ProductOwnership.create(
                        customer(),
                        product(ProductType.LIFE_INSURANCE),
                        TODAY.minusYears(1),
                        TODAY.plusMonths(2));
        cancelled.cancel();

        ProductOwnership expired =
                ProductOwnership.create(
                        customer(),
                        product(ProductType.HOMEOWNER_INSURANCE),
                        TODAY.minusYears(2),
                        TODAY.minusDays(1));
        expired.expire();

        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(cancelled, expired),
                                SegmentOperator.EQUALS,
                                "expiring_within_months",
                                "3",
                                TODAY))
                .isFalse();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(cancelled),
                                SegmentOperator.EQUALS,
                                "is_expiring",
                                "true",
                                TODAY))
                .isFalse();
    }

    @Test
    void notEqualsMatchesCustomersWithoutMatchingExpirationWindow() {
        ProductOwnership farFuture = ownershipExpiringInMonths(18);

        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(farFuture),
                                SegmentOperator.NOT_EQUALS,
                                "expiring_within_months",
                                "6",
                                TODAY))
                .isTrue();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(),
                                SegmentOperator.NOT_EQUALS,
                                "expiring_within_months",
                                "6",
                                TODAY))
                .isTrue();
    }

    @Test
    void rejectsUnsupportedProductExpirationFilterValues() {
        assertThatThrownBy(
                        () ->
                                SegmentProductExpirationSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "expiring_within_months", "-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                SegmentProductExpirationSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "expiration_date", "next-year"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                SegmentProductExpirationSupport.validateFilterValue(
                                        SegmentOperator.EQUALS, "is_expiring", "maybe"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                SegmentProductExpirationSupport.validateFilterValue(
                                        SegmentOperator.BETWEEN, "expiring_within_months", "3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lower and upper bounds");
    }

    @Test
    void matchesMonthsBetweenAndBeforeAfterOperatorsForKbWindows() {
        ProductOwnership expiringInFive = ownershipExpiringInMonths(5);

        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(expiringInFive),
                                SegmentOperator.BETWEEN,
                                "expiring_within_months",
                                "3,6",
                                TODAY))
                .isTrue();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(expiringInFive),
                                SegmentOperator.BEFORE,
                                "expiring_within_months",
                                "6",
                                TODAY))
                .isTrue();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(expiringInFive),
                                SegmentOperator.AFTER,
                                "expiring_within_months",
                                "3",
                                TODAY))
                .isTrue();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(ownershipExpiringInMonths(2)),
                                SegmentOperator.AFTER,
                                "expiring_within_months",
                                "3",
                                TODAY))
                .isFalse();
    }

    @Test
    void isExpiringWithinMonthsHelperHonorsActiveWindowBounds() {
        ProductOwnership twoMonths = ownershipExpiringInMonths(2);
        ProductOwnership eighteenMonths = ownershipExpiringInMonths(18);

        assertThat(SegmentProductExpirationSupport.isExpiringWithinMonths(twoMonths, 3, TODAY))
                .isTrue();
        assertThat(SegmentProductExpirationSupport.isExpiringWithinMonths(twoMonths, 6, TODAY))
                .isTrue();
        assertThat(SegmentProductExpirationSupport.isExpiringWithinMonths(twoMonths, 12, TODAY))
                .isTrue();
        assertThat(
                        SegmentProductExpirationSupport.isExpiringWithinMonths(
                                eighteenMonths, 12, TODAY))
                .isFalse();
        assertThat(SegmentProductExpirationSupport.isExpiringWithinMonths(null, 3, TODAY))
                .isFalse();
    }

    @Test
    void emptyOwnershipListDoesNotMatchIsExpiringTrue() {
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(), SegmentOperator.EQUALS, "is_expiring", "true", TODAY))
                .isFalse();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                List.of(), SegmentOperator.EQUALS, "is_expiring", "false", TODAY))
                .isTrue();
        assertThat(
                        SegmentProductExpirationSupport.matchesCustomerOwnerships(
                                null, SegmentOperator.EQUALS, "expiring_within_months", "3", TODAY))
                .isFalse();
    }

    private static ProductOwnership ownershipExpiringInMonths(int months) {
        return ownershipExpiringOn(TODAY.plusMonths(months));
    }

    private static ProductOwnership ownershipExpiringOn(LocalDate expirationDate) {
        return ProductOwnership.create(
                customer(),
                product(ProductType.LIFE_INSURANCE),
                TODAY.minusMonths(6),
                expirationDate);
    }

    private static Customer customer() {
        return Customer.create(CustomerType.CUSTOMER, "Tom", "Schmidt");
    }

    private static Product product(ProductType productType) {
        return Product.create(
                "Expiration Product " + productType.name(),
                productType,
                new BigDecimal("99.00"),
                12);
    }
}
