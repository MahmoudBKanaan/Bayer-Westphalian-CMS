package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionSmokeTestChecklistDocumentationTests {

    private static final Path CHECKLIST =
            Path.of("../docs/deployment/production-smoke-test-checklist.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");

    @Test
    void checklistDefinesTraceableExecutionAndHardReleaseDecision() throws Exception {
        String checklist = Files.readString(CHECKLIST, StandardCharsets.UTF_8);

        assertThat(checklist)
                .contains("Sprint 18 item 737")
                .contains("Release version / image digest")
                .contains("Git commit")
                .contains("Operator")
                .contains("Approver")
                .contains("PASS` / `BLOCKED")
                .contains("Partial execution is never release evidence");
    }

    @Test
    void checklistCoversInfrastructureSecurityAndCriticalWorkflows() throws Exception {
        String checklist = Files.readString(CHECKLIST, StandardCharsets.UTF_8);

        assertThat(checklist)
                .contains("SMK-002")
                .contains("SMK-011")
                .contains("SMK-024")
                .contains("SMK-033")
                .contains("SMK-043")
                .contains("SMK-046")
                .contains("SMK-054")
                .contains("SMK-063")
                .contains("SMK-064")
                .contains("SMK-073")
                .contains("EligibilityService")
                .contains("System Auditor");
    }

    @Test
    void checklistProtectsProductionDataAndRequiresCleanup() throws Exception {
        String checklist = Files.readString(CHECKLIST, StandardCharsets.UTF_8);

        assertThat(checklist)
                .contains("approved synthetic")
                .contains("Never alter a real customer")
                .contains("Do not launch a campaign to real recipients")
                .contains("Do not capture passwords, JWTs")
                .contains("SMK-070")
                .contains("cleanup actions are audited");
    }

    @Test
    void documentationIndexLinksItem737Checklist() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("deployment/production-smoke-test-checklist.md")
                .contains("item **737**");
    }
}
