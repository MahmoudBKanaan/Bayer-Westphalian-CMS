package com.bayerwestphalian.campaign.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull ProductType productType,
        String description,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Positive Integer durationMonths,
        @Size(max = 100) String expirationPolicy,
        Boolean active) {

    UpdateProductCommand toCommand() {
        return new UpdateProductCommand(
                name, productType, description, price, durationMonths, expirationPolicy, active);
    }
}
