package com.bayerwestphalian.campaign.schedule;

import java.time.LocalDate;
import java.util.UUID;

public record ReminderScheduleCommand(
        UUID customerId,
        UUID productId,
        ReminderType reminderType,
        ReminderLevel reminderLevel,
        LocalDate scheduledDate) {}
