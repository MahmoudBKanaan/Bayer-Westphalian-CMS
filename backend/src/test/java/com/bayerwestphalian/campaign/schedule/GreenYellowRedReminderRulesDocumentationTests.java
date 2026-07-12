package com.bayerwestphalian.campaign.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * KB item 405: Green/Yellow/Red reminder rules documentation covers BR-020–BR-022 payment
 * escalation, BR-023 expiration level mapping, and implementation references.
 */
class GreenYellowRedReminderRulesDocumentationTests {

    private static final Path RULES_DOC =
            Path.of("../docs/modules/green-yellow-red-reminder-rules.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path REMINDER_SCHEDULING_DOC =
            Path.of("../docs/modules/reminder-scheduling.md");
    private static final Path LEVEL_RULES_SOURCE =
            Path.of(
                    "src/main/java/com/bayerwestphalian/campaign/schedule/PaymentReminderLevelRules.java");

    @Test
    void documentsGreenYellowRedBusinessRulesAndLevels() throws Exception {
        String documentation = Files.readString(RULES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("# Green / Yellow / Red Reminder Rules")
                .contains("BR-020")
                .contains("BR-021")
                .contains("BR-022")
                .contains("BR-023")
                .contains("BR-024")
                .contains("FR-081")
                .contains("FR-082")
                .contains("FR-083")
                .contains("FR-084")
                .contains("TC-007")
                .contains("GREEN")
                .contains("YELLOW")
                .contains("RED")
                .contains("likely default risk");
    }

    @Test
    void documentsPaymentEscalationAlgorithmAndConstants() throws Exception {
        String documentation = Files.readString(RULES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("PaymentReminderLevelRules")
                .contains("FIRST_REMINDER_COUNT")
                .contains("SECOND_REMINDER_COUNT")
                .contains("THIRD_REMINDER_COUNT")
                .contains("resolve(PaymentRecord")
                .contains("resolveFromReminderCount")
                .contains("isFirstReminder")
                .contains("isSecondReminder")
                .contains("isThirdReminder")
                .contains("reminder_count == 0")
                .contains("reminder_count == 1")
                .contains("reminder_count >= 2")
                .contains("DEFAULT_RISK")
                .contains("Green → Yellow → Red");
    }

    @Test
    void documentsProductExpirationLevelMapping() throws Exception {
        String documentation = Files.readString(RULES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("ProductExpirationReminderRules")
                .contains("3 months")
                .contains("6 months")
                .contains("12 months")
                .contains("threeMonthReminderLevel")
                .contains("sixMonthReminderLevel")
                .contains("twelveMonthReminderLevel")
                .contains("/expiration/3-month/generate")
                .contains("/expiration/6-month/generate")
                .contains("/expiration/12-month/generate")
                .contains("window-based")
                .contains("count-based");
    }

    @Test
    void documentsUiImplementationAndAcceptanceCriteria() throws Exception {
        String documentation = Files.readString(RULES_DOC, StandardCharsets.UTF_8);

        assertThat(documentation)
                .contains("ReminderLevelBadge.tsx")
                .contains("## Acceptance Criteria")
                .contains("Green is resolved when")
                .contains("Yellow is resolved when")
                .contains("Red is resolved when")
                .contains("Default-risk payment status always resolves to Red")
                .contains("PaymentReminderLevelRulesTests")
                .contains("GreenReminderIsFirstReminderTests")
                .contains("YellowReminderIsSecondReminderTests")
                .contains("RedReminderIsThirdReminderTests")
                .contains("reminder-scheduling.md")
                .contains("payment-records.md");
    }

    @Test
    void paymentReminderLevelRulesSourceDocumentsKbRules() throws Exception {
        String source = Files.readString(LEVEL_RULES_SOURCE, StandardCharsets.UTF_8);

        assertThat(source)
                .contains("BR-020")
                .contains("BR-021")
                .contains("BR-022")
                .contains("FIRST_REMINDER_COUNT")
                .contains("SECOND_REMINDER_COUNT")
                .contains("THIRD_REMINDER_COUNT")
                .contains("DEFAULT_RISK")
                .contains("docs/modules/green-yellow-red-reminder-rules.md");
    }

    @Test
    void reminderSchedulingDocLinksGreenYellowRedRules() throws Exception {
        String schedulingDoc = Files.readString(REMINDER_SCHEDULING_DOC, StandardCharsets.UTF_8);

        assertThat(schedulingDoc)
                .contains("green-yellow-red-reminder-rules.md")
                .contains("BR-020")
                .contains("BR-021")
                .contains("BR-022");
    }

    @Test
    void documentationIndexLinksGreenYellowRedReminderRules() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("modules/green-yellow-red-reminder-rules.md")
                .contains("Green")
                .contains("Yellow")
                .contains("Red");
    }
}
