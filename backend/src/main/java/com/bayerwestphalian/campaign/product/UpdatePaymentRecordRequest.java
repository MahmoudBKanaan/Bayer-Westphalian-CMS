package com.bayerwestphalian.campaign.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePaymentRecordRequest(
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amountDue) {

    public UpdatePaymentRecordCommand toCommand() {
        return new UpdatePaymentRecordCommand(dueDate, amountDue);
    }
}
