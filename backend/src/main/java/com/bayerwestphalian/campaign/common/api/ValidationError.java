package com.bayerwestphalian.campaign.common.api;

public record ValidationError(
        String field, String message, Object rejectedValue, String objectName) {

    public static ValidationError of(
            String field, String message, Object rejectedValue, String objectName) {
        return new ValidationError(field, message, rejectedValue, objectName);
    }
}
