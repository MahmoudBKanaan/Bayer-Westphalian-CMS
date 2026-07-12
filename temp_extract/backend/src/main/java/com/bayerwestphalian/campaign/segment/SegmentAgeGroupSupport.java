package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerAgeGroup;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * KB FR-070 age-group segment filter helpers.
 *
 * <p>Supported field names: {@code age_group}, {@code agegroup}. Customer values use KB database
 * codes {@code MINOR}, {@code 18_25}, {@code 26_40}, {@code 41_60}, {@code 60_PLUS}. Filter input
 * may use those codes or enum names ({@code AGE_18_25}, …); values are normalized before matching.
 */
final class SegmentAgeGroupSupport {

    private SegmentAgeGroupSupport() {}

    static boolean isAgeGroupField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "age_group", "agegroup" -> true;
            default -> false;
        };
    }

    static String resolveCustomerValue(Customer customer) {
        if (customer == null || customer.getAgeGroup() == null) {
            return null;
        }
        return customer.getAgeGroup().getDatabaseValue();
    }

    static String normalizeFilterValue(SegmentOperator operator, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return rawValue;
        }

        if (operator == SegmentOperator.IN) {
            return Arrays.stream(rawValue.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(SegmentAgeGroupSupport::normalizeSingleFilterValue)
                    .collect(Collectors.joining(","));
        }

        return normalizeSingleFilterValue(rawValue.trim());
    }

    static void validateFilterValue(SegmentOperator operator, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new IllegalArgumentException("must be a supported customer age group");
        }

        if (operator == SegmentOperator.IN) {
            Arrays.stream(rawValue.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(SegmentAgeGroupSupport::parseFilterValue);
            return;
        }

        parseFilterValue(rawValue.trim());
    }

    private static String normalizeSingleFilterValue(String rawValue) {
        return parseFilterValue(rawValue).getDatabaseValue();
    }

    private static CustomerAgeGroup parseFilterValue(String rawValue) {
        String normalized = rawValue.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("must be a supported customer age group");
        }

        for (CustomerAgeGroup ageGroup : CustomerAgeGroup.values()) {
            if (ageGroup.name().equalsIgnoreCase(normalized)
                    || ageGroup.getDatabaseValue().equalsIgnoreCase(normalized)) {
                return ageGroup;
            }
        }

        throw new IllegalArgumentException(
                "must be one of MINOR, 18_25, 26_40, 41_60, 60_PLUS, AGE_18_25, AGE_26_40, AGE_41_60, or AGE_60_PLUS");
    }

    private static String normalizeFieldName(String fieldName) {
        return fieldName.trim().toLowerCase(Locale.ROOT);
    }
}