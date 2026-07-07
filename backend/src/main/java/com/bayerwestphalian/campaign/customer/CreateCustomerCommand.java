package com.bayerwestphalian.campaign.customer;

import java.time.LocalDate;

public record CreateCustomerCommand(
        CustomerType customerType,
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
        boolean doNotContact,
        String source) {}
