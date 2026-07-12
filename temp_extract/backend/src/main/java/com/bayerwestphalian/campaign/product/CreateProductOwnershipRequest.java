package com.bayerwestphalian.campaign.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateProductOwnershipRequest(
        @NotNull UUID customerId,
        @NotNull UUID productId,
        @NotNull LocalDate startDate,
        LocalDate expirationDate,
        @Size(max = 100) String policyNumber) {

    CreateProductOwnershipCommand toCommand() {
        return new CreateProductOwnershipCommand(
                customerId, productId, startDate, expirationDate, policyNumber);
    }
}