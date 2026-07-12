package com.bayerwestphalian.campaign.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends ApplicationException {

    public BusinessRuleException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
