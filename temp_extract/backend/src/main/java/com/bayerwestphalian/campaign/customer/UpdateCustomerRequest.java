package com.bayerwestphalian.campaign.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateCustomerRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Email @Size(max = 255) String email,
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,50}$") @Size(max = 50) String phone,
        @Size(max = 255) String addressLine,
        @Size(max = 100) String city,
        @Size(max = 100) String country,
        @PastOrPresent LocalDate dateOfBirth,
        CustomerAgeGroup ageGroup,
        CustomerStatus status,
        Boolean doNotContact,
        @Size(max = 100) String source) {

    UpdateCustomerCommand toCommand() {
        return new UpdateCustomerCommand(
                firstName,
                lastName,
                email,
                phone,
                addressLine,
                city,
                country,
                dateOfBirth,
                ageGroup,
                status,
                doNotContact,
                source);
    }
}
