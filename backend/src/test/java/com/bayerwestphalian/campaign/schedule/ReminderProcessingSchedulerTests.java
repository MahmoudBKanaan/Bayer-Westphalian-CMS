package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bayerwestphalian.campaign.CampaignApplication;
import com.bayerwestphalian.campaign.auth.AuthenticatedPrincipal;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.product.ProductType;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit coverage for {@link ReminderProcessingScheduler}: cron wiring, security context, environment
 * guards, and structured attempt logging (KB FR-089 / items 402 and 406). See {@code
 * docs/modules/reminder-scheduler.md}.
 */
class ReminderProcessingSchedulerTests {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void applicationEnablesScheduledReminderProcessing() {
        assertThat(CampaignApplication.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
    }

    @Test
    void processDueRemindersDeclaresConfigurableSchedulerCron() throws Exception {
        Method method = ReminderProcessingScheduler.class.getMethod("processDueReminders");

        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo(ReminderProcessingScheduler.PROCESSING_CRON);
        assertThat(scheduled.cron()).contains("app.reminders.processing-cron");
    }

    @Test
    void schedulerDeclaresLoggerForKbReminderAttemptLogging() throws Exception {
        Field logger = ReminderProcessingScheduler.class.getDeclaredField("LOGGER");

        assertThat(logger.getType()).isEqualTo(org.slf4j.Logger.class);
    }

