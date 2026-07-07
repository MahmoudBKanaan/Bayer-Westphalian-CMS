package com.bayerwestphalian.campaign.customer;

import java.util.List;

public record CustomerImportResult(
        int importedCount,
        int failedCount,
        List<CustomerView> customers,
        List<CustomerImportError> errors) {

    public CustomerImportResult {
        customers = customers == null ? List.of() : List.copyOf(customers);
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
