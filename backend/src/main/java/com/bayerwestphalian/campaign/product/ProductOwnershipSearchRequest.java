package com.bayerwestphalian.campaign.product;

import java.time.LocalDate;
import java.util.UUID;

public record ProductOwnershipSearchRequest(
        UUID customerId,
        UUID productId,
        OwnershipStatus status,
        LocalDate expiringFrom,
        LocalDate expiringTo) {

    ProductOwnershipSearchCriteria toCriteria() {
        return new ProductOwnershipSearchCriteria(
                customerId, productId, status, expiringFrom, expiringTo);
    }
}
