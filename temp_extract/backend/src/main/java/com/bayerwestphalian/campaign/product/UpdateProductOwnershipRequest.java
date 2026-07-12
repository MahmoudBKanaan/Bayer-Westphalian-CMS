package com.bayerwestphalian.campaign.product;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProductOwnershipRequest(
        LocalDate expirationDate, @Size(max = 100) String policyNumber) {

    UpdateProductOwnershipCommand toCommand() {
        return new UpdateProductOwnershipCommand(expirationDate, policyNumber);
    }
}