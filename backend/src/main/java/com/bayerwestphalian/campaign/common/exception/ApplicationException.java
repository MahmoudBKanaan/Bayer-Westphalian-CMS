package com.bayerwestphalian.campaign.common.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ApplicationException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<String> details;

    public ApplicationException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of(), null);
    }

    public ApplicationException(
            HttpStatus status, String code, String message, List<String> details) {
        this(status, code, message, details, null);
    }

    public ApplicationException(
            HttpStatus status, String code, String message, List<String> details, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<String> getDetails() {
        return details;
    }
}
