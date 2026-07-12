package com.bayerwestphalian.campaign.common.api;

import com.bayerwestphalian.campaign.auth.LoginLockoutException;
import com.bayerwestphalian.campaign.common.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global REST exception mapping to secure {@link ErrorResponse} bodies (KB items 538–539, 546).
 *
 * <p>Client responses use stable codes and safe messages. Unexpected failures never expose stack
 * traces to clients. Server-side logging uses {@link SafeApiErrorLogger} so passwords, tokens, and
 * other secrets are not written to application logs (item 546).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ErrorResponse> handleApplicationException(
            ApplicationException exception, HttpServletRequest request) {
        SafeApiErrorLogger.logApplicationError(log, exception, request);

        ErrorResponse response =
                ErrorResponse.of(
                        exception.getStatus(),
                        exception.getCode(),
                        exception.getMessage(),
                        SecureErrorResponses.requestPath(request),
                        exception.getDetails() == null ? List.of() : exception.getDetails(),
                        SecureErrorResponses.requestId(request));

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(exception.getStatus());
        // Item 544: advertise lockout wait time without leaking account existence beyond the
        // lockout message itself.
        if (exception instanceof LoginLockoutException lockout
                && lockout.getRetryAfterSeconds() != null
                && lockout.getRetryAfterSeconds() > 0) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(lockout.getRetryAfterSeconds()));
        }

        return builder.body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        return validationResponse(
                "Request validation failed",
                exception.getBindingResult().getFieldErrors(),
                request);
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ErrorResponse> handleBindException(
            BindException exception, HttpServletRequest request) {
        return validationResponse(
                "Request validation failed",
                exception.getBindingResult().getFieldErrors(),
                request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<ValidationError> validationErrors =
                exception.getConstraintViolations().stream()
                        .map(
                                violation -> {
                                    String field = violation.getPropertyPath().toString();
                                    return ValidationError.of(
                                            field,
                                            violation.getMessage(),
                                            SecureErrorResponses.sanitizeRejectedValue(
                                                    field, violation.getInvalidValue()),
                                            violation.getRootBeanClass().getSimpleName());
                                })
                        .toList();

        SafeApiErrorLogger.logValidationError(log, request, validationErrors.size());

        ErrorResponse response =
                ErrorResponse.validation(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        SecureErrorResponses.requestPath(request),
                        validationErrors,
                        SecureErrorResponses.requestId(request));

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        SafeApiErrorLogger.logValidationError(log, request, 1);
        ErrorResponse response =
                ErrorResponse.validation(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        SecureErrorResponses.requestPath(request),
                        List.of(
                                ValidationError.of(
                                        exception.getParameterName(),
                                        "Required request parameter is missing",
                                        null,
                                        exception.getParameterType())),
                        SecureErrorResponses.requestId(request));

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        SafeApiErrorLogger.logValidationError(log, request, 1);
        ErrorResponse response =
                ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        SecureErrorResponses.requestPath(request),
                        List.of(exception.getName() + ": invalid value"),
                        SecureErrorResponses.requestId(request));

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        // Do not log the raw body — it may contain passwords or tokens (item 546).
        log.debug(
                "Unreadable request body for {}", SafeApiErrorLogger.requestContext(request));
        ErrorResponse response =
                ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        "MALFORMED_REQUEST",
                        "Request body is malformed or unreadable",
                        SecureErrorResponses.requestPath(request),
                        List.of(),
                        SecureErrorResponses.requestId(request));

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        log.info(
                "API method not allowed {} method={}",
                SafeApiErrorLogger.requestContext(request),
                exception.getMethod());
        ErrorResponse response =
                ErrorResponse.of(
                        HttpStatus.METHOD_NOT_ALLOWED,
                        "METHOD_NOT_ALLOWED",
                        "HTTP method is not allowed for this endpoint",
                        SecureErrorResponses.requestPath(request),
                        List.of(),
                        SecureErrorResponses.requestId(request));

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        SafeApiErrorLogger.logAccessDenied(log, request);
        ErrorResponse response =
                ErrorResponse.of(
                        HttpStatus.FORBIDDEN,
                        "ACCESS_DENIED",
                        "Role is not allowed to perform this action",
                        SecureErrorResponses.requestPath(request),
                        List.of(),
                        SecureErrorResponses.requestId(request));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    ResponseEntity<ErrorResponse> handleAuthenticationMissing(
            AuthenticationCredentialsNotFoundException exception, HttpServletRequest request) {
        log.warn("API authentication required {}", SafeApiErrorLogger.requestContext(request));
        ErrorResponse response =
                ErrorResponse.of(
                        HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED",
                        "Authentication is required",
                        SecureErrorResponses.requestPath(request),
                        List.of(),
                        SecureErrorResponses.requestId(request));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception, HttpServletRequest request) {
        SafeApiErrorLogger.logUnexpectedError(log, exception, request);
        String path = SecureErrorResponses.requestPath(request);
        String requestId = SecureErrorResponses.requestId(request);
        ErrorResponse response =
                ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_ERROR",
                        "Unexpected server error",
                        path,
                        List.of(),
                        requestId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private static ResponseEntity<ErrorResponse> validationResponse(
            String message, List<FieldError> fieldErrors, HttpServletRequest request) {
        SafeApiErrorLogger.logValidationError(log, request, fieldErrors == null ? 0 : fieldErrors.size());
        List<ValidationError> validationErrors =
                fieldErrors.stream()
                        .map(
                                error ->
                                        ValidationError.of(
                                                error.getField(),
                                                error.getDefaultMessage(),
                                                SecureErrorResponses.sanitizeRejectedValue(
                                                        error.getField(), error.getRejectedValue()),
                                                error.getObjectName()))
                        .toList();
        ErrorResponse response =
                ErrorResponse.validation(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_FAILED",
                        message,
                        SecureErrorResponses.requestPath(request),
                        validationErrors,
                        SecureErrorResponses.requestId(request));

        return ResponseEntity.badRequest().body(response);
    }
}
