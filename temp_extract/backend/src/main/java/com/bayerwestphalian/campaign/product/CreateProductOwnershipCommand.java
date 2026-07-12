package com.bayerwestphalian.campaign.product;

import java.time.LocalDate;
import java.util.UUID;

public record CreateProductOwnershipCommand(
        UUID customerId,
        UUID productId,
        LocalDate startDate,
        LocalDate expirationDate,
        String policyNumber) {}