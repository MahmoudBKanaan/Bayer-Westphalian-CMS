package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.product.OwnershipStatus;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import com.bayerwestphalian.campaign.product.ProductType;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * KB FR-073 product-ownership segment filter helpers.
 *
 * <p>Supported field names: {@code product_type} (aliases {@code producttype}, {@code
 * owned_product_type}, {@code product_ownership}), {@code product_id} (aliases {@code productid},
 * {@code owned_product_id}), and {@code ownership_status} (alias {@code ownershipstatus}).
 *
 * <p>Product type and product id matching consider only <strong>active</strong> ownerships.
 * Ownership status matching uses ACTIVE / EXPIRED / CANCELLED. Operators: EQUALS, NOT_EQUALS, IN.
 * Product types: HOMEOWNER_INSURANCE, LIFE_INSURANCE, INVESTMENT_FUND, HEALTH_INSURANCE,
 * AUTO_INSURANCE, OTHER.
 */
final class SegmentProductOwnershipSupport {

    private SegmentProductOwnershipSupport() {}

    static boolean isProductOwnershipField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "product_type",
                    "producttype",
                    "owned_product_type",
                    "product_id",
                    "productid",
                    "owned_product_id",
                    "ownership_status",
                    "ownershipstatus",
                    "product_ownership" ->
                    true;
            default -> false;
        };
    }

    static String canonicalizeFieldName(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return fieldName;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "producttype", "owned_product_type" -> "product_type";
            case "productid", "owned_product_id" -> "product_id";
            case "ownershipstatus" -> "ownership_status";
            case "product_ownership" -> "product_type";
            default -> normalizeFieldName(fieldName);
        };
    }

    static String normalizeFilterValue(
            SegmentOperator operator, String fieldName, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return rawValue;
        }

        String canonicalField = canonicalizeFieldName(fieldName);
        if (operator == SegmentOperator.IN) {
            return Arrays.stream(rawValue.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(value -> normalizeSingleFilterValue(canonicalField, value))
                    .collect(Collectors.joining(","));
        }

        return normalizeSingleFilterValue(canonicalField, rawValue.trim());
    }

    static void validateFilterValue(SegmentOperator operator, String fieldName, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new IllegalArgumentException(productOwnershipValidationMessage(fieldName));
        }

        String canonicalField = canonicalizeFieldName(fieldName);
        if (operator == SegmentOperator.IN) {
            Arrays.stream(rawValue.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(value -> validateSingleFilterValue(canonicalField, value));
            return;
        }

        validateSingleFilterValue(canonicalField, rawValue.trim());
    }

    static boolean matchesCustomerOwnerships(
            List<ProductOwnership> ownerships,
            SegmentOperator operator,
            String fieldName,
            String normalizedValue) {
        if (!StringUtils.hasText(normalizedValue)) {
            return false;
        }

        List<ProductOwnership> customerOwnerships = ownerships == null ? List.of() : ownerships;
        return switch (canonicalizeFieldName(fieldName)) {
            case "product_type" ->
                    matchesProductType(customerOwnerships, operator, normalizedValue);
            case "product_id" -> matchesProductId(customerOwnerships, operator, normalizedValue);
            case "ownership_status" ->
                    matchesOwnershipStatus(customerOwnerships, operator, normalizedValue);
            default -> false;
        };
    }

    private static boolean matchesProductType(
            List<ProductOwnership> ownerships, SegmentOperator operator, String normalizedValue) {
        List<String> ownedTypes =
                ownerships.stream()
                        .filter(ProductOwnership::isActive)
                        .map(ownership -> ownership.getProduct().getProductType().name())
                        .toList();

        return switch (operator) {
            case EQUALS ->
                    ownedTypes.stream().anyMatch(type -> type.equalsIgnoreCase(normalizedValue));
            case NOT_EQUALS ->
                    ownedTypes.stream().noneMatch(type -> type.equalsIgnoreCase(normalizedValue));
            case IN ->
                    ownedTypes.stream()
                            .anyMatch(
                                    type ->
                                            splitInValues(normalizedValue).stream()
                                                    .anyMatch(
                                                            filter ->
                                                                    filter.equalsIgnoreCase(type)));
            default -> false;
        };
    }

    private static boolean matchesProductId(
            List<ProductOwnership> ownerships, SegmentOperator operator, String normalizedValue) {
        List<String> ownedProductIds =
                ownerships.stream()
                        .filter(ProductOwnership::isActive)
                        .map(ownership -> ownership.getProduct().getId().toString())
                        .toList();

        return switch (operator) {
            case EQUALS ->
                    ownedProductIds.stream().anyMatch(id -> id.equalsIgnoreCase(normalizedValue));
            case NOT_EQUALS ->
                    ownedProductIds.stream().noneMatch(id -> id.equalsIgnoreCase(normalizedValue));
            case IN ->
                    ownedProductIds.stream()
                            .anyMatch(
                                    id ->
                                            splitInValues(normalizedValue).stream()
                                                    .anyMatch(
                                                            filter -> filter.equalsIgnoreCase(id)));
            default -> false;
        };
    }

    private static boolean matchesOwnershipStatus(
            List<ProductOwnership> ownerships, SegmentOperator operator, String normalizedValue) {
        if (operator == SegmentOperator.IN) {
            return splitInValues(normalizedValue).stream()
                    .anyMatch(
                            value ->
                                    matchesOwnershipStatus(
                                            ownerships, SegmentOperator.EQUALS, value));
        }

        OwnershipStatus status = parseOwnershipStatus(normalizedValue);

        boolean matchesStatus =
                switch (status) {
                    case ACTIVE -> ownerships.stream().anyMatch(ProductOwnership::isActive);
                    case EXPIRED ->
                            ownerships.stream()
                                    .anyMatch(
                                            ownership ->
                                                    ownership.getStatus()
                                                            == OwnershipStatus.EXPIRED);
                    case CANCELLED ->
                            ownerships.stream()
                                    .anyMatch(
                                            ownership ->
                                                    ownership.getStatus()
                                                            == OwnershipStatus.CANCELLED);
                };

        return switch (operator) {
            case EQUALS -> matchesStatus;
            case NOT_EQUALS -> !matchesStatus;
            default -> false;
        };
    }

    private static String normalizeSingleFilterValue(String canonicalField, String rawValue) {
        validateSingleFilterValue(canonicalField, rawValue);
        return switch (canonicalField) {
            case "product_type" -> parseProductType(rawValue).name();
            case "product_id" -> parseProductId(rawValue).toString();
            case "ownership_status" -> parseOwnershipStatus(rawValue).name();
            default -> rawValue.trim();
        };
    }

    private static void validateSingleFilterValue(String canonicalField, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new IllegalArgumentException(productOwnershipValidationMessage(canonicalField));
        }

        switch (canonicalField) {
            case "product_type" -> parseProductType(rawValue);
            case "product_id" -> parseProductId(rawValue);
            case "ownership_status" -> parseOwnershipStatus(rawValue);
            default ->
                    throw new IllegalArgumentException(
                            productOwnershipValidationMessage(canonicalField));
        }
    }

    private static ProductType parseProductType(String rawValue) {
        String normalized = rawValue.trim();
        for (ProductType productType : ProductType.values()) {
            if (productType.name().equalsIgnoreCase(normalized)) {
                return productType;
            }
        }

        throw new IllegalArgumentException(
                "must be one of HOMEOWNER_INSURANCE, LIFE_INSURANCE, INVESTMENT_FUND,"
                        + " HEALTH_INSURANCE, AUTO_INSURANCE, or OTHER");
    }

    private static UUID parseProductId(String rawValue) {
        try {
            return UUID.fromString(rawValue.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("must be a valid product_id UUID");
        }
    }

    private static OwnershipStatus parseOwnershipStatus(String rawValue) {
        String normalized = rawValue.trim();
        for (OwnershipStatus status : OwnershipStatus.values()) {
            if (status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }

        throw new IllegalArgumentException("must be one of ACTIVE, EXPIRED, or CANCELLED");
    }

    private static List<String> splitInValues(String normalizedValue) {
        return Arrays.stream(normalizedValue.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private static String productOwnershipValidationMessage(String fieldName) {
        return "must be a valid "
                + canonicalizeFieldName(fieldName)
                + " value for product ownership segmentation";
    }

    private static String normalizeFieldName(String fieldName) {
        return fieldName.trim().toLowerCase(Locale.ROOT);
    }
}
