package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerType;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

final class SegmentCustomerTypeSupport {

    private SegmentCustomerTypeSupport() {}

    static boolean isCustomerTypeField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "customer_type", "customertype", "type" -> true;
            default -> false;
        };
    }

    static String canonicalizeFieldName(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return fieldName;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "customertype", "type" -> "customer_type";
            default -> normalizeFieldName(fieldName);
        };
    }

    static String resolveCustomerValue(Customer customer) {
        if (customer == null || customer.getCustomerType() == null) {
            return null;
        }
        return customer.getCustomerType().name();
    }

    static String normalizeFilterValue(SegmentOperator operator, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return rawValue;
        }

        if (operator == SegmentOperator.IN) {
            return Arrays.stream(rawValue.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(SegmentCustomerTypeSupport::normalizeSingleFilterValue)
                    .collect(Collectors.joining(","));
        }

        return normalizeSingleFilterValue(rawValue.trim());
    }

    static void validateFilterValue(SegmentOperator operator, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new IllegalArgumentException(customerTypeValidationMessage());
        }

        if (operator == SegmentOperator.IN) {
            Arrays.stream(rawValue.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(SegmentCustomerTypeSupport::parseFilterValue);
            return;
        }

        parseFilterValue(rawValue.trim());
    }

    private static String normalizeSingleFilterValue(String rawValue) {
        return parseFilterValue(rawValue).name();
    }

    private static CustomerType parseFilterValue(String rawValue) {
        String normalized = rawValue.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException(customerTypeValidationMessage());
        }

        for (CustomerType customerType : CustomerType.values()) {
            if (customerType.name().equalsIgnoreCase(normalized)) {
                return customerType;
            }
        }

        throw new IllegalArgumentException("must be one of CUSTOMER, PROSPECT, or BENEFICIARY");
    }

    private static String customerTypeValidationMessage() {
        return "must be a valid customer_type value for customer/prospect segmentation";
    }

    private static String normalizeFieldName(String fieldName) {
        return fieldName.trim().toLowerCase(Locale.ROOT);
    }
}