    @Test
    void processDueRemindersDelegatesAsSystemCampaignManagerAndRestoresAuthentication() {
        ReminderService reminderService = Mockito.mock(ReminderService.class);
        Authentication previousAuthentication =
                new TestingAuthenticationToken("existing-user", "credentials", "ROLE_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        when(reminderService.sendDueReminders())
                .thenAnswer(
                        invocation -> {
                            Authentication authentication =
                                    SecurityContextHolder.getContext().getAuthentication();
                            assertThat(authentication).isNotSameAs(previousAuthentication);
                            assertThat(authentication.isAuthenticated()).isTrue();
                            assertThat(authentication.getPrincipal())
                                    .isInstanceOf(AuthenticatedPrincipal.class);
                            assertThat(authentication.getAuthorities())
                                    .extracting(GrantedAuthority::getAuthority)
                                    .containsExactly("ROLE_" + SystemRoleName.CAMPAIGN_MANAGER);
                            return List.of();
                        });
        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(reminderService, new MockEnvironment());

        scheduler.processDueReminders();

        verify(reminderService).sendDueReminders();
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(previousAuthentication);
    }

    @Test
    void processDueRemindersClearsSecurityContextWhenNoPreviousAuthenticationExists() {
        ReminderService reminderService = Mockito.mock(ReminderService.class);
        when(reminderService.sendDueReminders()).thenReturn(List.of());
        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(reminderService, new MockEnvironment());

        scheduler.processDueReminders();

        verify(reminderService).sendDueReminders();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void processDueRemindersLogsScheduledStartAndCompletion() {
        // KB item 402 / FR-089: scheduler logs start, each attempt, and completion counts.
        ReminderService reminderService = Mockito.mock(ReminderService.class);
        when(reminderService.sendDueReminders())
                .thenReturn(
                        List.of(
                                reminderView(
                                        UUID.fromString("50000000-0000-0000-0000-000000000001"),
                                        UUID.fromString("50000000-0000-0000-0000-000000000101"),
                                        UUID.fromString("50000000-0000-0000-0000-000000000201"),
                                        ReminderType.PAYMENT_DUE,
                                        ReminderLevel.GREEN,
                                        ReminderStatus.SENT)));
        MockEnvironment environment =
                new MockEnvironment().withProperty("spring.profiles.active", "test");
        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(reminderService, environment);
        ListAppender<ILoggingEvent> appender = attachLogAppender();

        try {
            scheduler.processDueReminders();
        } finally {
            detachLogAppender(appender);
        }

        assertThat(appender.list).hasSize(3);
        assertThat(appender.list)
                .extracting(ILoggingEvent::getLevel)
                .containsExactly(Level.INFO, Level.INFO, Level.INFO);
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(
                        logLine ->
                                assertThat(logLine)
                                        .contains(
                                                ReminderProcessingScheduler.LOG_PROCESSING_STARTED,
                                                "trigger=scheduled",
                                                "activeProfiles=test"))
                .anySatisfy(
                        logLine ->
                                assertThat(logLine)
                                        .contains(
                                                ReminderProcessingScheduler.LOG_REMINDER_ATTEMPT,
                                                "trigger=scheduled",
                                                "reminderId=50000000-0000-0000-0000-000000000001",
                                                "customerId=50000000-0000-0000-0000-000000000101",
                                                "productId=50000000-0000-0000-0000-000000000201",
                                                "reminderType=PAYMENT_DUE",
                                                "reminderLevel=GREEN",
                                                "status=SENT",
                                                "scheduledDate=2026-07-11",
                                                "sentAt=2026-07-11T09:05:00Z"))
                .anySatisfy(
                        logLine ->
                                assertThat(logLine)
                                        .contains(
                                                ReminderProcessingScheduler
                                                        .LOG_PROCESSING_COMPLETED,
                                                "trigger=scheduled",
                                                "processedCount=1",
                                                "sentCount=1",
                                                "cancelledCount=0",
                                                "failedCount=0"));
    }

    @Test
    void processDueRemindersLogsFailuresAndRestoresAuthentication() {
        ReminderService reminderService = Mockito.mock(ReminderService.class);
        RuntimeException failure = new IllegalStateException("repository unavailable");
        when(reminderService.sendDueReminders()).thenThrow(failure);
        Authentication previousAuthentication =
                new TestingAuthenticationToken("existing-user", "credentials", "ROLE_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(reminderService, new MockEnvironment());
        ListAppender<ILoggingEvent> appender = attachLogAppender();

        try {
            assertThatThrownBy(scheduler::processDueReminders).isSameAs(failure);
        } finally {
            detachLogAppender(appender);
        }

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(previousAuthentication);
        assertThat(appender.list).hasSize(2);
        assertThat(appender.list)
                .extracting(ILoggingEvent::getLevel)
                .containsExactly(Level.INFO, Level.ERROR);
        assertThat(appender.list.get(1).getFormattedMessage())
                .contains(
                        ReminderProcessingScheduler.LOG_PROCESSING_FAILED,
                        "trigger=scheduled",
                        "errorType=IllegalStateException",
                        "errorMessage=repository unavailable");
        assertThat(appender.list.get(1).getThrowableProxy()).isNotNull();
    }

    @Test
    void manualTriggerProcessesDueRemindersInTestEnvironment() {
        ReminderService reminderService = Mockito.mock(ReminderService.class);
        when(reminderService.sendDueReminders()).thenReturn(List.of());
        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(
                        reminderService,
                        new MockEnvironment().withProperty("spring.profiles.active", "test"));

        List<ReminderScheduleView> reminders = scheduler.triggerManualProcessing();

        assertThat(reminders).isEmpty();
        verify(reminderService).sendDueReminders();
    }

    @Test
    void manualTriggerLogsStartAndCompletionInTestEnvironment() {
        // KB item 402: manual trigger uses the same attempt logging as the cron job.
        ReminderService reminderService = Mockito.mock(ReminderService.class);
        when(reminderService.sendDueReminders())
                .thenReturn(
                        List.of(
                                reminderView(
                                        UUID.fromString("50000000-0000-0000-0000-000000000002"),
                                        UUID.fromString("50000000-0000-0000-0000-000000000102"),
                                        UUID.fromString("50000000-0000-0000-0000-000000000202"),
                                        ReminderType.PAYMENT_DUE,
                                        ReminderLevel.YELLOW,
                                        ReminderStatus.SENT),
                                reminderView(
                                        UUID.fromString("50000000-0000-0000-0000-000000000003"),
                                        UUID.fromString("50000000-0000-0000-0000-000000000103"),
                                        UUID.fromString("50000000-0000-0000-0000-000000000203"),
                                        ReminderType.PRODUCT_EXPIRATION,
                                        ReminderLevel.RED,
                                        ReminderStatus.CANCELLED)));
        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(
                        reminderService,
                        new MockEnvironment().withProperty("spring.profiles.active", "test"));
        ListAppender<ILoggingEvent> appender = attachLogAppender();

        try {
            scheduler.triggerManualProcessing();
        } finally {
            detachLogAppender(appender);
        }

        assertThat(appender.list).hasSize(4);
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(
                        logLine ->
                                assertThat(logLine)
                                        .contains(
                                                ReminderProcessingScheduler.LOG_PROCESSING_STARTED,
                                                "trigger=manual"))
                .anySatisfy(
                        logLine ->
                                assertThat(logLine)
                                        .contains(
                                                ReminderProcessingScheduler.LOG_REMINDER_ATTEMPT,
                                                "trigger=manual",
                                                "reminderId=50000000-0000-0000-0000-000000000002",
                                                "reminderLevel=YELLOW",
                                                "status=SENT"))
                .anySatisfy(
                        logLine ->
                                assertThat(logLine)
                                        .contains(
                                                ReminderProcessingScheduler.LOG_REMINDER_ATTEMPT,
                                                "trigger=manual",
                                                "reminderId=50000000-0000-0000-0000-000000000003",
                                                "reminderType=PRODUCT_EXPIRATION",
                                                "reminderLevel=RED",
                                                "status=CANCELLED"))
                .anySatisfy(
                        logLine ->
                                assertThat(logLine)
                                        .contains(
                                                ReminderProcessingScheduler
                                                        .LOG_PROCESSING_COMPLETED,
                                                "trigger=manual",
                                                "processedCount=2",
                                                "sentCount=1",
                                                "cancelledCount=1",
                                                "failedCount=0"));
    }

    @Test
    void manualTriggerProcessesDueRemindersWhenDevelopmentIsDefaultEnvironment() {
        ReminderService reminderService = Mockito.mock(ReminderService.class);
        when(reminderService.sendDueReminders()).thenReturn(List.of());
        MockEnvironment environment = new MockEnvironment();
        environment.setDefaultProfiles("dev");
        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(reminderService, environment);

        List<ReminderScheduleView> reminders = scheduler.triggerManualProcessing();

        assertThat(reminders).isEmpty();
        verify(reminderService).sendDueReminders();
    }

    @Test
    void manualTriggerIsBlockedOutsideDevelopmentAndTestEnvironments() {
        ReminderService reminderService = Mockito.mock(ReminderService.class);
        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(
                        reminderService,
                        new MockEnvironment().withProperty("spring.profiles.active", "prod"));

        assertThatThrownBy(scheduler::triggerManualProcessing)
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("development or test");
        verifyNoInteractions(reminderService);
    }

    @Test
    void manualTriggerBlockedOutsideDevelopmentAndTestEnvironmentsIsLogged() {
        ReminderService reminderService = Mockito.mock(ReminderService.class);
        ReminderProcessingScheduler scheduler =
                new ReminderProcessingScheduler(
                        reminderService,
                        new MockEnvironment().withProperty("spring.profiles.active", "prod"));
        ListAppender<ILoggingEvent> appender = attachLogAppender();

        try {
            assertThatThrownBy(scheduler::triggerManualProcessing)
                    .isInstanceOf(ForbiddenException.class);
        } finally {
            detachLogAppender(appender);
        }

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(appender.list.get(0).getFormattedMessage())
                .contains(
                        "Reminder scheduler manual trigger blocked",
                        "activeProfiles=prod",
                        "defaultProfiles=default");
        verifyNoInteractions(reminderService);
    }

    private static ReminderScheduleView reminderView(
            UUID reminderId,
            UUID customerId,
            UUID productId,
            ReminderType reminderType,
            ReminderLevel reminderLevel,
            ReminderStatus status) {
        return new ReminderScheduleView(
                reminderId,
                customerId,
                "Ada Lovelace",
                productId,
                "Life Protect",
                ProductType.LIFE_INSURANCE,
                reminderType,
                reminderLevel,
                LocalDate.of(2026, 7, 11),
                status,
                Instant.parse("2026-07-11T09:00:00Z"),
                status == ReminderStatus.SENT ? Instant.parse("2026-07-11T09:05:00Z") : null,
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
