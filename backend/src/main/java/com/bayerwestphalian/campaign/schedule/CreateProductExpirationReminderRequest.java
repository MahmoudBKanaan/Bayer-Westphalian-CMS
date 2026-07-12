package com.bayerwestphalian.campaign.schedule;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateProductExpirationReminderRequest(
        @NotNull UUID customerId,
        @NotNull UUID productId,
        @NotNull ReminderLevel reminderLevel,
        @NotNull LocalDate scheduledDate) {

    public ReminderScheduleCommand toCommand() {
        return new ReminderScheduleCommand(
                customerId,
                productId,
                ReminderType.PRODUCT_EXPIRATION,
                reminderLevel,
                scheduledDate);
    }
}
