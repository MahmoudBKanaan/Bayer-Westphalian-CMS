package com.bayerwestphalian.campaign.schedule;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public record ReminderScheduleSearchRequest(
        UUID customerId,
        ReminderStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueOnOrBefore) {

    public ReminderScheduleSearchCriteria toCriteria() {
        return new ReminderScheduleSearchCriteria(customerId, status, dueOnOrBefore);
    }
}
