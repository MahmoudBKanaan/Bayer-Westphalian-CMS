package com.bayerwestphalian.campaign.common.api;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

public record ErrorResponse(
        int status,
        String error,
        String code,
        String message,
        String path,
        List<String> details,
        List<ValidationError> validationErrors,
        Instant timestamp,
        String requestId) {

    public ErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
        validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public ErrorResponse(
            int status,
            String error,
            String code,
            String message,
            String path,
            List<String> details,
            Instant timestamp,
            String requestId) {
        this(status, error, code, message, path, details, List.of(), timestamp, requestId);
    }

    public static ErrorResponse of(HttpStatus status, String code, String message, String path) {
        return of(status, code, message, path, List.of(), null);
    }

    public static ErrorResponse of(
            HttpStatus status,
            String code,
            String message,
            String path,
            List<String> details,
            String requestId) {
        return new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                details,
                List.of(),
                Instant.now(),
                requestId);
    }

    public static ErrorResponse validation(
            HttpStatus status,
            String code,
            String message,
            String path,
            List<ValidationError> validationErrors,
            String requestId) {
        List<ValidationError> normalizedValidationErrors =
                validationErrors == null ? List.of() : List.copyOf(validationErrors);

        return new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                normalizedValidationErrors.stream()
                        .map(error -> error.field() + ": " + error.message())
                        .toList(),
                normalizedValidationErrors,
                Instant.now(),
                requestId);
    }
}
