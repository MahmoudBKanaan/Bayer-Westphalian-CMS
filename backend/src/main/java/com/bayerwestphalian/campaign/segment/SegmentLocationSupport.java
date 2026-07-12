package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.customer.Customer;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * KB FR-071 location segment filter helpers.
 *
 * <p>Supported field names: {@code city}, {@code country}, {@code address_line} (alias {@code
 * addressline}), and {@code location} (alias for {@code city}). Operators commonly used for
 * location matching are EQUALS, NOT_EQUALS, CONTAINS, and IN. Values are trimmed; length limits
 * match customer address fields (city/country 100, address_line 255).
 */
final class SegmentLocationSupport {

    private static final int CITY_MAX_LENGTH = 100;
    private static final int COUNTRY_MAX_LENGTH = 100;
    private static final int ADDRESS_LINE_MAX_LENGTH = 255;

    private SegmentLocationSupport() {}

    static boolean isLocationField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "city", "country", "address_line", "addressline", "location" -> true;
            default -> false;
        };
    }

    static String canonicalizeFieldName(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return fieldName;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "location" -> "city";
            case "addressline" -> "address_line";
            default -> normalizeFieldName(fieldName);
        };
    }

    static String resolveCustomerValue(Customer customer, String fieldName) {
        if (customer == null || !StringUtils.hasText(fieldName)) {
            return null;
        }

        return switch (canonicalizeFieldName(fieldName)) {
            case "city" -> customer.getCity();
            case "country" -> customer.getCountry();
            case "address_line" -> customer.getAddressLine();
            default -> null;
        };
    }

    static String normalizeFilterValue(
            SegmentOperator operator, String fieldName, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return rawValue;
        }

        if (operator == SegmentOperator.IN) {
            return Arrays.stream(rawValue.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(value -> normalizeSingleFilterValue(fieldName, value))
                    .collect(Collectors.joining(","));
        }

        return normalizeSingleFilterValue(fieldName, rawValue.trim());
    }

    static void validateFilterValue(SegmentOperator operator, String fieldName, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new IllegalArgumentException(locationValidationMessage(fieldName));
        }

        if (operator == SegmentOperator.IN) {
            Arrays.stream(rawValue.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(value -> validateSingleFilterValue(fieldName, value));
            return;
        }

        validateSingleFilterValue(fieldName, rawValue.trim());
    }

    private static String normalizeSingleFilterValue(String fieldName, String rawValue) {
        validateSingleFilterValue(fieldName, rawValue);
        return rawValue.trim();
    }

    private static void validateSingleFilterValue(String fieldName, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new IllegalArgumentException(locationValidationMessage(fieldName));
        }

        String trimmed = rawValue.trim();
        int maxLength = maxLengthForField(fieldName);
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(
                    "must be at most "
                            + maxLength
                            + " characters for "
                            + canonicalizeFieldName(fieldName));
        }
    }

    private static int maxLengthForField(String fieldName) {
        return switch (canonicalizeFieldName(fieldName)) {
            case "city" -> CITY_MAX_LENGTH;
            case "country" -> COUNTRY_MAX_LENGTH;
            case "address_line" -> ADDRESS_LINE_MAX_LENGTH;
            default -> CITY_MAX_LENGTH;
        };
    }

    private static String locationValidationMessage(String fieldName) {
        return "must be a valid "
                + canonicalizeFieldName(fieldName)
                + " value for location segmentation";
    }

    private static String normalizeFieldName(String fieldName) {
        return fieldName.trim().toLowerCase(Locale.ROOT);
    }
}
