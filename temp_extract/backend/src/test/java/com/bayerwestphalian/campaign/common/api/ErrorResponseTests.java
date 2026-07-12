package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorResponseTests {

    @Test
    void createsErrorResponseFromHttpStatus() {
        ErrorResponse response =
                ErrorResponse.of(
                        HttpStatus.NOT_FOUND,
                        "CUSTOMER_NOT_FOUND",
                        "Customer was not found",
                        "/api/customers/123");

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.error()).isEqualTo("Not Found");
        assertThat(response.code()).isEqualTo("CUSTOMER_NOT_FOUND");
        assertThat(response.message()).isEqualTo("Customer was not found");
        assertThat(response.path()).isEqualTo("/api/customers/123");
        assertThat(response.details()).isEmpty();
        assertThat(response.validationErrors()).isEmpty();
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.requestId()).isNull();
    }

    @Test
    void createsValidationErrorResponseWithDetailsAndRequestId() {
        ErrorResponse response =
                ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        "/api/customers",
                        List.of("email is invalid", "status is required"),
                        "request-123");

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.error()).isEqualTo("Bad Request");
        assertThat(response.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.message()).isEqualTo("Request validation failed");
        assertThat(response.path()).isEqualTo("/api/customers");
        assertThat(response.details()).containsExactly("email is invalid", "status is required");
        assertThat(response.validationErrors()).isEmpty();
        assertThat(response.requestId()).isEqualTo("request-123");
    }

    @Test
    void normalizesNullDetailsAndTimestamp() {
        ErrorResponse response =
                new ErrorResponse(
                        500,
                        "Internal Server Error",
                        "INTERNAL_ERROR",
                        "Unexpected error",
                        "/api/campaigns",
                        null,
                        null,
                        null);

        assertThat(response.details()).isEmpty();
        assertThat(response.validationErrors()).isEmpty();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void createsValidationErrorResponseWithTypedValidationErrors() {
        ErrorResponse response =
                ErrorResponse.validation(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        "/api/customers",
                        List.of(
                                ValidationError.of(
                                        "email", "must be valid", "bad-email", "customer")),
                        "request-123");

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.details()).containsExactly("email: must be valid");
        assertThat(response.validationErrors()).hasSize(1);
        assertThat(response.validationErrors().get(0).field()).isEqualTo("email");
        assertThat(response.validationErrors().get(0).message()).isEqualTo("must be valid");
        assertThat(response.validationErrors().get(0).rejectedValue()).isEqualTo("bad-email");
        assertThat(response.validationErrors().get(0).objectName()).isEqualTo("customer");
    }

    @Test
    void preservesExplicitTimestamp() {
        Instant timestamp = Instant.parse("2026-07-03T12:00:00Z");

        ErrorResponse response =
                new ErrorResponse(
                        403,
                        "Forbidden",
                        "ACCESS_DENIED",
                        "Role is not allowed",
                        "/api/users",
                        List.of(),
                        timestamp,
                        "request-456");

        assertThat(response.timestamp()).isEqualTo(timestamp);
    }

    @Test
    void copiesDetailsDefensively() {
        List<String> details = new ArrayList<>();
        details.add("initial detail");

        ErrorResponse response =
                ErrorResponse.of(
                        HttpStatus.CONFLICT,
                        "DUPLICATE_CAMPAIGN_RECIPIENT",
                        "Recipient already exists",
                        "/api/campaigns/1/recipients",
                        details,
                        "request-789");
        details.add("later mutation");

        assertThat(response.details()).containsExactly("initial detail");
        assertThatThrownBy(() -> response.details().add("not allowed"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void copiesValidationErrorsDefensively() {
        List<ValidationError> validationErrors = new ArrayList<>();
        validationErrors.add(ValidationError.of("status", "is invalid", "BROKEN", "campaign"));

        ErrorResponse response =
                ErrorResponse.validation(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        "/api/campaigns",
                        validationErrors,
                        null);
        validationErrors.add(ValidationError.of("name", "is required", null, "campaign"));

        assertThat(response.validationErrors()).hasSize(1);
        assertThatThrownBy(
                        () ->
                                response.validationErrors()
                                        .add(ValidationError.of("x", "y", null, "z")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void normalizesNullValidationErrors() {
        ErrorResponse response =
                ErrorResponse.validation(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_FAILED",
                        "Request validation failed",
                        "/api/customers",
                        null,
                        null);

        assertThat(response.details()).isEmpty();
        assertThat(response.validationErrors()).isEmpty();
    }
}
