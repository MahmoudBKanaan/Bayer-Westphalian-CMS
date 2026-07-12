package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * KB item 406: Scheduler documentation covers cron configuration, security context, manual trigger
 * guards, FR-089 attempt logging, and operational guidance for {@link ReminderProcessingScheduler}.
 */
class ReminderSchedulerDocumentationTests {

    private static final Path SCHEDULER_DOC = Path.of("../docs/modules/reminder-scheduler.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path REMINDER_SCHEDULING_DOC =
            Path.of("../docs/modules/reminder-scheduling.md");
    private static final Path SCHEDULER_SOURCE =
            Path.of(
                    "src/main/java/com/bayerwestphalian/campaign/schedule/ReminderProcessingScheduler.java");
    private static final Path PACKAGE_INFO =
            Path.of("src/main/java/com/bayerwestphalian/campaign/schedule/package-info.java");
    private static final Path APPLICATION_SOURCE =
            Path.of("src/main/java/com/bayerwestphalian/campaign/CampaignApplication.java");

    @Test
    void documentsSchedulerBoundaryAndCronConfiguration() throws Exception {
        String documentation = Files.readString(SCHEDULER_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("# Reminder Scheduler Documentation")
                .contains("## Package Boundary")
                .contains("ReminderProcessingScheduler")
                .contains("CampaignApplication")
                .contains("@EnableScheduling")
                .contains("## Cron Configuration")
                .contains("app.reminders.processing-cron")
                .contains("PROCESSING_CRON")
                .contains("0 */15 * * * *")
                .contains("REMINDER_PROCESSING_CRON")
                .contains("E18")
                .contains("FR-089")
                .contains("item 406");
    }

    @Test
    void documentsScheduledRunSecurityContextAndSendRules() throws Exception {
        String documentation = Files.readString(SCHEDULER_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Scheduled Run")
                .contains("processDueReminders")
                .contains("system Campaign Manager")
                .contains("ROLE_CAMPAIGN_MANAGER")
                .contains("00000000-0000-0000-0000-000000000000")
                .contains("finally")
                .contains("sendDueReminders")
                .contains("BR-024")
                .contains("Item 401")
                .contains("SENT");
    }

    @Test
    void documentsManualTriggerEnvironmentGuardAndApi() throws Exception {
        String documentation = Files.readString(SCHEDULER_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Manual Trigger")
                .contains("triggerManualProcessing")
                .contains("/api/reminders/due/manual-trigger")
                .contains("ADMIN")
                .contains("dev")
                .contains("test")
                .contains("prod")
                .contains("ForbiddenException")
                .contains("Manual reminder processing is available only in development or test")
                .contains("manual trigger blocked");
    }

    @Test
    void documentsSchedulerLoggingMarkersAndOutcomes() throws Exception {
        String documentation = Files.readString(SCHEDULER_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Scheduler Logging")
                .contains("LOG_PROCESSING_STARTED")
                .contains("LOG_REMINDER_ATTEMPT")
                .contains("LOG_PROCESSING_COMPLETED")
                .contains("LOG_PROCESSING_FAILED")
                .contains("Reminder scheduler processing started")
                .contains("Reminder scheduler reminder attempt")
                .contains("Reminder scheduler processing completed")
                .contains("Reminder scheduler processing failed")
                .contains("processedCount")
                .contains("sentCount")
                .contains("cancelledCount")
                .contains("failedCount")
                .contains("attemptCount=0")
                .contains("trigger=scheduled")
                .contains("trigger=manual");
    }

    @Test
    void documentsOperationalGuidanceAcceptanceAndTests() throws Exception {
        String documentation = Files.readString(SCHEDULER_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Operational Guidance")
                .contains("## Authorization Summary")
                .contains("## Acceptance Criteria")
                .contains("## Evidence For Demo And Review")
                .contains("ReminderProcessingSchedulerTests")
                .contains("SchedulerLogsReminderAttemptsTests")
                .contains("ReminderControllerTests")
                .contains("reminder-scheduling.md")
                .contains("green-yellow-red-reminder-rules.md");
    }

    @Test
    void schedulerSourceDocumentsCronAndLoggingMarkers() throws Exception {
        String source = Files.readString(SCHEDULER_SOURCE, StandardCharsets.UTF_8);

        assertThat(source)
                .contains("@Scheduled")
                .contains("PROCESSING_CRON")
                .contains("app.reminders.processing-cron")
                .contains("LOG_REMINDER_ATTEMPT")
                .contains("LOG_PROCESSING_STARTED")
                .contains("LOG_PROCESSING_COMPLETED")
                .contains("LOG_PROCESSING_FAILED")
                .contains("docs/modules/reminder-scheduler.md")
                .contains("FR-089");
    }

    @Test
    void applicationEnablesSchedulingForReminderScheduler() throws Exception {
        String application = Files.readString(APPLICATION_SOURCE, StandardCharsets.UTF_8);

        assertThat(application).contains("@EnableScheduling");
    }

    @Test
    void packageInfoAndReminderSchedulingDocLinkSchedulerDocumentation() throws Exception {
        String packageInfo = Files.readString(PACKAGE_INFO, StandardCharsets.UTF_8);
        String schedulingDoc = Files.readString(REMINDER_SCHEDULING_DOC, StandardCharsets.UTF_8);

        assertThat(packageInfo).contains("docs/modules/reminder-scheduler.md");
        assertThat(schedulingDoc).contains("reminder-scheduler.md");
    }

    @Test
    void documentationIndexLinksReminderSchedulerDocumentation() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("modules/reminder-scheduler.md")
                .contains("Reminder Scheduler");
    }
}
