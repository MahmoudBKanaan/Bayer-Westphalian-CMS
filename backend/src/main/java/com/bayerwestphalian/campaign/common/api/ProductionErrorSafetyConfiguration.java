package com.bayerwestphalian.campaign.common.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.request.WebRequest;

/**
 * Production-only error attribute hardening (KB item 539 / Sprint 16 critical item 664).
 *
 * <p>When the {@code prod} profile is active, Spring Boot container/error-page attributes never
 * include stack traces, exception class names, binding dumps, or exception messages — even if
 * misconfiguration tried to enable {@code server.error.include-*} flags. API controller errors
 * continue to use {@link GlobalExceptionHandler} / {@link SecureErrorResponses} (item 538).
 */
@Configuration
@Profile("prod")
public class ProductionErrorSafetyConfiguration {

    private static final Set<String> FORBIDDEN_CLIENT_KEYS =
            Set.of(
                    "trace",
                    "stackTrace",
                    "stacktrace",
                    "exception",
                    "message",
                    "errors",
                    "bindingErrors",
                    "path");

    /**
     * Safe subset of keys that may appear on production container error attributes.
     *
     * <p>{@code path} is intentionally omitted here for container errors to reduce information
     * disclosure; REST {@link ErrorResponse} from the controller advice still includes path.
     */
    private static final Set<String> ALLOWED_CLIENT_KEYS =
            Set.of("timestamp", "status", "error", "requestId");

    @Bean
    ErrorAttributes productionSafeErrorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(
                    WebRequest webRequest, ErrorAttributeOptions options) {
                // Force minimal includes regardless of server.error.* property overrides.
                // Note: TIMESTAMP is not an ErrorAttributeOptions.Include value in Spring Boot 3.5;
                // DefaultErrorAttributes still supplies "timestamp" by default.
                ErrorAttributeOptions safeOptions =
                        ErrorAttributeOptions.of(
                                ErrorAttributeOptions.Include.STATUS,
                                ErrorAttributeOptions.Include.ERROR);
                Map<String, Object> attributes =
                        new LinkedHashMap<>(super.getErrorAttributes(webRequest, safeOptions));
                return sanitizeForProduction(attributes);
            }
        };
    }

    /**
     * Removes any residual stack-trace / exception fields from an error attribute map.
     *
     * <p>Package-visible for unit tests (item 539).
     */
    static Map<String, Object> sanitizeForProduction(Map<String, Object> attributes) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        if (attributes == null) {
            return sanitized;
        }
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            String normalized = key.trim();
            if (FORBIDDEN_CLIENT_KEYS.contains(normalized)
                    || normalized.equalsIgnoreCase("trace")
                    || normalized.equalsIgnoreCase("stackTrace")
                    || normalized.equalsIgnoreCase("exception")) {
                continue;
            }
            if (ALLOWED_CLIENT_KEYS.contains(normalized)
                    || "status".equals(normalized)
                    || "error".equals(normalized)
                    || "timestamp".equals(normalized)) {
                sanitized.put(normalized, entry.getValue());
            }
        }
        return sanitized;
    }

    /** True when a key must never appear in production client-facing error attributes. */
    static boolean isForbiddenClientErrorKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = key.trim();
        return FORBIDDEN_CLIENT_KEYS.contains(normalized)
                || "trace".equalsIgnoreCase(normalized)
                || "stackTrace".equalsIgnoreCase(normalized)
                || "exception".equalsIgnoreCase(normalized)
                || "message".equalsIgnoreCase(normalized);
    }
}
