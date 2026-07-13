package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionBackupCreationTestDocumentationTests {

    private static final Path SCRIPT = Path.of("../scripts/test-production-backup.ps1");
    private static final Path RUNBOOK = Path.of("../docs/deployment/backup-and-restore.md");

    @Test
    void backupVerificationTriggersAndDetectsANewRecoveryPoint() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("Invoke-Compose up -d postgres")
                .contains("Invoke-Compose restart database-backup")
                .contains("Where-Object { $_ -notin $before }")
                .contains("No new PostgreSQL dump was created")
                .contains("TimeoutSeconds");
    }

    @Test
    void backupVerificationChecksArtifactIntegrityWithoutDumpingRows() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("test -s")
                .contains("sha256sum -c")
                .contains("pg_restore --list")
                .contains("Size: $size bytes")
                .doesNotContain("pg_restore --data-only")
                .doesNotContain("psql -c");
    }

    @Test
    void runbookDocumentsItem735ExecutionEvidenceAndFailureCriteria() throws Exception {
        String runbook = Files.readString(RUNBOOK, StandardCharsets.UTF_8);

        assertThat(runbook)
                .contains("Backup creation verification (Item 735)")
                .contains("test-production-backup.ps1")
                .contains("non-empty")
                .contains("SHA-256 integrity")
                .contains("pg_restore --list")
                .contains("Do not attach the dump itself");
    }
}
