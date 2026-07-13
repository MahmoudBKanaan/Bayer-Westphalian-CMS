package com.bayerwestphalian.campaign.schedule;

import com.bayerwestphalian.campaign.auth.AuthenticatedPrincipal;
import com.bayerwestphalian.campaign.common.exception.ForbiddenException;
import com.bayerwestphalian.campaign.user.SystemRoleName;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Processes due reminder schedules on a configurable cron and via admin manual trigger (KB E18 /
 * FR-089 / items 402 and 406).
 *
 * <p>Every processing run logs:
 *
 * <ul>
 *   <li>run start (trigger source and active profiles)
 *   <li>one structured line per reminder attempt (id, customer, product, type, level, status,
 *       scheduled date)
 *   <li>run completion with attempt outcome counts (sent / cancelled / failed)
 *   <li>failures with exception details
 * </ul>
 *
 * <p>Manual trigger is limited to {@code dev} and {@code test} environments.
 *
 * <p>See {@code docs/modules/reminder-scheduler.md} for operator and developer documentation.
 */
@Component
public class ReminderProcessingScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReminderProcessingScheduler.class);

    /** Configurable cron property placeholder used by {@link #processDueReminders()}. */
    static final String PROCESSING_CRON = "${app.reminders.processing-cron:0 */15 * * * *}";

    /** Explicit scheduler timezone; production defaults to UTC. */
    static final String PROCESSING_ZONE = "${app.reminders.processing-zone:UTC}";

    static final String SCHEDULER_RUN_ID_MDC_KEY = "schedulerRunId";

    /** Log marker for a single reminder send/cancel attempt (KB FR-089 / item 402). */
    static final String LOG_REMINDER_ATTEMPT = "Reminder scheduler reminder attempt";

    /** Log marker for processing start. */
    static final String LOG_PROCESSING_STARTED = "Reminder scheduler processing started";

    /** Log marker for processing completion with outcome counts. */
    static final String LOG_PROCESSING_COMPLETED = "Reminder scheduler processing completed";

    /** Log marker for processing failure. */
    static final String LOG_PROCESSING_FAILED = "Reminder scheduler processing failed";

    private static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final ReminderService reminderService;
    private final Environment environment;

    public ReminderProcessingScheduler(ReminderService reminderService, Environment environment) {
        this.reminderService = reminderService;
        this.environment = environment;
    }

    /**
     * Cron-driven due-reminder processing. Runs under a system Campaign Manager principal so
     * {@link ReminderService#sendDueReminders()} method security succeeds. Logs every reminder
     * attempt (KB FR-089 / item 402).
     */
    @Scheduled(cron = PROCESSING_CRON, zone = PROCESSING_ZONE)
    public void processDueReminders() {
        Authentication previousAuthentication =
                SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(systemCampaignManagerAuthentication());
        try {
            processAndLogAttempts("scheduled");
        } finally {
            restoreAuthentication(previousAuthentication);
        }
    }

    /**
     * Manual due-reminder processing for Admin in dev/test (KB E18 / item 402). Logs every
     * reminder attempt using the same structured format as the scheduled job.
     */
    public List<ReminderScheduleView> triggerManualProcessing() {
        ensureManualTriggerAllowed();
        return processAndLogAttempts("manual");
    }

    /**
     * Sends due reminders and writes start, per-attempt, and completion logs (KB FR-089).
     *
     * @param trigger {@code scheduled} or {@code manual}
     * @return processed reminder views (including cancelled outcomes)
     */
    private List<ReminderScheduleView> processAndLogAttempts(String trigger) {
        String runId = UUID.randomUUID().toString();
        long startedAtNanos = System.nanoTime();
        try (MDC.MDCCloseable ignored = MDC.putCloseable(SCHEDULER_RUN_ID_MDC_KEY, runId)) {
            try {
                logProcessingStarted(trigger, runId);
                List<ReminderScheduleView> reminders = reminderService.sendDueReminders();
                logReminderAttempts(trigger, runId, reminders);
                logProcessingCompleted(trigger, runId, reminders, elapsedMillis(startedAtNanos));
                return reminders;
            } catch (RuntimeException exception) {
                logProcessingFailed(trigger, runId, elapsedMillis(startedAtNanos), exception);
                throw exception;
            }
        }
    }

    private void ensureManualTriggerAllowed() {
        if (hasProfile("dev") || hasProfile("test")) {
            return;
        }
        LOGGER.warn(
                "Reminder scheduler manual trigger blocked schedulerEvent=manual_blocked "
                        + "activeProfiles={} defaultProfiles={}",
                profiles(environment.getActiveProfiles()),
                profiles(environment.getDefaultProfiles()));
        throw new ForbiddenException(
                "Manual reminder processing is available only in development or test environments");
    }

    private boolean hasProfile(String profileName) {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(profileName::equalsIgnoreCase)
                || Arrays.stream(environment.getDefaultProfiles())
                        .anyMatch(profileName::equalsIgnoreCase);
    }

    private void logProcessingStarted(String trigger, String runId) {
        LOGGER.info(
                "{} schedulerEvent=run_started trigger={} runId={} activeProfiles={} "
                        + "defaultProfiles={}",
                LOG_PROCESSING_STARTED,
                trigger,
                runId,
                profiles(environment.getActiveProfiles()),
                profiles(environment.getDefaultProfiles()));
    }

    /**
     * Emits one INFO log line per processed reminder so operators can audit each attempt (KB
     * FR-089 / item 402). Status may be SENT, CANCELLED (e.g. paid payment or ineligible
     * recipient), or FAILED depending on send rules.
     */
    private static void logReminderAttempts(
            String trigger, String runId, List<ReminderScheduleView> reminders) {
        if (reminders == null || reminders.isEmpty()) {
            LOGGER.info(
                    "{} schedulerEvent=reminder_attempt trigger={} runId={} attemptCount=0 "
                            + "(no due reminders processed)",
                    LOG_REMINDER_ATTEMPT,
                    trigger,
                    runId);
            return;
        }
        reminders.forEach(
                reminder ->
                        LOGGER.info(
                                "{} schedulerEvent=reminder_attempt trigger={} runId={} "
                                        + "reminderId={} customerId={} productId={} "
                                        + "reminderType={} reminderLevel={} status={} "
                                        + "scheduledDate={} sentAt={}",
                                LOG_REMINDER_ATTEMPT,
                                trigger,
                                runId,
                                reminder.id(),
                                reminder.customerId(),
                                reminder.productId(),
                                reminder.reminderType(),
                                reminder.reminderLevel(),
                                reminder.status(),
                                reminder.scheduledDate(),
                                reminder.sentAt()));
    }

    private static void logProcessingCompleted(
            String trigger, String runId, List<ReminderScheduleView> reminders, long durationMs) {
        int processedCount = reminders == null ? 0 : reminders.size();
        long sentCount = countByStatus(reminders, ReminderStatus.SENT);
        long cancelledCount = countByStatus(reminders, ReminderStatus.CANCELLED);
        long failedCount = countByStatus(reminders, ReminderStatus.FAILED);
        LOGGER.info(
                "{} schedulerEvent=run_completed trigger={} runId={} durationMs={} "
                        + "processedCount={} sentCount={} cancelledCount={} failedCount={}",
                LOG_PROCESSING_COMPLETED,
                trigger,
                runId,
                durationMs,
                processedCount,
                sentCount,
                cancelledCount,
                failedCount);
    }

    private static long countByStatus(
            List<ReminderScheduleView> reminders, ReminderStatus status) {
        if (reminders == null || reminders.isEmpty()) {
            return 0L;
        }
        return reminders.stream().filter(reminder -> reminder.status() == status).count();
    }

    private static void logProcessingFailed(
            String trigger, String runId, long durationMs, RuntimeException exception) {
        LOGGER.error(
                "{} schedulerEvent=run_failed trigger={} runId={} durationMs={} "
                        + "errorType={} errorMessage={}",
                LOG_PROCESSING_FAILED,
                trigger,
                runId,
                durationMs,
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                exception);
    }

    private static long elapsedMillis(long startedAtNanos) {
        return Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

    private static String profiles(String[] profiles) {
        return profiles == null || profiles.length == 0 ? "none" : String.join(",", profiles);
    }

    private Authentication systemCampaignManagerAuthentication() {
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(
                        SYSTEM_USER_ID,
                        "system-reminder-scheduler@bayerwestphalian.local",
                        List.of(SystemRoleName.CAMPAIGN_MANAGER));
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + SystemRoleName.CAMPAIGN_MANAGER)));
    }

    private void restoreAuthentication(Authentication previousAuthentication) {
        if (previousAuthentication == null) {
            SecurityContextHolder.clearContext();
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(previousAuthentication);
    }
}
