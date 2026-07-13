package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("764 Rollback plan")
class FinalRollbackPlanDocumentationTests {

    private static final Path PLAN = Path.of("../docs/deployment/rollback-plan.md");

    @Test
    void planRequiresRollbackReadinessBeforeDeployment() throws Exception {
        String plan = Files.readString(PLAN, StandardCharsets.UTF_8);

        assertThat(plan)
                .contains("items 739 and 764")
                .contains("Pre-Release Rollback Readiness")
                .contains("last known-good backend/frontend image digests")
                .contains("matching consent-evidence recovery point")
                .contains("non-production rehearsal")
                .contains("unknown schema compatibility")
                .contains("blocks deployment");
    }

    @Test
    void planDefinesHumanApprovedFailClosedRecoveryPaths() throws Exception {
        String plan = Files.readString(PLAN, StandardCharsets.UTF_8);

        assertThat(plan)
                .contains("automation and AI may provide evidence but may not approve it")
                .contains("Path A: Application Image And Configuration Rollback")
                .contains("Path B: Database And Consent-Evidence Restore")
                .contains("Path C: Hold, Escalate, Or Forward-Fix")
                .contains("Never run `flyway clean`")
                .contains("all Critical item 737 checks (the final item 763 checklist)")
                .contains("Any failed Critical check returns the process to containment");
    }
}
