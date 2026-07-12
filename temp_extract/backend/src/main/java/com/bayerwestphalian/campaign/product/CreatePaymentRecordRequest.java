package com.bayerwestphalian.campaign.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePaymentRecordRequest(
        @NotNull UUID customerId,
        @NotNull UUID productOwnershipId,
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amountDue) {

    CreatePaymentRecordCommand toCommand() {
        return new CreatePaymentRecordCommand(
                customerId, productOwnershipId, dueDate, amountDue);
    }
}