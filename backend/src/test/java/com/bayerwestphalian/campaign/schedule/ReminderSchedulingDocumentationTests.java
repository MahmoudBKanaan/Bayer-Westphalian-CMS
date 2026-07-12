package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * KB item 404: Reminder scheduling documentation exists and covers package boundary, API, payment
 * and expiration rules, eligibility, scheduler logging, authorization, and acceptance criteria for
 * epic E18.
 */
class ReminderSchedulingDocumentationTests {

    private static final Path REMINDER_SCHEDULING_DOC =
            Path.of("../docs/modules/reminder-scheduling.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path PACKAGE_INFO =
            Path.of("src/main/java/com/bayerwestphalian/campaign/schedule/package-info.java");

    @Test
    void documentsReminderSchedulingModuleBoundaryAndApiSurface() throws Exception {
        String documentation = Files.readString(REMINDER_SCHEDULING_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("# Reminder Scheduling Documentation")
                .contains("## Package Boundary")
                .contains("com.bayerwestphalian.campaign.schedule")
                .contains("ReminderSchedule")
                .contains("ReminderRepository")
                .contains("ReminderService")
                .contains("ReminderController")
                .contains("ReminderProcessingScheduler")
                .contains("PaymentReminderLevelRules")
                .contains("ProductExpirationReminderRules")
                .contains("reminder_schedules")
                .contains("/api/reminders")
                .contains("## REST API")
                .contains("/payment/generate")
                .contains("/expiration/3-month/generate")
                .contains("/expiration/6-month/generate")
                .contains("/expiration/12-month/generate")
                .contains("/due/send")
                .contains("/due/manual-trigger")
                .contains("/sent")
                .contains("/cancel")
                .contains("customerId")
                .contains("dueOnOrBefore")
                .contains("asOfDate");
    }

    @Test
    void documentsPaymentAndExpirationBusinessRules() throws Exception {
        String documentation = Files.readString(REMINDER_SCHEDULING_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Data Model")
                .contains("## Domain Rules")
                .contains("PAYMENT_DUE")
                .contains("PRODUCT_EXPIRATION")
                .contains("GREEN")
                .contains("YELLOW")
                .contains("RED")
                .contains("PENDING")
                .contains("SENT")
                .contains("CANCELLED")
                .contains("FAILED")
                .contains("BR-020")
                .contains("BR-021")
                .contains("BR-022")
                .contains("BR-023")
                .contains("BR-024")
                .contains("PAYMENT_REMINDER_PAYMENT_COMPLETED")
                .contains("3 months")
                .contains("6 months")
                .contains("12 months")
                .contains("findExpiringBetween")
                .contains("E18");
    }

    @Test
    void documentsConsentContactLimitsAndSchedulerLogging() throws Exception {
        String documentation = Files.readString(REMINDER_SCHEDULING_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("evaluateForReminder")
                .contains("MARKETING_EMAIL")
                .contains("REMINDER_RECIPIENT_INELIGIBLE")
                .containsIgnoringCase("monthly")
                .contains("FR-089")
                .contains("item 401")
                .contains("item 402")
                .contains("app.reminders.processing-cron")
                .contains("reminder attempt")
                .contains("processedCount")
                .contains("sentCount")
                .contains("cancelledCount")
                .contains("failedCount")
                .contains("dev")
                .contains("test");
    }

    @Test
    void documentsReminderAuthorizationFrontendAndAcceptanceCriteria() throws Exception {
        String documentation = Files.readString(REMINDER_SCHEDULING_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("## Authorization")
                .contains("Spring Security")
                .contains("ADMIN")
                .contains("CAMPAIGN_MANAGER")
                .contains("CUSTOMER_SERVICE_AGENT")
                .contains("SALES_AGENT")
                .contains("COMPLIANCE_OFFICER")
                .contains("## Frontend Boundary")
                .contains("RemindersPage.tsx")
                .contains("ReminderLevelBadge.tsx")
                .contains("frontend/src/api/reminders.ts")
                .contains("## Acceptance Criteria")
                .contains("Payment due reminder is generated")
                .contains("Green is the first payment reminder")
                .contains("Payment reminder is not sent if payment is completed")
                .contains("Product-expiration reminder is generated 3, 6, and 12 months")
                .contains("Reminder respects consent and contact limits")
                .contains("Scheduler logs reminder attempts")
                .contains("## Production Gate")
                .contains("item 409")
                .contains("Payment status")
                .contains("Expiration dates")
                .contains("Contact frequency limits")
                .contains("ReminderLogicRespectsConsentPaymentExpirationAndContactLimitsTests")
                .contains("payment-records.md")
                .contains("product-ownership.md")
                .contains("follow-up-tasks.md")
                .contains("eligibility-rules.md")
                .contains("green-yellow-red-reminder-rules.md")
                .contains("reminder-scheduler.md");
    }

    @Test
    void schedulePackageInfoReferencesModuleDocumentation() throws Exception {
        String packageInfo = Files.readString(PACKAGE_INFO, StandardCharsets.UTF_8);

        assertThat(packageInfo)
                .contains("docs/modules/reminder-scheduling.md")
                .contains("E18")
                .contains("FR-089");
    }

    @Test
    void documentationIndexLinksReminderSchedulingDocumentation() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("modules/reminder-scheduling.md")
                .contains("Reminder Scheduling");
    }
}
