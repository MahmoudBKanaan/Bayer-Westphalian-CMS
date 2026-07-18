package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionSmokeTestExecutionDocumentationTests {

    private static final Path EXECUTION =
            Path.of("../docs/deployment/smoke-test-executions/2026-07-12-item-738.md");
    private static final Path CHECKLIST =
            Path.of("../docs/deployment/production-smoke-test-checklist.md");

    @Test
    void blockedExecutionRecordsImmutableDeploymentEvidenceAndReason() throws Exception {
        String execution = DocumentationTestText.normalize(Files.readString(EXECUTION, StandardCharsets.UTF_8));

        assertThat(execution)
                .contains("item 738")
                .contains("2026-07-12T20:47:50Z")
                .contains("7cb7b01543fa533c38d2935cfe1236c8f20cecf2")
                .contains("Final decision | **BLOCKED**")
                .contains("No `bwc-production` project is running")
                .contains("curl` exit 7")
                .contains("This is an environment prerequisite failure")
                .contains("must not be considered releasable");
    }

    @Test
    void executionDoesNotMisrepresentDevelopmentServicesAsProductionEvidence() throws Exception {
        String execution = DocumentationTestText.normalize(Files.readString(EXECUTION, StandardCharsets.UTF_8));

        assertThat(execution)
                .contains("Vite development page, not production deployment")
                .contains("do not count as production smoke evidence")
                .contains("were not executed")
                .contains("BLOCKED**, not `PASS` and not `N/A`")
                .contains("No secret values");
    }

    @Test
    void checklistLinksTimestampedExecutionHistory() throws Exception {
        String checklist = DocumentationTestText.normalize(Files.readString(CHECKLIST, StandardCharsets.UTF_8));

        assertThat(checklist)
                .contains("Item 738 execution history")
                .contains("smoke-test-executions/2026-07-12-item-738.md")
                .contains("BLOCKED: production deployment unavailable");
    }
}
