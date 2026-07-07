package com.bayerwestphalian.campaign.customer;

public record CustomerSearchCriteria(
        String term,
        CustomerType customerType,
        CustomerStatus status,
        String city,
        String country,
        Boolean contactable) {}
