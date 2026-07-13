package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.annotation.Scheduled;

class ProductionSchedulerLoggingTests {

    private static final Pattern RUN_ID =
            Pattern.compile("runId=([0-9a-f]{8}-[0-9a-f-]{27})");

    @Test
    void oneRunUsesStableCorrelationAndDurationThenClearsMdc() {
        ReminderService reminderService = Mockito.mock(ReminderService.class);
        when(reminderService.sendDueReminders()).thenReturn(List.of());
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            new ReminderProcessingScheduler(reminderService, new MockEnvironment())
                    .processDueReminders();
        } finally {
            detachAppender(appender);
        }

        assertThat(appender.list).hasSize(3);
        List<String> messages =
                appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        String runId = extractRunId(messages.getFirst());
        assertThat(messages)
                .allSatisfy(message -> assertThat(message).contains("runId=" + runId));
        assertThat(messages.getFirst()).contains("schedulerEvent=run_started");
        assertThat(messages.get(1)).contains("schedulerEvent=reminder_attempt");
        assertThat(messages.get(2))
                .contains("schedulerEvent=run_completed")
                .containsPattern("durationMs=\\d+");
        assertThat(appender.list)
                .allSatisfy(
                        event ->
                                assertThat(
                                                event.getMDCPropertyMap()
                                                        .get(
                                                                ReminderProcessingScheduler
                                                                        .SCHEDULER_RUN_ID_MDC_KEY))
                                        .isEqualTo(runId));
        assertThat(MDC.get(ReminderProcessingScheduler.SCHEDULER_RUN_ID_MDC_KEY)).isNull();
    }

    @Test
    void scheduledAnnotationUsesConfigurableCronAndZone() throws Exception {
        Scheduled scheduled =
                ReminderProcessingScheduler.class
                        .getMethod("processDueReminders")
                        .getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo(ReminderProcessingScheduler.PROCESSING_CRON);
        assertThat(scheduled.zone()).isEqualTo(ReminderProcessingScheduler.PROCESSING_ZONE);
    }

    private static String extractRunId(String message) {
        Matcher matcher = RUN_ID.matcher(message);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(ReminderProcessingScheduler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(ReminderProcessingScheduler.class);
        logger.detachAppender(appender);
    }
}
