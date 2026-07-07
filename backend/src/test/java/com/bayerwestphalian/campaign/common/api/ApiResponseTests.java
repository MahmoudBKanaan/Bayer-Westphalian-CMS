package com.bayerwestphalian.campaign.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiResponseTests {

    @Test
    void createsSuccessfulResponseWithDefaultMessage() {
        ApiResponse<String> response = ApiResponse.success("payload");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Request completed successfully");
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.errors()).isEmpty();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void createsSuccessfulResponseWithCustomMessage() {
        ApiResponse<Integer> response = ApiResponse.success("Customer loaded", 42);

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Customer loaded");
        assertThat(response.data()).isEqualTo(42);
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void createsErrorResponseWithMessageAsDefaultError() {
        ApiResponse<Object> response = ApiResponse.error("Validation failed");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Validation failed");
        assertThat(response.data()).isNull();
        assertThat(response.errors()).containsExactly("Validation failed");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void createsErrorResponseWithMultipleErrors() {
        ApiResponse<Object> response =
                ApiResponse.error(
                        "Request validation failed",
                        List.of("email is required", "status is invalid"));

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Request validation failed");
        assertThat(response.data()).isNull();
        assertThat(response.errors()).containsExactly("email is required", "status is invalid");
    }

    @Test
    void normalizesNullErrorsAndTimestamp() {
        ApiResponse<String> response = new ApiResponse<>(true, "OK", "payload", null, null);

        assertThat(response.errors()).isEmpty();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void preservesExplicitTimestamp() {
        Instant timestamp = Instant.parse("2026-07-03T12:00:00Z");

        ApiResponse<String> response =
                new ApiResponse<>(true, "OK", "payload", List.of(), timestamp);

        assertThat(response.timestamp()).isEqualTo(timestamp);
    }

    @Test
    void copiesErrorsDefensively() {
        List<String> errors = new ArrayList<>();
        errors.add("initial error");

        ApiResponse<Object> response = ApiResponse.error("Failed", errors);
        errors.add("later mutation");

        assertThat(response.errors()).containsExactly("initial error");
        assertThatThrownBy(() -> response.errors().add("not allowed"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
