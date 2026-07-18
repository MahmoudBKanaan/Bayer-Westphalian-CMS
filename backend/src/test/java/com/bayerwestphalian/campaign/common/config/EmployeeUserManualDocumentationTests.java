package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("776 Write user manual")
class EmployeeUserManualDocumentationTests {

    private static final Path MANUAL = Path.of("../docs/user-guides/user-manual.md");

    @Test
    void manualCoversSharedUiAndEndToEndEmployeeWorkflows() throws Exception {
        String manual = DocumentationTestText.normalize(Files.readString(MANUAL, StandardCharsets.UTF_8));

        assertThat(manual)
                .contains("Item 776")
                .contains("Sign in and sign out")
                .contains("Application layout")
                .contains("Customer and consent workflow")
                .contains("Products and ownership")
                .contains("Segments")
                .contains("Campaign lifecycle")
                .contains("Contact history, reminders, and follow-ups")
                .contains("Analytics and reports")
                .contains("Errors and support");
    }

    @Test
    void manualPreservesAuthorizationConsentAiAndDataSafety() throws Exception {
        String manual = DocumentationTestText.normalize(Files.readString(MANUAL, StandardCharsets.UTF_8));

        assertThat(manual)
                .contains("backend authorization remains decisive")
                .contains("EligibilityService")
                .contains("Approval or rejection is a human decision")
                .contains("AI cannot")
                .contains("immutable audit records")
                .contains("Never include passwords, JWTs")
                .contains("do not try alternate URLs/APIs");
    }
}
