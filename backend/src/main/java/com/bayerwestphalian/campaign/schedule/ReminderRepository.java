package com.bayerwestphalian.campaign.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<ReminderSchedule, UUID> {

    List<ReminderSchedule> findByStatusOrderByScheduledDateAsc(ReminderStatus status);

    List<ReminderSchedule> findByCustomer_IdOrderByScheduledDateAsc(UUID customerId);

    List<ReminderSchedule> findByStatusAndScheduledDateLessThanEqualOrderByScheduledDateAsc(
            ReminderStatus status, LocalDate scheduledDate);

    default List<ReminderSchedule> findDueReminders(LocalDate asOfDate) {
        return findByStatusAndScheduledDateLessThanEqualOrderByScheduledDateAsc(
                ReminderStatus.PENDING, asOfDate);
    }

    default List<ReminderSchedule> findByStatus(ReminderStatus status) {
        return findByStatusOrderByScheduledDateAsc(status);
    }

    default List<ReminderSchedule> findByCustomerId(UUID customerId) {
        return findByCustomer_IdOrderByScheduledDateAsc(customerId);
    }
}
