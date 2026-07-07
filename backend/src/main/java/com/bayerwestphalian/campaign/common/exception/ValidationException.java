package com.bayerwestphalian.campaign.common.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ValidationException extends ApplicationException {

    public ValidationException(String message, List<String> details) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, details);
    }
}
