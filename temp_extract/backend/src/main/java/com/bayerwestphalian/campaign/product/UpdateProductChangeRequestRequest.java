package com.bayerwestphalian.campaign.product;

import jakarta.validation.constraints.NotBlank;

public record UpdateProductChangeRequestRequest(@NotBlank String description) {

    UpdateProductChangeRequestCommand toCommand() {
        return new UpdateProductChangeRequestCommand(description);
    }
}