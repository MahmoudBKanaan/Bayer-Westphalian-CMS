package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.customer.Customer;
import com.bayerwestphalian.campaign.customer.CustomerStatus;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * Segment criteria support for KB FR-075 behavior/status filtering using customer marketing
 * status, do-not-contact behavior, and interest/source signals.
 */
final class SegmentBehaviorStatusSupport {

    private static final int SOURCE_MAX_LENGTH = 100;

    private SegmentBehaviorStatusSupport() {}

    static boolean isBehaviorStatusField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "status",
                    "customer_status",
                    "customerstatus",
                    "behavior",
                    "behaviour",
                    "do_not_contact",
                    "donotcontact",
                    "dnc",
                    "source",
                    "interest",
                    "interests" -> true;
            default -> false;
        };
    }

    static String canonicalizeFieldName(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return fieldName;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "customer_status", "customerstatus", "behavior", "behaviour" -> "status";
            case "donotcontact", "dnc" -> "do_not_contact";
            case "interest", "interests" -> "source";
            default -> normalizeFieldName(fieldName);
        };
    }

    static String resolveCustomerValue(Customer customer, String fieldName) {
        if (customer == null || !StringUtils.hasText(fieldName)) {
            return null;
        }

        return switch (canonicalizeFieldName(fieldName)) {
            case "status" -> customer.getStatus() == null ? null : customer.getStatus().name();
            case "do_not_contact" -> Boolean.toString(customer.isDoNotContact());
            case "source" -> customer.getSource();
            default -> null;
        };
    }

    static String normalizeFilterValue(SegmentOperator operator, String fieldName, String rawValue) {
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
            throw new IllegalArgumentException(behaviorStatusValidationMessage(fieldName));
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

    private static String normalizeSingleFilterValue(String canonicalField, String rawValue) {
        validateSingleFilterValue(canonicalField, rawValue);
        return switch (canonicalField) {
            case "status" -> parseCustomerStatus(rawValue).name();
            case "do_not_contact" -> Boolean.toString(parseBoolean(rawValue));
            case "source" -> rawValue.trim();
            default -> rawValue.trim();
        };
    }

    private static void validateSingleFilterValue(String canonicalField, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new IllegalArgumentException(behaviorStatusValidationMessage(canonicalField));
        }

        switch (canonicalField) {
            case "status" -> parseCustomerStatus(rawValue);
            case "do_not_contact" -> parseBoolean(rawValue);
            case "source" -> validateSourceValue(rawValue);
            default ->
                    throw new IllegalArgumentException(
                            behaviorStatusValidationMessage(canonicalField));
        }
    }

    private static CustomerStatus parseCustomerStatus(String rawValue) {
        String normalized = rawValue.trim();
        for (CustomerStatus status : CustomerStatus.values()) {
            if (status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }

        throw new IllegalArgumentException(
                "must be one of ACTIVE, INACTIVE, INTERESTED, UNINTERESTED, or CONVERTED");
    }

    private static boolean parseBoolean(String rawValue) {
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "yes", "1" -> true;
            case "false", "no", "0" -> false;
            default ->
                    throw new IllegalArgumentException(
                            "must be one of true, false, yes, no, 1, or 0");
        };
    }

    private static void validateSourceValue(String rawValue) {
        String trimmed = rawValue.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException(behaviorStatusValidationMessage("source"));
        }
        if (trimmed.length() > SOURCE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "must be at most " + SOURCE_MAX_LENGTH + " characters for source");
        }
    }

    private static String behaviorStatusValidationMessage(String fieldName) {
        return "must be a valid "
                + canonicalizeFieldName(fieldName)
                + " value for behavior/status segmentation";
    }

    private static String normalizeFieldName(String fieldName) {
        return fieldName.trim().toLowerCase(Locale.ROOT);
    }
}
