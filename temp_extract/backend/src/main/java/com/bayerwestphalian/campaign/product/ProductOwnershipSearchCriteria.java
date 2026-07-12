package com.bayerwestphalian.campaign.product;

import java.time.LocalDate;
import java.util.UUID;

public record ProductOwnershipSearchCriteria(
        UUID customerId,
        UUID productId,
        OwnershipStatus status,
        LocalDate expiringFrom,
        LocalDate expiringTo) {}