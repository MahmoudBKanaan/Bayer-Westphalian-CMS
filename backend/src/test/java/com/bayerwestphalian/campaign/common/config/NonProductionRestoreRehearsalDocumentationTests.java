package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NonProductionRestoreRehearsalDocumentationTests {

    private static final Path SCRIPT = Path.of("../scripts/test-production-restore.ps1");
    private static final Path RUNBOOK = Path.of("../docs/deployment/backup-and-restore.md");

    @Test
    void rehearsalUsesDisposableIsolatedPostgresAndReadOnlyBackupMount() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("bwc-restore-rehearsal-")
                .contains("--network none")
                .contains("--tmpfs")
                .contains("dst=/backups,readonly")
                .contains("postgres:16-alpine")
                .contains("finally")
                .contains("docker rm -f")
                .doesNotContain("--publish")
                .doesNotContain("-p 5432");
    }

    @Test
    void rehearsalValidatesBackupRestoreMigrationsAndCoreSchema() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("sha256sum -c")
                .contains("pg_restore --list")
                .contains("pg_restore --exit-on-error --no-owner --no-privileges")
                .contains("flyway_schema_history")
                .contains("public.users")
                .contains("public.customers")
                .contains("public.campaigns")
                .contains("public.audit_logs")
                .contains("ON_ERROR_STOP=1");
    }

    @Test
    void runbookDocumentsItem736ReleaseEvidenceAndBlockingFailures() throws Exception {
        String runbook = Files.readString(RUNBOOK, StandardCharsets.UTF_8);

        assertThat(runbook)
                .contains("Non-production restore rehearsal (Item 736)")
                .contains("test-production-restore.ps1")
                .contains("never connects to or mutates the production PostgreSQL service")
                .contains("blocks release")
                .contains("Never attach the source dump");
    }
}
