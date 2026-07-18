package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("778 Write operations manual")
class FinalOperationsManualDocumentationTests {

    private static final Path MANUAL = Path.of("../docs/operations/operations-guide.md");

    @Test
    void manualDefinesSafeServiceLifecycleAndValidation() throws Exception {
        String manual = DocumentationTestText.normalize(Files.readString(MANUAL, StandardCharsets.UTF_8));

        assertThat(manual)
                .contains("Item 778")
                .contains("Service lifecycle")
                .contains("Inspect without changing state")
                .contains("Planned start")
                .contains("Planned restart")
                .contains("Planned shutdown")
                .contains("Emergency containment")
                .contains("up -d --no-build postgres backend")
                .contains("stop reverse-proxy frontend backend database-backup")
                .contains("Never use `docker compose down -v`");
    }

    @Test
    void manualRoutesOperatorsToAuthoritativeRunbooks() throws Exception {
        String manual = DocumentationTestText.normalize(Files.readString(MANUAL, StandardCharsets.UTF_8));

        assertThat(manual)
                .contains("Runbook selection")
                .contains("item 770 release gate")
                .contains("Administrator Manual")
                .contains("Production Backup Guide")
                .contains("Production Restore Guide")
                .contains("Rollback Plan")
                .contains("Incident Response Notes")
                .contains("Production Smoke Test Checklist")
                .contains("human recovery approval take priority");
    }
}
