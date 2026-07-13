package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("765 Operations guide")
class ProductionOperationsGuideDocumentationTests {

    private static final Path GUIDE = Path.of("../docs/operations/operations-guide.md");

    @Test
    void guideDefinesOwnershipHandoverAndRoutineOperations() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Sprint 18 item 765")
                .contains("NFR-004")
                .contains("NFR-013")
                .contains("NFR-014")
                .contains("Roles and handover")
                .contains("Start-of-shift checks")
                .contains("Routine schedule")
                .contains("End-of-shift and evidence")
                .contains("99% target");
    }

    @Test
    void guideCoversSecurityChangesProvidersRecoveryAndIncidents() throws Exception {
        String guide = Files.readString(GUIDE, StandardCharsets.UTF_8);

        assertThat(guide)
                .contains("Controlled changes and maintenance")
                .contains("Access and administrator operations")
                .contains("Scheduler and provider operations")
                .contains("Data, backup, and recovery operations")
                .contains("Alert and incident response")
                .contains("Never automatically replay sends")
                .contains("Never run `flyway clean`")
                .contains("all critical smoke checks");
    }
}
