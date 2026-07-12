package com.bayerwestphalian.campaign.common.api;

import com.bayerwestphalian.campaign.common.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;

/**
 * Server-side API error logging that never writes secrets to logs (KB item 546).
 *
 * <p>Logs structured context (path, method, request id, status, code, exception type) and redacts
 * bearer tokens, passwords, API keys, and other sensitive substrings from free-text messages.
 * Stack traces may still be attached for unexpected errors; messages on those exceptions are
 * sanitized first.
 */
public final class SafeApiErrorLogger {

    public static final String REDACTED = "[REDACTED]";

    private static final Pattern BEARER_TOKEN =
            Pattern.compile("(?i)(bearer\\s+)[a-z0-9\\-._~+/]+=*");
    private static final Pattern JWT_LIKE =
            Pattern.compile("\\beyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\b");
    private static final Pattern JSON_SENSITIVE_FIELD =
            Pattern.compile(
                    "(?i)(\"(?:password|passwd|pwd|secret|token|accessToken|refreshToken|apiKey|api_key|authorization|credential)\"\\s*:\\s*\")([^\"]*)(\")");
    private static final Pattern FORM_SENSITIVE_FIELD =
            Pattern.compile(
                    "(?i)((?:password|passwd|pwd|secret|token|access_token|refresh_token|api[_-]?key|authorization)=)([^&\\s]+)");
    private static final Pattern BASIC_AUTH =
            Pattern.compile("(?i)(basic\\s+)[a-z0-9+/=]+");
    /**
     * Free-text phrases such as {@code Invalid password SuperSecret1! for user}. Requires whitespace
     * after the keyword so JSON {@code "password":"..."} is left to {@link #JSON_SENSITIVE_FIELD}.
     */
    private static final Pattern PASSWORD_IN_TEXT =
            Pattern.compile("(?i)(\\b(?:password|passwd|pwd|secret)\\s+)(\\S+)");

    private SafeApiErrorLogger() {}

    /**
     * Redacts common secret patterns from a log message. Never returns {@code null} (uses empty
     * string).
     */
    public static String sanitizeForLog(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        String sanitized = text;
        sanitized = BEARER_TOKEN.matcher(sanitized).replaceAll("$1" + REDACTED);
        sanitized = BASIC_AUTH.matcher(sanitized).replaceAll("$1" + REDACTED);
        sanitized = JWT_LIKE.matcher(sanitized).replaceAll(REDACTED);
        sanitized = JSON_SENSITIVE_FIELD.matcher(sanitized).replaceAll("$1" + REDACTED + "$3");
        sanitized = FORM_SENSITIVE_FIELD.matcher(sanitized).replaceAll("$1" + REDACTED);
        sanitized = PASSWORD_IN_TEXT.matcher(sanitized).replaceAll("$1" + REDACTED);
        return sanitized;
    }

    /** True when a header name must never be logged with its raw value. */
    public static boolean isSensitiveHeader(String headerName) {
        if (headerName == null || headerName.isBlank()) {
            return false;
        }
        String normalized = headerName.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("authorization")
                || normalized.equals("cookie")
                || normalized.equals("set-cookie")
                || normalized.equals("x-api-key")
                || normalized.equals("proxy-authorization")
                || normalized.contains("token")
                || normalized.contains("secret")
                || normalized.contains("password");
    }

    /**
     * Safe one-line description of a request for logs (method, path, request id only — no headers
     * or body).
     */
    public static String requestContext(HttpServletRequest request) {
        if (request == null) {
            return "method=? path=? requestId=?";
        }
        String method = request.getMethod() == null ? "?" : request.getMethod();
        String path = SecureErrorResponses.requestPath(request);
        if (path == null) {
            path = "?";
        }
        String requestId = SecureErrorResponses.requestId(request);
        if (requestId == null) {
            requestId = "?";
        }
        return "method=" + method + " path=" + path + " requestId=" + requestId;
    }

    /** Logs a mapped application/API error without secrets. */
    public static void logApplicationError(
            Logger log, ApplicationException exception, HttpServletRequest request) {
        if (log == null || exception == null) {
            return;
        }
        int status = exception.getStatus() == null ? 0 : exception.getStatus().value();
        String message = sanitizeForLog(exception.getMessage());
        String context = requestContext(request);
        if (status >= 500) {
            log.error(
                    "API application error code={} status={} {} message={}",
                    exception.getCode(),
                    status,
                    context,
                    message);
        } else if (status == HttpStatus.TOO_MANY_REQUESTS.value()
                || status == HttpStatus.UNAUTHORIZED.value()
                || status == HttpStatus.FORBIDDEN.value()) {
            log.warn(
                    "API security error code={} status={} {} message={}",
                    exception.getCode(),
                    status,
                    context,
                    message);
        } else {
            log.info(
                    "API client error code={} status={} {} message={}",
                    exception.getCode(),
                    status,
                    context,
                    message);
        }
    }

    /** Logs validation failures at debug/info without rejected secret values. */
    public static void logValidationError(
            Logger log, HttpServletRequest request, int errorCount) {
        if (log == null) {
            return;
        }
        log.info(
                "API validation failed {} fieldErrorCount={}",
                requestContext(request),
                errorCount);
    }

    /**
     * Logs unexpected failures. Attaches the throwable for stack traces; sanitizes the message
     * argument so the formatted log line does not embed secrets.
     */
    public static void logUnexpectedError(
            Logger log, Exception exception, HttpServletRequest request) {
        if (log == null || exception == null) {
            return;
        }
        String type = exception.getClass().getName();
        String message = sanitizeForLog(exception.getMessage());
        log.error(
                "Unhandled API exception type={} {} message={}",
                type,
                requestContext(request),
                message,
                exception);
    }

    /** Logs access denied without principal secrets. */
    public static void logAccessDenied(Logger log, HttpServletRequest request) {
        if (log == null) {
            return;
        }
        log.warn("API access denied {}", requestContext(request));
    }
}
