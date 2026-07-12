package com.bayerwestphalian.campaign.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                resourceName + " was not found: " + resourceId);
    }
}
