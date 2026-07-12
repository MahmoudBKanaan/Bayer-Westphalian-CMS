package com.bayerwestphalian.campaign.customer;

import java.time.LocalDate;

public record UpdateCustomerCommand(
        String firstName,
        String lastName,
        String email,
        String phone,
        String addressLine,
        String city,
        String country,
        LocalDate dateOfBirth,
        CustomerAgeGroup ageGroup,
        CustomerStatus status,
        Boolean doNotContact,
        String source) {}
