package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.product.PaymentRecord;
import com.bayerwestphalian.campaign.product.PaymentStatus;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * KB FR-074 payment-history segment filter helpers.
 *
 * <p>Supported field names: {@code payment_status} (aliases {@code paymentstatus}, {@code
 * payment_history}, {@code paymenthistory}), {@code reminder_count} (alias {@code
 * remindercount}), {@code days_overdue} (alias {@code daysoverdue}), and {@code default_risk}
 * (alias {@code defaultrisk}).
 *
 * <p>Payment statuses: {@code DUE}, {@code PAID}, {@code OVERDUE}, {@code DEFAULT_RISK}. Operators
 * for status: EQUALS, NOT_EQUALS, IN. Numeric fields support EQUALS, NOT_EQUALS, BEFORE, AFTER,
 * BETWEEN. Default risk is a boolean EQUALS / NOT_EQUALS flag derived from {@link
 * PaymentRecord#isDefaultRisk()}.
 */
final class SegmentPaymentHistorySupport {

    private SegmentPaymentHistorySupport() {}

    static boolean isPaymentHistoryField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "payment_status",
                    "paymentstatus",
                    "payment_history",
                    "paymenthistory",
                    "reminder_count",
                    "remindercount",
                    "days_overdue",
                    "daysoverdue",
                    "default_risk",
                    "defaultrisk" -> true;
            default -> false;
        };
    }

    static String canonicalizeFieldName(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return fieldName;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "paymentstatus", "payment_history", "paymenthistory" -> "payment_status";
            case "remindercount" -> "reminder_count";
            case "daysoverdue" -> "days_overdue";
            case "defaultrisk" -> "default_risk";
            default -> normalizeFieldName(fieldName);
        };
    }

    static String normalizeFilterValue(SegmentOperator operator, String fieldName, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return rawValue;
        }

        String canonicalField = canonicalizeFieldName(fieldName);
        if (operator == SegmentOperator.IN || operator == SegmentOperator.BETWEEN) {
            String separator = operator == SegmentOperator.BETWEEN && rawValue.contains("..") ? "\\.\\." : ",";
            String joinSeparator = operator == SegmentOperator.BETWEEN && rawValue.contains("..") ? ".." : ",";
            return Arrays.stream(rawValue.split(separator))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(value -> normalizeSingleFilterValue(canonicalField, value))
                    .collect(Collectors.joining(joinSeparator));
        }

        return normalizeSingleFilterValue(canonicalField, rawValue.trim());
    }

    static void validateFilterValue(SegmentOperator operator, String fieldName, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new IllegalArgumentException(paymentHistoryValidationMessage(fieldName));
        }

        String canonicalField = canonicalizeFieldName(fieldName);
        if (operator == SegmentOperator.IN || operator == SegmentOperator.BETWEEN) {
            String separator = operator == SegmentOperator.BETWEEN && rawValue.contains("..") ? "\\.\\." : ",";
            List<String> parts =
                    Arrays.stream(rawValue.split(separator))
                            .map(String::trim)
                            .filter(StringUtils::hasText)
                            .toList();
            if (operator == SegmentOperator.BETWEEN && parts.size() != 2) {
                throw new IllegalArgumentException(
                        "must provide lower and upper bounds for " + canonicalField);
            }
            parts.forEach(value -> validateSingleFilterValue(canonicalField, value));
            return;
        }

        validateSingleFilterValue(canonicalField, rawValue.trim());
    }

    static boolean matchesCustomerPayments(
            List<PaymentRecord> payments,
            SegmentOperator operator,
            String fieldName,
            String normalizedValue) {
        if (!StringUtils.hasText(normalizedValue)) {
            return false;
        }

        List<PaymentRecord> customerPayments = payments == null ? List.of() : payments;
        return switch (canonicalizeFieldName(fieldName)) {
            case "payment_status" ->
                    matchesPaymentStatus(customerPayments, operator, normalizedValue);
            case "reminder_count" ->
                    matchesNumericAggregate(
                            maxReminderCount(customerPayments), operator, normalizedValue);
            case "days_overdue" ->
                    matchesNumericAggregate(
                            maxDaysOverdue(customerPayments), operator, normalizedValue);
            case "default_risk" -> matchesDefaultRisk(customerPayments, operator, normalizedValue);
            default -> false;
        };
    }

    private static boolean matchesPaymentStatus(
            List<PaymentRecord> payments, SegmentOperator operator, String normalizedValue) {
        List<String> statuses =
                payments.stream().map(payment -> payment.getStatus().name()).toList();

        return switch (operator) {
            case EQUALS ->
                    statuses.stream().anyMatch(status -> status.equalsIgnoreCase(normalizedValue));
            case NOT_EQUALS ->
                    statuses.stream().noneMatch(status -> status.equalsIgnoreCase(normalizedValue));
            case IN ->
                    statuses.stream()
                            .anyMatch(
                                    status ->
                                            splitInValues(normalizedValue).stream()
                                                    .anyMatch(filter -> filter.equalsIgnoreCase(status)));
            default -> false;
        };
    }

    private static boolean matchesDefaultRisk(
            List<PaymentRecord> payments, SegmentOperator operator, String normalizedValue) {
        boolean expected = parseBoolean(normalizedValue);
        boolean actual = payments.stream().anyMatch(PaymentRecord::isDefaultRisk);

        return switch (operator) {
            case EQUALS -> actual == expected;
            case NOT_EQUALS -> actual != expected;
            default -> false;
        };
    }

    private static boolean matchesNumericAggregate(
            long aggregateValue, SegmentOperator operator, String normalizedValue) {
        return SegmentCriteria.matchesValue(operator, normalizedValue, Long.toString(aggregateValue));
    }

    private static long maxReminderCount(List<PaymentRecord> payments) {
        return payments.stream().mapToLong(PaymentRecord::getReminderCount).max().orElse(0L);
    }

    private static long maxDaysOverdue(List<PaymentRecord> payments) {
        return payments.stream().mapToLong(PaymentRecord::calculateDaysOverdue).max().orElse(0L);
    }

    private static String normalizeSingleFilterValue(String canonicalField, String rawValue) {
        validateSingleFilterValue(canonicalField, rawValue);
        return switch (canonicalField) {
            case "payment_status" -> parsePaymentStatus(rawValue).name();
            case "reminder_count", "days_overdue" -> Long.toString(parseNonNegativeLong(rawValue));
            case "default_risk" -> Boolean.toString(parseBoolean(rawValue));
            default -> rawValue.trim();
        };
    }

    private static void validateSingleFilterValue(String canonicalField, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new IllegalArgumentException(paymentHistoryValidationMessage(canonicalField));
        }

        switch (canonicalField) {
            case "payment_status" -> parsePaymentStatus(rawValue);
            case "reminder_count", "days_overdue" -> parseNonNegativeLong(rawValue);
            case "default_risk" -> parseBoolean(rawValue);
            default ->
                    throw new IllegalArgumentException(
                            paymentHistoryValidationMessage(canonicalField));
        }
    }

    private static PaymentStatus parsePaymentStatus(String rawValue) {
        String normalized = rawValue.trim();
        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }

        throw new IllegalArgumentException(
                "must be one of DUE, PAID, OVERDUE, or DEFAULT_RISK");
    }

    private static long parseNonNegativeLong(String rawValue) {
        try {
            long value = Long.parseLong(rawValue.trim());
            if (value < 0) {
                throw new IllegalArgumentException("must be a non-negative integer");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("must be a non-negative integer");
        }
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

    private static List<String> splitInValues(String normalizedValue) {
        return Arrays.stream(normalizedValue.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private static String paymentHistoryValidationMessage(String fieldName) {
        return "must be a valid "
                + canonicalizeFieldName(fieldName)
                + " value for payment history segmentation";
    }

    private static String normalizeFieldName(String fieldName) {
        return fieldName.trim().toLowerCase(Locale.ROOT);
    }
}
