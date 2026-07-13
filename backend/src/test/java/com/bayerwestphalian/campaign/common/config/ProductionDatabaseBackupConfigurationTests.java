package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductionDatabaseBackupConfigurationTests {

    private static final Path COMPOSE = Path.of("../docker-compose.prod.yml");
    private static final Path SCRIPT = Path.of("../docker/postgres/backup.sh");
    private static final Path RUNBOOK = Path.of("../docs/deployment/backup-and-restore.md");

    @Test
    void productionComposeSchedulesCredentialSafePersistentBackups() throws Exception {
        String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);

        assertThat(compose)
                .contains("database-backup:")
                .contains("condition: service_healthy")
                .contains("PGPASSWORD: ${DB_PASSWORD:?DB_PASSWORD is required}")
                .contains("BACKUP_INTERVAL_SECONDS")
                .contains("BACKUP_RETENTION_DAYS")
                .contains("BACKUP_HEALTH_MAX_AGE_MINUTES")
                .contains("bwc_postgres_backups:/backups")
                .contains("internal: true")
                .doesNotContain("database-backup:\n    ports:");
    }

    @Test
    void backupScriptPublishesOnlyValidatedAtomicDumpsWithChecksums() throws Exception {
        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);

        assertThat(script)
                .contains("pg_dump --format=custom")
                .contains("pg_restore --list")
                .contains(".partial")
                .contains("sha256sum")
                .contains(".last-success")
                .contains("-mtime")
                .contains("BACKUP_RETENTION_DAYS");
    }

    @Test
    void runbookDocumentsScheduleRetentionVerificationAndOffHostRequirement() throws Exception {
        String runbook = Files.readString(RUNBOOK, StandardCharsets.UTF_8);

        assertThat(runbook)
                .contains("Automated Production Backup (Item 733)")
                .contains("BACKUP_INTERVAL_SECONDS")
                .contains("BACKUP_RETENTION_DAYS")
                .contains("pg_restore --list")
                .contains("SHA-256")
                .contains("encrypted off-host copy");
    }
}
