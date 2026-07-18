package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("763 Smoke test checklist")
class FinalSmokeTestChecklistDocumentationTests {

    private static final Path CHECKLIST =
            Path.of("../docs/deployment/production-smoke-test-checklist.md");

    @Test
    void checklistIsTraceableAndBlocksOnFailedPreflightOrCriticalChecks() throws Exception {
        String checklist = DocumentationTestText.normalize(Files.readString(CHECKLIST, StandardCharsets.UTF_8));

        assertThat(checklist)
                .contains("items 737 and 763")
                .contains("Previous failed execution / retest reference")
                .contains("Browser / client version")
                .contains("Consent-evidence recovery point")
                .contains("CI passed for the deployed commit")
                .contains("A failed preflight is `BLOCKED`")
                .contains("Partial execution is never release evidence");
    }

    @Test
    void checklistCoversKbBusinessSecurityAndRecoveryJourneys() throws Exception {
        String checklist = DocumentationTestText.normalize(Files.readString(CHECKLIST, StandardCharsets.UTF_8));

        assertThat(checklist)
                .contains("Directly request an Admin API/UI route as Campaign Manager")
                .contains("Campaign Manager")
                .contains("Compliance Officer")
                .contains("System Auditor")
                .contains("EligibilityService")
                .contains("Human approval/rejection is required")
                .contains("backup creation verification")
                .contains("non-production restore rehearsal")
                .contains("operator and independent approver decision");
    }
}
