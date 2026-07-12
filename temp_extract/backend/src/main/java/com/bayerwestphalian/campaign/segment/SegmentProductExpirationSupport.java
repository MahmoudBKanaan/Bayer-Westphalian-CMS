package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.product.OwnershipStatus;
import com.bayerwestphalian.campaign.product.ProductOwnership;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * KB FR-076 product-expiration segment filter helpers (BR-023 / FR-085–087 3/6/12-month windows).
 *
 * <p>Supported field names: {@code expiring_within_months} (aliases {@code expiringwithinmonths},
 * {@code expiring_within}, {@code product_expiration}, {@code productexpiration}, {@code
 * product_expiration_months}), {@code expiration_date} (aliases {@code expirationdate}, {@code
 * product_expiration_date}), and {@code is_expiring} (aliases {@code isexpiring}, {@code
 * product_expiring} — true when any active ownership expires within 12 months).
 *
 * <p>Month windows commonly use 3, 6, or 12. Operators for months: EQUALS, NOT_EQUALS, IN, BEFORE,
 * AFTER, BETWEEN. Expiration dates use ISO-8601 {@code YYYY-MM-DD} with EQUALS / BEFORE / AFTER /
 * BETWEEN. Only active, non-past ownerships are considered.
 */
final class SegmentProductExpirationSupport {

    private SegmentProductExpirationSupport() {}

