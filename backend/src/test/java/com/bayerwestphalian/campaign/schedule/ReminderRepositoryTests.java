package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

/** KB item 371: ReminderRepository exposes due, status, and customer reminder lookups. */
class ReminderRepositoryTests {

    @Test
    void extendsJpaRepositoryForReminderScheduleAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(ReminderRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(ReminderRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(ReminderSchedule.class, UUID.class);
    }

    @Test
    void declaresKbReminderLookupMethods() throws Exception {
        assertThat(
                        ReminderRepository.class
                                .getMethod("findDueReminders", LocalDate.class)
                                .getGenericReturnType())
                .isEqualTo(reminderScheduleList());
        assertThat(
                        ReminderRepository.class
                                .getMethod("findByStatus", ReminderStatus.class)
                                .getGenericReturnType())
                .isEqualTo(reminderScheduleList());
        assertThat(
                        ReminderRepository.class
                                .getMethod("findByCustomerId", UUID.class)
                                .getGenericReturnType())
                .isEqualTo(reminderScheduleList());
    }

    @Test
    void concreteFindersUseStableScheduledDateOrderingForSchedulerWork() throws Exception {
        assertThat(
                        ReminderRepository.class
                                .getMethod(
                                        "findByStatusOrderByScheduledDateAsc",
                                        ReminderStatus.class)
                                .getGenericReturnType())
                .isEqualTo(reminderScheduleList());
        assertThat(
                        ReminderRepository.class
                                .getMethod(
                                        "findByCustomer_IdOrderByScheduledDateAsc", UUID.class)
                                .getGenericReturnType())
                .isEqualTo(reminderScheduleList());
        assertThat(
                        ReminderRepository.class
                                .getMethod(
                                        "findByStatusAndScheduledDateLessThanEqualOrderByScheduledDateAsc",
                                        ReminderStatus.class,
                                        LocalDate.class)
                                .getGenericReturnType())
                .isEqualTo(reminderScheduleList());
    }

    private static Type reminderScheduleList() throws NoSuchMethodException {
        return ReturnTypes.class.getDeclaredMethod("reminderScheduleList").getGenericReturnType();
    }

    private interface ReturnTypes {
        List<ReminderSchedule> reminderScheduleList();
    }
}
