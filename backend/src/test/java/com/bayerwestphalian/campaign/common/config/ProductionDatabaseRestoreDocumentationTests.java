package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionDatabaseRestoreDocumentationTests {

    private static final Path RUNBOOK = Path.of("../docs/deployment/backup-and-restore.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");

    @Test
    void productionRestoreDocumentsVerifiedControlledRestoreSequence() throws Exception {
        String runbook = Files.readString(RUNBOOK, StandardCharsets.UTF_8);

        assertThat(runbook)
                .contains("Production Database Restore (Item 734)")
                .contains("maintenance mode")
                .contains("sha256sum -c")
                .contains("pg_restore --list")
                .contains("stop reverse-proxy frontend backend database-backup")
                .contains("dropdb --force --if-exists")
                .contains("--exit-on-error")
                .contains("--no-owner --no-privileges")
                .contains("actuator/health/readiness")
                .contains("flyway_schema_history");
    }

    @Test
    void productionRestoreDefinesSafetyAbortAndEvidenceRequirements() throws Exception {
        String runbook = Files.readString(RUNBOOK, StandardCharsets.UTF_8);

        assertThat(runbook)
                .contains("matching consent-evidence recovery point")
                .contains("never restore a `.partial` file")
                .contains("Abort criteria")
                .contains("critical smoke test fails")
                .contains("actual RPO")
                .contains("Do not place dump contents, passwords, tokens, or connection strings in evidence");
    }

    @Test
    void documentationIndexIdentifiesItem734() throws Exception {
        String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

        assertThat(index)
                .contains("deployment/backup-and-restore.md")
                .contains("**734**")
                .contains("NFR-013");
    }
}
