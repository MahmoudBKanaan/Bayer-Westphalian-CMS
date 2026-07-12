package com.bayerwestphalian.campaign.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CommonExceptionTests {

    @Test
    void createsResourceNotFoundException() {
        ResourceNotFoundException exception =
                new ResourceNotFoundException("Customer", "customer-123");

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("Customer was not found: customer-123");
    }

    @Test
    void createsValidationException() {
        ValidationException exception =
                new ValidationException(
                        "Request validation failed",
                        List.of("email is required", "status is invalid"));

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(exception.getDetails())
                .containsExactly("email is required", "status is invalid");
    }

    @Test
    void createsConflictException() {
        ConflictException exception =
                new ConflictException(
                        "DUPLICATE_CAMPAIGN_RECIPIENT", "Customer is already in campaign");

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getCode()).isEqualTo("DUPLICATE_CAMPAIGN_RECIPIENT");
        assertThat(exception.getMessage()).isEqualTo("Customer is already in campaign");
    }

    @Test
    void createsForbiddenException() {
        ForbiddenException exception = new ForbiddenException("Role cannot approve this campaign");

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exception.getCode()).isEqualTo("ACCESS_DENIED");
        assertThat(exception.getMessage()).isEqualTo("Role cannot approve this campaign");
    }

    @Test
    void createsUnauthorizedException() {
        UnauthorizedException exception = new UnauthorizedException("Authentication is required");

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getCode()).isEqualTo("UNAUTHORIZED");
        assertThat(exception.getMessage()).isEqualTo("Authentication is required");
    }

    @Test
    void createsBusinessRuleException() {
        BusinessRuleException exception =
                new BusinessRuleException(
                        "CONSENT_REQUIRED", "Marketing consent is required before contact");

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(exception.getCode()).isEqualTo("CONSENT_REQUIRED");
        assertThat(exception.getMessage())
                .isEqualTo("Marketing consent is required before contact");
    }
}
