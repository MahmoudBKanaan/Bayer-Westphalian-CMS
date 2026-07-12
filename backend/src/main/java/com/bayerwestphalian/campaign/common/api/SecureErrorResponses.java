package com.bayerwestphalian.campaign.common.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Writes KB-aligned secure API error JSON (item 538).
 *
 * <p>Responses never include stack traces, exception class names, or raw internal messages for
 * auth/filter failures. Sensitive validation field values are redacted.
 *
 * <p>Registered as a Spring bean from {@code SecurityConfiguration} so WebMvc security tests that
 * import the security stack receive the writer without extra {@code @Import} entries.
 */
public class SecureErrorResponses {

    private static final Set<String> SENSITIVE_FIELD_MARKERS =
            Set.of(
                    "password",
                    "secret",
                    "token",
                    "authorization",
                    "apikey",
                    "api_key",
                    "accesstoken",
                    "refreshtoken",
                    "credential",
                    "privatekey",
                    "private_key");

    public static final String REDACTED_VALUE = "[REDACTED]";

    private final ObjectMapper objectMapper;

    public SecureErrorResponses(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Writes a structured {@link ErrorResponse} body with secure defaults (no stack traces).
     */
    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        ErrorResponse body =
                ErrorResponse.of(
                        status,
                        code,
                        message,
                        requestPath(request),
                        List.of(),
                        requestId(request));
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    public static String requestId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String requestId = request.getHeader("X-Request-Id");
        return requestId == null || requestId.isBlank() ? null : requestId.trim();
    }

    public static String requestPath(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String uri = request.getRequestURI();
        return uri == null || uri.isBlank() ? null : uri;
    }

    /**
     * Returns a safe rejected-value representation for validation errors. Sensitive field names
     * never echo the submitted value back to the client.
     */
    public static Object sanitizeRejectedValue(String field, Object rejectedValue) {
        if (rejectedValue == null) {
            return null;
        }
        if (isSensitiveField(field)) {
            return REDACTED_VALUE;
        }
        return rejectedValue;
    }

    public static boolean isSensitiveField(String field) {
        if (field == null || field.isBlank()) {
            return false;
        }
        String normalized =
                field.toLowerCase(Locale.ROOT)
                        .replace('-', '_')
                        .replace('.', '_')
                        .replace('[', '_')
                        .replace(']', '_');
        for (String marker : SENSITIVE_FIELD_MARKERS) {
            if (normalized.contains(marker)) {
                return true;
            }
        }
        return false;
    }
}
