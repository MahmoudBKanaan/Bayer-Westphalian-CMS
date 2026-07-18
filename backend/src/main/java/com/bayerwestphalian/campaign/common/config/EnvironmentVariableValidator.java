package com.bayerwestphalian.campaign.common.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.env.Environment;

/**
 * Validates required environment / Spring properties (KB item 542).
 *
 * <p>Production must fail fast with clear configuration errors when mandatory deployment variables
 * are missing, blank, unresolved placeholders, or obviously unsafe. Dedicated secret presence and
 * strength rules live in {@link SecretPresenceValidator} (item 543); this class still rejects known
 * short/dev JWT placeholders for early feedback.
 */
public final class EnvironmentVariableValidator {

    /** Known insecure JWT defaults that must never be used in production. */
    public static final Set<String> FORBIDDEN_JWT_SECRET_VALUES =
            Set.of("dev-only-change-me", "changeme", "change-me", "secret", "password");

    private EnvironmentVariableValidator() {}

    public static boolean isProductionProfile(Environment environment) {
        if (environment == null) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile));
    }

    /**
     * Validates production deployment configuration.
     *
     * @throws IllegalStateException when one or more required variables are invalid
     */
    public static void validateProduction(Environment environment) {
        List<String> errors = collectProductionErrors(environment);
        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    "Production environment variable validation failed: "
                            + String.join("; ", errors));
        }
    }

    /**
     * Collects human-readable validation problems without throwing (for tests / diagnostics).
     *
     * <p>Error messages never include secret values.
     */
    public static List<String> collectProductionErrors(Environment environment) {
        List<String> errors = new ArrayList<>();
        if (environment == null) {
            errors.add("environment is required");
            return errors;
        }

        requireResolved(
                environment,
                errors,
                "DB_URL",
                List.of("DB_URL", "spring.datasource.url"),
                value -> {
                    return validateProductionDatabaseUrl(value);
                });

        requireResolved(
                environment,
                errors,
                "DB_USERNAME",
                List.of("DB_USERNAME", "spring.datasource.username"),
                null);

        requireResolved(
                environment,
                errors,
                "DB_PASSWORD",
                List.of("DB_PASSWORD", "spring.datasource.password"),
                null);

        requireResolved(
                environment,
                errors,
                "JWT_SECRET",
                List.of("JWT_SECRET", "app.security.jwt.secret"),
                value -> {
                    if (FORBIDDEN_JWT_SECRET_VALUES.contains(value.toLowerCase(Locale.ROOT))) {
                        return "JWT_SECRET / app.security.jwt.secret must not use a known development placeholder";
                    }
                    if (value.length() < SecretPresenceValidator.MIN_JWT_SECRET_LENGTH) {
                        return "JWT secret presence check failed: JWT_SECRET / app.security.jwt.secret must be at least "
                                + SecretPresenceValidator.MIN_JWT_SECRET_LENGTH
                                + " characters";
                    }
                    return null;
                });

        requireResolved(
                environment,
                errors,
                "CORS_ALLOWED_ORIGINS",
                List.of("CORS_ALLOWED_ORIGINS", "app.cors.allowed-origins"),
                value -> {
                    try {
                        ProductionCorsOrigins.parseAndValidate(value);
                    } catch (IllegalStateException exception) {
                        return "CORS_ALLOWED_ORIGINS: " + exception.getMessage();
                    }
                    return null;
                });

        // Optional numeric checks when present (prod YAML requires these without defaults).
        requirePositiveIntegerIfPresent(
                environment,
                errors,
                "LOGIN_RATE_LIMIT_MAX_FAILURES",
                List.of("LOGIN_RATE_LIMIT_MAX_FAILURES", "app.security.login-rate-limit.max-failures"));
        requirePositiveIntegerIfPresent(
                environment,
                errors,
                "LOGIN_RATE_LIMIT_FAILURE_WINDOW_MINUTES",
                List.of(
                        "LOGIN_RATE_LIMIT_FAILURE_WINDOW_MINUTES",
                        "app.security.login-rate-limit.failure-window-minutes"));
        requirePositiveIntegerIfPresent(
                environment,
                errors,
                "LOGIN_RATE_LIMIT_LOCKOUT_MINUTES",
                List.of(
                        "LOGIN_RATE_LIMIT_LOCKOUT_MINUTES",
                        "app.security.login-rate-limit.lockout-minutes"));

        return List.copyOf(errors);
    }

    /**
     * When the active profiles include {@code prod}, runs {@link #validateProduction(Environment)}.
     * No-op for other profiles.
     */
    public static void validateIfProduction(Environment environment) {
        if (isProductionProfile(environment)) {
            validateProduction(environment);
        }
    }

    @FunctionalInterface
    interface ValueConstraint {
        /** @return error message or {@code null} when valid */
        String validate(String value);
    }

    private static void requireResolved(
            Environment environment,
            List<String> errors,
            String displayName,
            List<String> propertyKeys,
            ValueConstraint constraint) {
        String value = firstNonBlank(environment, propertyKeys);
        if (value == null) {
            errors.add(displayName + " is required (keys: " + String.join(" | ", propertyKeys) + ")");
            return;
        }
        if (looksLikeUnresolvedPlaceholder(value)) {
            errors.add(displayName + " is unresolved (placeholder not substituted)");
            return;
        }
        if (constraint != null) {
            String constraintError = constraint.validate(value);
            if (constraintError != null) {
                errors.add(constraintError);
            }
        }
    }

    private static void requirePositiveIntegerIfPresent(
            Environment environment,
            List<String> errors,
            String displayName,
            List<String> propertyKeys) {
        String value = firstNonBlank(environment, propertyKeys);
        if (value == null || looksLikeUnresolvedPlaceholder(value)) {
            // Missing optional-with-prod-required vars: if any key is configured as required in
            // prod YAML without default, Spring may fail earlier; still flag unresolved placeholders.
            if (value != null && looksLikeUnresolvedPlaceholder(value)) {
                errors.add(displayName + " is unresolved (placeholder not substituted)");
            }
            return;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 1) {
                errors.add(displayName + " must be a positive integer");
            }
        } catch (NumberFormatException ex) {
            errors.add(displayName + " must be a positive integer");
        }
    }

    private static String firstNonBlank(Environment environment, List<String> propertyKeys) {
        if (environment == null || propertyKeys == null || propertyKeys.isEmpty()) {
            return null;
        }
        for (String key : propertyKeys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            try {
                String value = environment.getProperty(key);
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            } catch (RuntimeException ex) {
                // Spring may throw PlaceholderResolutionException for circular/unresolved
                // placeholders such as DB_URL=${DB_URL}. Surface a synthetic placeholder so
                // looksLikeUnresolvedPlaceholder can flag the variable safely.
                return "${" + key + "}";
            }
        }
        return null;
    }

    static boolean looksLikeUnresolvedPlaceholder(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("${") && trimmed.endsWith("}");
    }

    static String validateProductionDatabaseUrl(String value) {
        if (value == null
                || !value.toLowerCase(Locale.ROOT).startsWith("jdbc:postgresql://")) {
            return "DB_URL / spring.datasource.url must be a PostgreSQL JDBC URL "
                    + "(jdbc:postgresql://...)";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        String authorityAndPath = value.substring("jdbc:postgresql://".length());
        int pathStart = authorityAndPath.indexOf('/');
        String authority =
                pathStart >= 0 ? authorityAndPath.substring(0, pathStart) : authorityAndPath;
        if (authority.contains("@")
                || lower.matches(".*[?&](user|username|password)=[^&]*.*")) {
            return "DB_URL must not embed database credentials; use DB_USERNAME and DB_PASSWORD";
        }
        if (authority.isBlank() || pathStart < 0 || pathStart == authorityAndPath.length() - 1) {
            return "DB_URL must include a database host and database name";
        }
        return null;
    }

    /** Required production environment variable names (documentation / ops checklist). */
    public static Set<String> productionRequiredEnvNames() {
        Set<String> names = new LinkedHashSet<>();
        names.add("DB_URL");
        names.add("DB_USERNAME");
        names.add("DB_PASSWORD");
        names.add("JWT_SECRET");
        names.add("CORS_ALLOWED_ORIGINS");
        return names;
    }
}
