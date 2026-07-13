package com.bayerwestphalian.campaign.common.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.env.Environment;

/**
 * Validates that required secrets are present and not unsafe placeholders (KB item 543 / Sprint 16
 * critical item 665).
 *
 * <p>Runs on production startup so deployments without JWT/database (and provider) secrets fail
 * before serving traffic. Error messages name secret keys but never include secret values.
 */
public final class SecretPresenceValidator {

    /** Minimum length for production JWT signing secrets. */
    public static final int MIN_JWT_SECRET_LENGTH = 32;

    /** Minimum length for production database passwords. */
    public static final int MIN_DB_PASSWORD_LENGTH = 8;

    public static final Set<String> FORBIDDEN_SECRET_PLACEHOLDERS =
            Set.of(
                    "dev-only-change-me",
                    "changeme",
                    "change-me",
                    "changeit",
                    "secret",
                    "password",
                    "passw0rd",
                    "admin",
                    "default",
                    "todo",
                    "replace-me",
                    "your-secret-here");

    private SecretPresenceValidator() {}

    /**
     * Validates production secrets.
     *
     * @throws IllegalStateException when one or more secrets are missing or unsafe
     */
    public static void validateProductionSecrets(Environment environment) {
        List<String> errors = collectProductionSecretErrors(environment);
        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    "Production secret presence validation failed: " + String.join("; ", errors));
        }
    }

    /**
     * When the active profiles include {@code prod}, runs {@link #validateProductionSecrets}.
     */
    public static void validateIfProduction(Environment environment) {
        if (EnvironmentVariableValidator.isProductionProfile(environment)) {
            validateProductionSecrets(environment);
        }
    }

    /**
     * Collects secret-related problems without throwing. Messages never include secret values.
     */
    public static List<String> collectProductionSecretErrors(Environment environment) {
        List<String> errors = new ArrayList<>();
        if (environment == null) {
            errors.add("environment is required");
            return errors;
        }

        validateSecret(
                environment,
                errors,
                "JWT_SECRET",
                List.of("JWT_SECRET", "app.security.jwt.secret"),
                MIN_JWT_SECRET_LENGTH,
                true);

        validateSecret(
                environment,
                errors,
                "DB_PASSWORD",
                List.of("DB_PASSWORD", "spring.datasource.password"),
                MIN_DB_PASSWORD_LENGTH,
                true);

        rejectReusedJwtAndDatabaseSecret(environment, errors);

        // Provider secrets only when real sending is enabled in production.
        if (isRealSendingEnabled(environment)) {
            String emailMode =
                    firstNonBlank(
                            environment, List.of("EMAIL_PROVIDER_MODE", "app.providers.email.mode"));
            if (emailMode != null && "smtp".equalsIgnoreCase(emailMode)) {
                requireProviderValue(
                        environment,
                        errors,
                        "SMTP_HOST",
                        List.of("SMTP_HOST", "app.providers.email.smtp-host"));
                requirePositivePort(
                        environment,
                        errors,
                        "SMTP_PORT",
                        List.of("SMTP_PORT", "app.providers.email.smtp-port"));
                requireProviderValue(
                        environment,
                        errors,
                        "SMTP_USERNAME",
                        List.of("SMTP_USERNAME", "app.providers.email.smtp-username"));
                validateSecret(
                        environment,
                        errors,
                        "SMTP_PASSWORD",
                        List.of("SMTP_PASSWORD", "app.providers.email.smtp-password"),
                        1,
                        false);
                errors.add(
                        "EMAIL_PROVIDER_MODE=smtp cannot enable real sending until the SMTP "
                                + "placeholder is replaced by a delivery implementation");
            } else if (emailMode != null && !"disabled".equalsIgnoreCase(emailMode)) {
                errors.add(
                        "Production real email sending requires EMAIL_PROVIDER_MODE=disabled "
                                + "until a real provider is implemented");
            }
            String smsMode =
                    firstNonBlank(
                            environment, List.of("SMS_PROVIDER_MODE", "app.providers.sms.mode"));
            if (smsMode != null && "provider".equalsIgnoreCase(smsMode)) {
                validateSecret(
                        environment,
                        errors,
                        "SMS_API_KEY",
                        List.of("SMS_API_KEY", "app.providers.sms.api-key"),
                        8,
                        true);
                errors.add(
                        "SMS_PROVIDER_MODE=provider cannot enable real sending until the SMS "
                                + "placeholder is replaced by a delivery implementation");
            } else if (smsMode != null && !"disabled".equalsIgnoreCase(smsMode)) {
                errors.add(
                        "Production real SMS sending requires SMS_PROVIDER_MODE=disabled until "
                                + "a real provider is implemented");
            }
        }

        return List.copyOf(errors);
    }

    /** Ops checklist of secrets that must be present in production. */
    public static Set<String> productionRequiredSecretNames() {
        Set<String> names = new LinkedHashSet<>();
        names.add("JWT_SECRET");
        names.add("DB_PASSWORD");
        return names;
    }

    static boolean isForbiddenPlaceholder(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (FORBIDDEN_SECRET_PLACEHOLDERS.contains(normalized)) {
            return true;
        }
        // Common patterns without echoing the value.
        return normalized.startsWith("changeme")
                || normalized.startsWith("replace")
                || normalized.contains("your-secret")
                || normalized.contains("example-secret");
    }

    static boolean looksLikeUnresolvedPlaceholder(String value) {
        return EnvironmentVariableValidator.looksLikeUnresolvedPlaceholder(value);
    }

    private static void rejectReusedJwtAndDatabaseSecret(
            Environment environment, List<String> errors) {
        String jwtSecret =
                firstNonBlank(environment, List.of("JWT_SECRET", "app.security.jwt.secret"));
        String databasePassword =
                firstNonBlank(
                        environment, List.of("DB_PASSWORD", "spring.datasource.password"));
        if (jwtSecret != null
                && databasePassword != null
                && !looksLikeUnresolvedPlaceholder(jwtSecret)
                && jwtSecret.equals(databasePassword)) {
            errors.add("JWT_SECRET must be unique and must not reuse DB_PASSWORD");
        }
    }

    private static void validateSecret(
            Environment environment,
            List<String> errors,
            String displayName,
            List<String> propertyKeys,
            int minLength,
            boolean rejectPlaceholders) {
        String value = firstNonBlank(environment, propertyKeys);
        if (value == null) {
            errors.add(
                    displayName
                            + " is required (secret not present; keys: "
                            + String.join(" | ", propertyKeys)
                            + ")");
            return;
        }
        if (looksLikeUnresolvedPlaceholder(value)) {
            errors.add(displayName + " is unresolved (placeholder not substituted)");
            return;
        }
        if (value.isBlank()) {
            errors.add(displayName + " must not be blank");
            return;
        }
        // Prefer placeholder diagnosis over length so weak "changeme..." secrets are explicit.
        if (rejectPlaceholders && isForbiddenPlaceholder(value)) {
            errors.add(displayName + " must not use a known insecure placeholder value");
            return;
        }
        if (value.length() < minLength) {
            errors.add(displayName + " must be at least " + minLength + " characters");
        }
    }

    private static void requireProviderValue(
            Environment environment,
            List<String> errors,
            String displayName,
            List<String> propertyKeys) {
        if (firstNonBlank(environment, propertyKeys) == null) {
            errors.add(displayName + " is required when real email sending is enabled");
        }
    }

    private static void requirePositivePort(
            Environment environment,
            List<String> errors,
            String displayName,
            List<String> propertyKeys) {
        String value = firstNonBlank(environment, propertyKeys);
        if (value == null) {
            errors.add(displayName + " is required when real email sending is enabled");
            return;
        }
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                errors.add(displayName + " must be between 1 and 65535");
            }
        } catch (NumberFormatException exception) {
            errors.add(displayName + " must be between 1 and 65535");
        }
    }

    private static boolean isRealSendingEnabled(Environment environment) {
        String flag =
                firstNonBlank(
                        environment,
                        List.of(
                                "PROVIDER_REAL_SENDING_ENABLED",
                                "app.providers.real-sending-enabled"));
        if (flag == null) {
            return false;
        }
        return "true".equalsIgnoreCase(flag) || "1".equals(flag) || "yes".equalsIgnoreCase(flag);
    }

    private static String firstNonBlank(Environment environment, List<String> propertyKeys) {
        for (String key : propertyKeys) {
            String value = environment.getProperty(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
