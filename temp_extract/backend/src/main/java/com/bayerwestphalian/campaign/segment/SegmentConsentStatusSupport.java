package com.bayerwestphalian.campaign.segment;

import com.bayerwestphalian.campaign.consent.ConsentRecord;
import com.bayerwestphalian.campaign.consent.ConsentStatus;
import com.bayerwestphalian.campaign.consent.ConsentType;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * KB consent segment filter helpers (FR-034 / FR-075 consent dimension / FR-097 opt-out).
 *
 * <p>Supported field names: {@code consent_status} (aliases {@code consentstatus}, {@code
 * consent}), {@code consent_type} (alias {@code consenttype}), {@code
 * has_valid_marketing_consent} (aliases {@code valid_marketing_consent}, {@code
 * marketing_consent}), {@code opt_out} (aliases {@code optout}, {@code marketing_opt_out}, {@code
 * opted_out}), and {@code has_valid_guardian_consent} (aliases {@code valid_guardian_consent},
 * {@code guardian_consent}).
 *
 * <p>Consent statuses: {@code GIVEN}, {@code WITHDRAWN}, {@code REQUIRED}, {@code EXPIRED}, {@code
 * REJECTED}. Consent types: {@code MARKETING_EMAIL}, {@code MARKETING_PHONE}, {@code
 * MARKETING_SMS}, {@code GUARDIAN}, {@code DATA_PROCESSING}. Operators for status/type: EQUALS,
 * NOT_EQUALS, IN. Boolean flags use EQUALS / NOT_EQUALS.
 */
final class SegmentConsentStatusSupport {

    private static final Set<ConsentType> MARKETING_CONSENT_TYPES =
            Set.of(
                    ConsentType.MARKETING_EMAIL,
                    ConsentType.MARKETING_PHONE,
                    ConsentType.MARKETING_SMS);

    private SegmentConsentStatusSupport() {}

