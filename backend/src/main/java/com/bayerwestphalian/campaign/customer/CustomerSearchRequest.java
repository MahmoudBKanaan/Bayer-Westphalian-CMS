package com.bayerwestphalian.campaign.customer;

import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

public record CustomerSearchRequest(
        @Size(max = 255) String term,
        CustomerType customerType,
        CustomerStatus status,
        @Size(max = 100) String city,
        @Size(max = 100) String country,
        Boolean contactable) {

    CustomerSearchCriteria toCriteria() {
        return new CustomerSearchCriteria(
                normalize(term),
                customerType,
                status,
                normalize(city),
                normalize(country),
                contactable);
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
