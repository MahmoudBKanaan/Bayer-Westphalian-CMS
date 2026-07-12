package com.bayerwestphalian.campaign.common.api;

import com.bayerwestphalian.campaign.common.exception.ApplicationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ErrorResponse> handleApplicationException(
            ApplicationException exception, HttpServletRequest request) {
        ErrorResponse response =
                ErrorResponse.of(
                        exception.getStatus(),
                        exception.getCode(),
                        exception.getMessage(),
                        request.getRequestURI(),
                        exception.getDetails(),
                        requestId(request));

        return ResponseEntity.status(exception.getStatus()).body(response);
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
                                violation ->
                                        ValidationError.of(
                                                violation.getPropertyPath().toString(),
                                                violation.getMessage(),
                                                violation.getInvalidValue(),
                                                violation.getRootBeanClass().getSimpleName()))
                        .toList();

        ErrorResponse response =
                ErrorResponse.validation(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        request.getRequestURI(),
                        validationErrors,
                        requestId(request));

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> handleMissingRequestParameter(
            MissingServletRequestParameterException exception, HttpServletRequest request) {
        ErrorResponse response =
                ErrorResponse.validation(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        request.getRequestURI(),
                        List.of(
                                ValidationError.of(
                                        exception.getParameterName(),
                                        "Required request parameter is missing",
                                        null,
                                        exception.getParameterType())),
                        requestId(request));

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException exception, HttpServletRequest request) {
        ErrorResponse response =
                ErrorResponse.of(
                        HttpStatus.FORBIDDEN,
                        "ACCESS_DENIED",
                        "Role is not allowed to perform this action",
                        request.getRequestURI(),
                        List.of(),
                        requestId(request));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    ResponseEntity<ErrorResponse> handleAuthenticationMissing(
            AuthenticationCredentialsNotFoundException exception, HttpServletRequest request) {
        ErrorResponse response =
                ErrorResponse.of(
                        HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED",
                        "Authentication is required",
                        request.getRequestURI(),
                        List.of(),
                        requestId(request));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception, HttpServletRequest request) {
        ErrorResponse response =
                ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_ERROR",
                        "Unexpected server error",
                        request.getRequestURI(),
                        List.of(),
                        requestId(request));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private static ResponseEntity<ErrorResponse> validationResponse(
            String message, List<FieldError> fieldErrors, HttpServletRequest request) {
        List<ValidationError> validationErrors =
                fieldErrors.stream()
                        .map(
                                error ->
                                        ValidationError.of(
                                                error.getField(),
                                                error.getDefaultMessage(),
                                                error.getRejectedValue(),
                                                error.getObjectName()))
                        .toList();
        ErrorResponse response =
                ErrorResponse.validation(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_FAILED",
                        message,
                        request.getRequestURI(),
                        validationErrors,
                        requestId(request));

        return ResponseEntity.badRequest().body(response);
    }

    private static String requestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        return requestId == null || requestId.isBlank() ? null : requestId;
    }
}