    static boolean isConsentStatusField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "consent_status",
                    "consentstatus",
                    "consent",
                    "consent_type",
                    "consenttype",
                    "has_valid_marketing_consent",
                    "valid_marketing_consent",
                    "marketing_consent",
                    "opt_out",
                    "optout",
                    "marketing_opt_out",
                    "marketingoptout",
                    "opted_out",
                    "has_valid_guardian_consent",
                    "valid_guardian_consent",
                    "guardian_consent" -> true;
            default -> false;
        };
    }

    static String canonicalizeFieldName(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return fieldName;
        }

        return switch (normalizeFieldName(fieldName)) {
            case "consentstatus", "consent" -> "consent_status";
            case "consenttype" -> "consent_type";
            case "valid_marketing_consent", "marketing_consent" -> "has_valid_marketing_consent";
            case "optout", "marketing_opt_out", "marketingoptout", "opted_out" -> "opt_out";
            case "valid_guardian_consent", "guardian_consent" -> "has_valid_guardian_consent";
            default -> normalizeFieldName(fieldName);
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
            throw new IllegalArgumentException(consentValidationMessage(fieldName));
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

    static boolean matchesCustomerConsents(
            List<ConsentRecord> consents,
            SegmentOperator operator,
            String fieldName,
            String normalizedValue,
            Instant now) {
        if (!StringUtils.hasText(normalizedValue)) {
            return false;
        }

        List<ConsentRecord> customerConsents = consents == null ? List.of() : consents;
        Instant evaluationTime = now == null ? Instant.now() : now;

        return switch (canonicalizeFieldName(fieldName)) {
            case "consent_status" ->
                    matchesConsentStatus(customerConsents, operator, normalizedValue);
            case "consent_type" -> matchesConsentType(customerConsents, operator, normalizedValue);
            case "has_valid_marketing_consent" ->
                    matchesBoolean(
                            hasValidMarketingConsent(customerConsents, evaluationTime),
                            operator,
                            normalizedValue);
            case "opt_out" ->
                    matchesBoolean(hasMarketingOptOut(customerConsents), operator, normalizedValue);
            case "has_valid_guardian_consent" ->
                    matchesBoolean(
                            hasValidGuardianConsent(customerConsents, evaluationTime),
                            operator,
                            normalizedValue);
            default -> false;
        };
    }

    private static boolean matchesConsentStatus(
            List<ConsentRecord> consents, SegmentOperator operator, String normalizedValue) {
        List<String> statuses =
                consents.stream().map(consent -> consent.getStatus().name()).toList();

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

    private static boolean matchesConsentType(
            List<ConsentRecord> consents, SegmentOperator operator, String normalizedValue) {
        List<String> types =
                consents.stream().map(consent -> consent.getConsentType().name()).toList();

        return switch (operator) {
            case EQUALS -> types.stream().anyMatch(type -> type.equalsIgnoreCase(normalizedValue));
            case NOT_EQUALS ->
                    types.stream().noneMatch(type -> type.equalsIgnoreCase(normalizedValue));
            case IN ->
                    types.stream()
                            .anyMatch(
                                    type ->
                                            splitInValues(normalizedValue).stream()
                                                    .anyMatch(filter -> filter.equalsIgnoreCase(type)));
            default -> false;
        };
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

    private static boolean hasValidMarketingConsent(List<ConsentRecord> consents, Instant now) {
        if (hasMarketingOptOut(consents)) {
            return false;
        }
        return consents.stream()
                .filter(consent -> isMarketingConsentType(consent.getConsentType()))
                .anyMatch(consent -> isValidConsent(consent, now));
    }

    private static boolean hasMarketingOptOut(List<ConsentRecord> consents) {
        return consents.stream()
                .filter(consent -> isMarketingConsentType(consent.getConsentType()))
                .map(ConsentRecord::getStatus)
                .anyMatch(
                        status ->
                                status == ConsentStatus.WITHDRAWN
                                        || status == ConsentStatus.REJECTED);
    }

    private static boolean hasValidGuardianConsent(List<ConsentRecord> consents, Instant now) {
        return consents.stream()
                .filter(consent -> consent.getConsentType() == ConsentType.GUARDIAN)
                .anyMatch(consent -> isValidConsent(consent, now));
    }

    private static boolean isValidConsent(ConsentRecord consent, Instant now) {
        if (consent == null) {
            return false;
        }
        if (consent.getWithdrawnAt() != null) {
            return false;
        }
        return consent.isValid(now);
    }

    private static boolean isMarketingConsentType(ConsentType consentType) {
        return consentType != null && MARKETING_CONSENT_TYPES.contains(consentType);
    }

    private static String normalizeSingleFilterValue(String canonicalField, String rawValue) {
        validateSingleFilterValue(canonicalField, rawValue);
        return switch (canonicalField) {
            case "consent_status" -> parseConsentStatus(rawValue).name();
            case "consent_type" -> parseConsentType(rawValue).name();
            case "has_valid_marketing_consent",
                    "opt_out",
                    "has_valid_guardian_consent" -> Boolean.toString(parseBoolean(rawValue));
            default -> rawValue.trim();
        };
    }

    private static void validateSingleFilterValue(String canonicalField, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new IllegalArgumentException(consentValidationMessage(canonicalField));
        }

        switch (canonicalField) {
            case "consent_status" -> parseConsentStatus(rawValue);
            case "consent_type" -> parseConsentType(rawValue);
            case "has_valid_marketing_consent", "opt_out", "has_valid_guardian_consent" ->
                    parseBoolean(rawValue);
            default -> throw new IllegalArgumentException(consentValidationMessage(canonicalField));
        }
    }

    private static ConsentStatus parseConsentStatus(String rawValue) {
        String normalized = rawValue.trim();
        for (ConsentStatus status : ConsentStatus.values()) {
            if (status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }

        throw new IllegalArgumentException(
                "must be one of GIVEN, WITHDRAWN, REQUIRED, EXPIRED, or REJECTED");
    }

    private static ConsentType parseConsentType(String rawValue) {
        String normalized = rawValue.trim();
        for (ConsentType type : ConsentType.values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "must be one of MARKETING_EMAIL, MARKETING_PHONE, MARKETING_SMS, GUARDIAN,"
                        + " or DATA_PROCESSING");
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

    private static String consentValidationMessage(String fieldName) {
        return "must be a valid "
                + canonicalizeFieldName(fieldName)
                + " value for consent status segmentation";
    }

    private static String normalizeFieldName(String fieldName) {
        return fieldName.trim().toLowerCase(Locale.ROOT);
    }
}
