package com.bayerwestphalian.campaign.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record MarkPaymentPaidRequest(
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amountPaid,
        Instant paidAt) {

    MarkPaymentPaidCommand toCommand() {
        return new MarkPaymentPaidCommand(amountPaid, paidAt);
    }
}
