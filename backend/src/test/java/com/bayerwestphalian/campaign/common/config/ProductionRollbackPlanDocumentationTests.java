package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionRollbackPlanDocumentationTests {

    private static final Path PLAN = Path.of("../docs/deployment/rollback-plan.md");
    private static final Path INDEX = Path.of("../docs/README.md");

    @Test
    void rollbackPlanDefinesOwnershipTriggersContainmentAndEvidence() throws Exception {
        String plan = Files.readString(PLAN, StandardCharsets.UTF_8);

        assertThat(plan)
                .contains("Sprint 18 item 739")
                .contains("rollback operator")
                .contains("release/incident approver")
                .contains("System Auditor")
                .contains("Trigger And Immediate Containment")
                .contains("stop reverse-proxy frontend backend database-backup")
                .contains("Expected and actual RPO/RTO")
                .contains("Never record secret values");
    }

    @Test
    void rollbackPlanSeparatesCompatibleImagesStateRestoreAndForwardFix() throws Exception {
        String plan = Files.readString(PLAN, StandardCharsets.UTF_8);

        assertThat(plan)
                .contains("Path A: Application Image And Configuration Rollback")
                .contains("Path B: Database And Consent-Evidence Restore")
                .contains("Path C: Hold, Escalate, Or Forward-Fix")
                .contains("@sha256:<approved-digest>")
                .contains("up -d --no-build postgres backend")
                .contains("item 735")
                .contains("item 736")
                .contains("Production Database Restore");
    }

    @Test
    void rollbackPlanForbidsUnsafeFlywayAndRequiresFullValidation() throws Exception {
        String plan = Files.readString(PLAN, StandardCharsets.UTF_8);

        assertThat(plan)
                .contains("Never run `flyway clean`")
                .contains("delete or edit `flyway_schema_history`")
                .contains("forward-only")
                .contains("all Critical item 737 checks")
                .contains("Any failed Critical check returns the process to containment")
                .contains("Abort Criteria");
    }

    @Test
    void rollbackPlanIsIndexedAndLinkedFromSmokeGate() throws Exception {
        String index = Files.readString(INDEX, StandardCharsets.UTF_8);
        String smoke =
                Files.readString(
                        Path.of("../docs/deployment/production-smoke-test-checklist.md"),
                        StandardCharsets.UTF_8);

        assertThat(index).contains("deployment/rollback-plan.md").contains("item **739**");
        assertThat(smoke).contains("Production Rollback Plan").contains("rollback-plan.md");
    }
}
