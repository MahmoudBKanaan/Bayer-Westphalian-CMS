package com.bayerwestphalian.campaign.common.api;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
        boolean success, String message, T data, List<String> errors, Instant timestamp) {

    public ApiResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
        timestamp = timestamp == null ? Instant.now() : timestamp;
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Request completed successfully", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, List.of(), Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(message, List.of(message));
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return new ApiResponse<>(false, message, null, errors, Instant.now());
    }
}
