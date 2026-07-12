package com.bayerwestphalian.campaign.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Sprint 16 critical test item <b>666</b>: Backup and restore process is documented and testable.
 *
 * <p>KB rules:
 *
 * <ul>
 *   <li>{@code NFR-013} — Database backup strategy
 *   <li>KB deployment: scheduled PostgreSQL backups; PostgreSQL is the system of record
 *   <li>Schema remains Flyway-versioned; dumps are operator-runnable and document-locked
 * </ul>
 *
 * <p>There is no in-app backup UI. Testability is provided by documentation tests plus companion
 * migration/Postgres integrity suites.
 */
@DisplayName("666 Backup and restore process is documented and testable")
class BackupAndRestoreProcessIsDocumentedAndTestableTests {

    private static final Path BACKUP_RESTORE_DOC =
            Path.of("../docs/deployment/backup-and-restore.md");
    private static final Path DOCKER_README = Path.of("../docker/README.md");
    private static final Path MIGRATION_STRATEGY =
            Path.of("../docs/database/migration-strategy.md");
    private static final Path PRODUCTION_CHECKLIST =
            Path.of("../docs/deployment/production-security-checklist.md");
    private static final Path DOCS_INDEX = Path.of("../docs/README.md");
    private static final Path COMPOSE = Path.of("../docker-compose.yml");

    @Nested
    @DisplayName("Primary runbook document")
    class PrimaryRunbook {

        @Test
        void backupAndRestoreDocumentExistsWithRequiredProcessSections() throws Exception {
            assertThat(BACKUP_RESTORE_DOC).exists();
            String doc = Files.readString(BACKUP_RESTORE_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("666")
                    .contains("NFR-013")
                    .contains("BackupAndRestoreProcessIsDocumentedAndTestableTests")
                    .contains("pg_dump")
                    .contains("pg_restore")
                    .contains("bwc_postgres_data")
                    .contains("bwc_campaign")
                    .contains("Flyway")
                    .contains("scheduled PostgreSQL backups")
                    .containsIgnoringCase("restore")
                    .containsIgnoringCase("backup")
                    .contains("Operator Checklist")
                    .contains("Testability");
        }

        @Test
        void documentsLogicalDumpAndRestoreCommands() throws Exception {
            String doc = Files.readString(BACKUP_RESTORE_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .contains("docker compose exec")
                    .contains("pg_dump")
                    .contains("psql")
                    .contains("--clean")
                    .contains("/actuator/health");
        }

        @Test
        void documentsWhatNotToDoWithSecretsAndGit() throws Exception {
            String doc = Files.readString(BACKUP_RESTORE_DOC, StandardCharsets.UTF_8);

            assertThat(doc)
                    .containsIgnoringCase("never committed")
                    .contains("JWT_SECRET")
                    .containsIgnoringCase("not treat Git history as a data backup");
        }
    }

    @Nested
    @DisplayName("Supporting infrastructure documentation")
    class SupportingDocs {

        @Test
        void dockerReadmeDocumentsPostgresVolumeAndWipeCaution() throws Exception {
            assertThat(DOCKER_README).exists();
            String docker = Files.readString(DOCKER_README, StandardCharsets.UTF_8);

            assertThat(docker)
                    .contains("bwc_postgres_data")
                    .contains("postgres")
                    .contains("docker compose down -v")
                    .contains("666");
        }

        @Test
        void composeDefinesNamedPostgresVolume() throws Exception {
            assertThat(COMPOSE).exists();
            String compose = Files.readString(COMPOSE, StandardCharsets.UTF_8);

            assertThat(compose)
                    .contains("postgres")
                    .contains("bwc_postgres_data")
                    .contains("POSTGRES_DB");
        }

        @Test
        void migrationStrategyRemainsVersionControlled() throws Exception {
            assertThat(MIGRATION_STRATEGY).exists();
            String migration = Files.readString(MIGRATION_STRATEGY, StandardCharsets.UTF_8);

            assertThat(migration)
                    .contains("Flyway")
                    .contains("db/migration")
                    .contains("Do not edit an applied migration");
        }

        @Test
        void productionChecklistLinksBackupAndRestoreProcess() throws Exception {
            assertThat(PRODUCTION_CHECKLIST).exists();
            String checklist =
                    Files.readString(PRODUCTION_CHECKLIST, StandardCharsets.UTF_8);

            assertThat(checklist)
                    .contains("666")
                    .contains("backup-and-restore.md")
                    .containsIgnoringCase("backup");
        }

        @Test
        void documentationIndexLinksBackupAndRestore() throws Exception {
            assertThat(DOCS_INDEX).exists();
            String index = Files.readString(DOCS_INDEX, StandardCharsets.UTF_8);

            assertThat(index).contains("deployment/backup-and-restore.md");
        }
    }
}
