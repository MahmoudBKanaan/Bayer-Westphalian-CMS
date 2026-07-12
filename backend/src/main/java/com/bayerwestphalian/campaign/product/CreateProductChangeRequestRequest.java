package com.bayerwestphalian.campaign.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateProductChangeRequestRequest(
        @NotNull UUID productId,
        @NotNull ProductChangeType requestType,
        @NotBlank String description) {

    CreateProductChangeRequestCommand toCommand() {
        return new CreateProductChangeRequestCommand(productId, requestType, description);
    }
}
