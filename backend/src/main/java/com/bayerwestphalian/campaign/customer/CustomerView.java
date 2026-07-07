package com.bayerwestphalian.campaign.customer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerView(
        UUID id,
        CustomerType customerType,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        String addressLine,
        String city,
        String country,
        LocalDate dateOfBirth,
        CustomerAgeGroup ageGroup,
        CustomerStatus status,
        boolean doNotContact,
        boolean active,
        boolean contactable,
        String source,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {

    public static CustomerView from(Customer customer) {
        return new CustomerView(
                customer.getId(),
                customer.getCustomerType(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddressLine(),
                customer.getCity(),
                customer.getCountry(),
                customer.getDateOfBirth(),
                customer.getAgeGroup(),
                customer.getStatus(),
                customer.isDoNotContact(),
                customer.isActive(),
                customer.canBeContacted(),
                customer.getSource(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                customer.getDeletedAt());
    }
}
