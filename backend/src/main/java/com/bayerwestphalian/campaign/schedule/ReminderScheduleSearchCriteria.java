package com.bayerwestphalian.campaign.schedule;

import java.time.LocalDate;
import java.util.UUID;

public record ReminderScheduleSearchCriteria(
        UUID customerId, ReminderStatus status, LocalDate dueOnOrBefore) {}