    static boolean isProductExpirationField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "expiring_within_months",
                    "expiringwithinmonths",
                    "expiring_within",
                    "product_expiration",
                    "productexpiration",
                    "product_expiration_months",
                    "expiration_date",
                    "expirationdate",
                    "product_expiration_date",
                    "is_expiring",
                    "isexpiring",
                    "product_expiring" -> true;
            default -> false;
        };
    }

    static String canonicalizeFieldName(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return fieldName;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "expiringwithinmonths",
                    "expiring_within",
                    "product_expiration",
                    "productexpiration",
                    "product_expiration_months" -> "expiring_within_months";
            case "expirationdate", "product_expiration_date" -> "expiration_date";
            case "isexpiring", "product_expiring" -> "is_expiring";
            default -> normalizeFieldName(fieldName);
        };
    }

    static String normalizeFilterValue(SegmentOperator operator, String fieldName, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return rawValue;
        }

        String canonicalField = canonicalizeFieldName(fieldName);
        if (operator == SegmentOperator.IN || operator == SegmentOperator.BETWEEN) {
            String separator =
                    operator == SegmentOperator.BETWEEN && rawValue.contains("..") ? "\\.\\." : ",";
            String joinSeparator =
                    operator == SegmentOperator.BETWEEN && rawValue.contains("..") ? ".." : ",";
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
            throw new IllegalArgumentException(expirationValidationMessage(fieldName));
        }

        String canonicalField = canonicalizeFieldName(fieldName);
        if (operator == SegmentOperator.IN || operator == SegmentOperator.BETWEEN) {
            String separator =
                    operator == SegmentOperator.BETWEEN && rawValue.contains("..") ? "\\.\\." : ",";
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

    static boolean matchesCustomerOwnerships(
            List<ProductOwnership> ownerships,
            SegmentOperator operator,
            String fieldName,
            String normalizedValue,
            LocalDate today) {
        if (!StringUtils.hasText(normalizedValue)) {
            return false;
        }

        List<ProductOwnership> customerOwnerships = ownerships == null ? List.of() : ownerships;
        LocalDate evaluationDate = today == null ? LocalDate.now() : today;

        return switch (canonicalizeFieldName(fieldName)) {
            case "expiring_within_months" ->
                    matchesExpiringWithinMonths(
                            customerOwnerships, operator, normalizedValue, evaluationDate);
            case "expiration_date" ->
                    matchesExpirationDate(
                            customerOwnerships, operator, normalizedValue, evaluationDate);
            case "is_expiring" ->
                    matchesBoolean(
                            hasAnyExpiringWithinMonths(customerOwnerships, 12, evaluationDate),
                            operator,
                            normalizedValue);
            default -> false;
        };
    }

    private static boolean matchesExpiringWithinMonths(
            List<ProductOwnership> ownerships,
            SegmentOperator operator,
            String normalizedValue,
            LocalDate today) {
        return switch (operator) {
            case EQUALS ->
                    hasAnyExpiringWithinMonths(
                            ownerships, Integer.parseInt(normalizedValue), today);
            case NOT_EQUALS ->
                    !hasAnyExpiringWithinMonths(
                            ownerships, Integer.parseInt(normalizedValue), today);
            case IN ->
                    splitInValues(normalizedValue).stream()
                            .map(Integer::parseInt)
                            .anyMatch(months -> hasAnyExpiringWithinMonths(ownerships, months, today));
            case BEFORE ->
                    ownerships.stream()
                            .anyMatch(
                                    ownership ->
                                            earliestMonthsUntilExpiration(ownership, today) >= 0
                                                    && earliestMonthsUntilExpiration(ownership, today)
                                                            < Integer.parseInt(normalizedValue));
            case AFTER ->
                    ownerships.stream()
                            .anyMatch(
                                    ownership ->
                                            earliestMonthsUntilExpiration(ownership, today)
                                                    > Integer.parseInt(normalizedValue));
            case BETWEEN -> matchesMonthsBetween(ownerships, normalizedValue, today);
            default -> false;
        };
    }

    private static boolean matchesMonthsBetween(
            List<ProductOwnership> ownerships, String normalizedValue, LocalDate today) {
        List<String> bounds = splitRangeValues(normalizedValue);
        if (bounds.size() != 2) {
            return false;
        }
        int lower = Integer.parseInt(bounds.get(0));
        int upper = Integer.parseInt(bounds.get(1));
        return ownerships.stream()
                .mapToInt(ownership -> earliestMonthsUntilExpiration(ownership, today))
                .anyMatch(months -> months >= lower && months <= upper);
    }

    private static boolean matchesExpirationDate(
            List<ProductOwnership> ownerships,
            SegmentOperator operator,
            String normalizedValue,
            LocalDate today) {
        List<LocalDate> expirationDates =
                ownerships.stream()
                        .filter(ownership -> isOwnershipActive(ownership, today))
                        .map(ProductOwnership::getExpirationDate)
                        .filter(date -> date != null)
                        .toList();

        if (expirationDates.isEmpty()) {
            return operator == SegmentOperator.NOT_EQUALS;
        }

        return expirationDates.stream()
                .anyMatch(
                        expirationDate ->
                                SegmentCriteria.matchesValue(
                                        operator, normalizedValue, expirationDate.toString()));
    }

    private static boolean matchesBoolean(
            boolean actual, SegmentOperator operator, String normalizedValue) {
        boolean expected = parseBoolean(normalizedValue);
        return switch (operator) {
            case EQUALS -> actual == expected;
            case NOT_EQUALS -> actual != expected;
            default -> false;
        };
    }

    private static boolean hasAnyExpiringWithinMonths(
            List<ProductOwnership> ownerships, int months, LocalDate today) {
        return ownerships.stream()
                .anyMatch(ownership -> isExpiringWithinMonths(ownership, months, today));
    }

    static boolean isExpiringWithinMonths(
            ProductOwnership ownership, int months, LocalDate today) {
        if (ownership == null || months < 0 || ownership.getExpirationDate() == null) {
            return false;
        }
        if (!isOwnershipActive(ownership, today)) {
            return false;
        }

        LocalDate expirationDate = ownership.getExpirationDate();
        LocalDate latestExpiration = today.plusMonths(months);
        return !expirationDate.isBefore(today) && !expirationDate.isAfter(latestExpiration);
    }

    private static boolean isOwnershipActive(ProductOwnership ownership, LocalDate today) {
        return ownership.getStatus() == OwnershipStatus.ACTIVE
                && (ownership.getExpirationDate() == null
                        || !ownership.getExpirationDate().isBefore(today));
    }

    private static int earliestMonthsUntilExpiration(ProductOwnership ownership, LocalDate today) {
        if (ownership == null
                || ownership.getExpirationDate() == null
                || !isOwnershipActive(ownership, today)
                || ownership.getExpirationDate().isBefore(today)) {
            return -1;
        }

        long totalMonths =
                ChronoUnit.MONTHS.between(
                        today.withDayOfMonth(1), ownership.getExpirationDate().withDayOfMonth(1));
        return (int) Math.max(0, totalMonths);
    }

    private static String normalizeSingleFilterValue(String canonicalField, String rawValue) {
        validateSingleFilterValue(canonicalField, rawValue);
        return switch (canonicalField) {
            case "expiring_within_months" -> Integer.toString(parseNonNegativeInt(rawValue));
            case "expiration_date" -> parseLocalDate(rawValue).toString();
            case "is_expiring" -> Boolean.toString(parseBoolean(rawValue));
            default -> rawValue.trim();
        };
    }

    private static void validateSingleFilterValue(String canonicalField, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new IllegalArgumentException(expirationValidationMessage(canonicalField));
        }

        switch (canonicalField) {
            case "expiring_within_months" -> parseNonNegativeInt(rawValue);
            case "expiration_date" -> parseLocalDate(rawValue);
            case "is_expiring" -> parseBoolean(rawValue);
            default ->
                    throw new IllegalArgumentException(expirationValidationMessage(canonicalField));
        }
    }

    private static int parseNonNegativeInt(String rawValue) {
        try {
            int value = Integer.parseInt(rawValue.trim());
            if (value < 0) {
                throw new IllegalArgumentException("must be a non-negative integer number of months");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("must be a non-negative integer number of months");
        }
    }

    private static LocalDate parseLocalDate(String rawValue) {
        try {
            return LocalDate.parse(rawValue.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("must be an ISO-8601 date (YYYY-MM-DD)");
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

    private static List<String> splitRangeValues(String normalizedValue) {
        String separator = normalizedValue.contains("..") ? "\\.\\." : ",";
        return Arrays.stream(normalizedValue.split(separator))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private static String expirationValidationMessage(String fieldName) {
        return "must be a valid "
                + canonicalizeFieldName(fieldName)
                + " value for product expiration segmentation";
    }

    private static String normalizeFieldName(String fieldName) {
        return fieldName.trim().toLowerCase(Locale.ROOT);
    }
}
