package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bayerwestphalian.campaign.product.ProductType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * KB item 402 / FR-089: scheduler logs reminder attempts.
 *
 * <p>Both the cron job ({@link ReminderProcessingScheduler#processDueReminders()}) and the admin
 * manual trigger must emit structured logs for every processed reminder (SENT, CANCELLED, FAILED)
 * plus start and completion summaries with outcome counts.
 */
class SchedulerLogsReminderAttemptsTests {

    private static final UUID REMINDER_SENT_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000402");
    private static final UUID REMINDER_CANCELLED_ID =
            UUID.fromString("50000000-0000-0000-0000-000000000412");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000402");
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-0000-0000-000000000402");
    private static final LocalDate SCHEDULED_DATE = LocalDate.of(2026, 7, 11);
    private static final Instant SENT_AT = Instant.parse("2026-07-11T09:05:00Z");

    private ReminderService reminderService;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        reminderService = Mockito.mock(ReminderService.class);
        appender = attachLogAppender();
    }

    @AfterEach
    void tearDown() {
        detachLogAppender(appender);
        SecurityContextHolder.clearContext();
    }

    @Test
    void scheduledJobLogsEachReminderAttemptWithIdentifiersAndStatus() {
        when(reminderService.sendDueReminders())
                .thenReturn(
                        List.of(
                                reminderView(
                                        REMINDER_SENT_ID,
                                        ReminderType.PAYMENT_DUE,
                                        ReminderLevel.GREEN,
                                        ReminderStatus.SENT,
                                        SENT_AT),
                                reminderView(
                                        REMINDER_CANCELLED_ID,
                                        ReminderType.PRODUCT_EXPIRATION,
                                        ReminderLevel.YELLOW,
                                        ReminderStatus.CANCELLED,
                                        null)));

        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(
                        reminderService,
                        new MockEnvironment().withProperty("spring.profiles.active", "test"));

        scheduler.processDueReminders();

        List<String> messages =
                appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();

        assertThat(messages)
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .contains(
                                                ReminderProcessingScheduler.LOG_PROCESSING_STARTED,
                                                "trigger=scheduled"));
        assertThat(messages)
                .filteredOn(line -> line.contains(ReminderProcessingScheduler.LOG_REMINDER_ATTEMPT))
                .hasSize(2)
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .contains(
                                                "trigger=scheduled",
                                                "reminderId=" + REMINDER_SENT_ID,
                                                "customerId=" + CUSTOMER_ID,
                                                "productId=" + PRODUCT_ID,
                                                "reminderType=PAYMENT_DUE",
                                                "reminderLevel=GREEN",
                                                "status=SENT",
                                                "scheduledDate=" + SCHEDULED_DATE,
                                                "sentAt=" + SENT_AT))
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .contains(
                                                "trigger=scheduled",
                                                "reminderId=" + REMINDER_CANCELLED_ID,
                                                "reminderType=PRODUCT_EXPIRATION",
                                                "reminderLevel=YELLOW",
                                                "status=CANCELLED",
                                                "scheduledDate=" + SCHEDULED_DATE,
                                                "sentAt=null"));
        assertThat(messages)
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .contains(
                                                ReminderProcessingScheduler
                                                        .LOG_PROCESSING_COMPLETED,
                                                "trigger=scheduled",
                                                "processedCount=2",
                                                "sentCount=1",
                                                "cancelledCount=1",
                                                "failedCount=0"));
        verify(reminderService).sendDueReminders();
    }

    @Test
    void manualTriggerLogsEachReminderAttempt() {
        when(reminderService.sendDueReminders())
                .thenReturn(
                        List.of(
                                reminderView(
                                        REMINDER_SENT_ID,
                                        ReminderType.PAYMENT_DUE,
                                        ReminderLevel.RED,
                                        ReminderStatus.SENT,
                                        SENT_AT)));

        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(
                        reminderService,
                        new MockEnvironment().withProperty("spring.profiles.active", "dev"));

        List<ReminderScheduleView> result = scheduler.triggerManualProcessing();

        assertThat(result).hasSize(1);
        List<String> messages =
                appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages)
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .contains(
                                                ReminderProcessingScheduler.LOG_PROCESSING_STARTED,
                                                "trigger=manual"))
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .contains(
                                                ReminderProcessingScheduler.LOG_REMINDER_ATTEMPT,
                                                "trigger=manual",
                                                "reminderId=" + REMINDER_SENT_ID,
                                                "status=SENT",
                                                "reminderLevel=RED"))
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .contains(
                                                ReminderProcessingScheduler
                                                        .LOG_PROCESSING_COMPLETED,
                                                "trigger=manual",
                                                "processedCount=1",
                                                "sentCount=1"));
    }

    @Test
    void schedulerLogsZeroAttemptSummaryWhenNoDueReminders() {
        when(reminderService.sendDueReminders()).thenReturn(List.of());

        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(
                        reminderService,
                        new MockEnvironment().withProperty("spring.profiles.active", "test"));

        scheduler.processDueReminders();

        List<String> messages =
                appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages)
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .contains(
                                                ReminderProcessingScheduler.LOG_REMINDER_ATTEMPT,
                                                "trigger=scheduled",
                                                "attemptCount=0",
                                                "no due reminders processed"));
        assertThat(messages)
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .contains(
                                                ReminderProcessingScheduler
                                                        .LOG_PROCESSING_COMPLETED,
                                                "processedCount=0",
                                                "sentCount=0",
                                                "cancelledCount=0",
                                                "failedCount=0"));
        assertThat(appender.list)
                .extracting(ILoggingEvent::getLevel)
                .containsOnly(Level.INFO);
    }

    @Test
    void schedulerLogsFailedReminderAttemptStatus() {
        when(reminderService.sendDueReminders())
                .thenReturn(
                        List.of(
                                reminderView(
                                        REMINDER_SENT_ID,
                                        ReminderType.PAYMENT_DUE,
                                        ReminderLevel.YELLOW,
                                        ReminderStatus.FAILED,
                                        null)));

        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(reminderService, new MockEnvironment());

        scheduler.processDueReminders();

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .contains(
                                                ReminderProcessingScheduler.LOG_REMINDER_ATTEMPT,
                                                "status=FAILED",
                                                "reminderLevel=YELLOW"))
                .anySatisfy(
                        line ->
                                assertThat(line)
                                        .contains(
                                                "processedCount=1",
                                                "sentCount=0",
                                                "cancelledCount=0",
                                                "failedCount=1"));
    }

    @Test
    void schedulerLogsProcessingFailureWithoutSilentSwallow() {
        RuntimeException failure = new IllegalStateException("send pipeline down");
        when(reminderService.sendDueReminders()).thenThrow(failure);

        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(reminderService, new MockEnvironment());

        assertThatThrownBy(scheduler::processDueReminders).isSameAs(failure);

        assertThat(appender.list)
                .anySatisfy(
                        event -> {
                            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                            assertThat(event.getFormattedMessage())
                                    .contains(
                                            ReminderProcessingScheduler.LOG_PROCESSING_FAILED,
                                            "trigger=scheduled",
                                            "errorType=IllegalStateException",
                                            "errorMessage=send pipeline down");
                            assertThat(event.getThrowableProxy()).isNotNull();
                        });
        // Start was logged; no successful attempt/completion lines after failure.
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(
                        line ->
                                line.contains(
                                        ReminderProcessingScheduler.LOG_PROCESSING_COMPLETED));
    }

    @Test
    void reminderAttemptLogsUseInfoLevelForAuditTrail() {
        when(reminderService.sendDueReminders())
                .thenReturn(
                        List.of(
                                reminderView(
                                        REMINDER_SENT_ID,
                                        ReminderType.PAYMENT_DUE,
                                        ReminderLevel.GREEN,
                                        ReminderStatus.SENT,
                                        SENT_AT)));

        new ReminderProcessingScheduler(reminderService, new MockEnvironment())
                .processDueReminders();

        assertThat(appender.list)
                .filteredOn(
                        event ->
                                event.getFormattedMessage()
                                        .contains(ReminderProcessingScheduler.LOG_REMINDER_ATTEMPT))
                .isNotEmpty()
                .allMatch(event -> event.getLevel() == Level.INFO);
    }

    private static ReminderScheduleView reminderView(
            UUID reminderId,
            ReminderType reminderType,
            ReminderLevel reminderLevel,
            ReminderStatus status,
            Instant sentAt) {
        return new ReminderScheduleView(
                reminderId,
                CUSTOMER_ID,
                "Ada Scheduler",
                PRODUCT_ID,
                "Life Protection",
                ProductType.LIFE_INSURANCE,
                reminderType,
                reminderLevel,
                SCHEDULED_DATE,
                status,
                Instant.parse("2026-07-11T09:00:00Z"),
                sentAt,
                false);
    }

    private static ListAppender<ILoggingEvent> attachLogAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(ReminderProcessingScheduler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachLogAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(ReminderProcessingScheduler.class);
        logger.detachAppender(appender);
    }
}
