/**
 * Sprint 16 critical test item **666**: Backup and restore process is documented and testable.
 *
 * KB: NFR-013 — database backup strategy. No in-app backup UI; ops runbook + documentation tests
 * + Flyway/Postgres integrity companions.
 */

export const BACKUP_AND_RESTORE_PROCESS_IS_DOCUMENTED_AND_TESTABLE_ITEM = 666;

export const BACKUP_AND_RESTORE_PROCESS_IS_DOCUMENTED_AND_TESTABLE_STATEMENT =
  "Backup and restore process is documented and testable";

export const BACKUP_AND_RESTORE_PROCESS_IS_DOCUMENTED_AND_TESTABLE_NFR = [
  "NFR-013",
] as const;

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.BackupAndRestoreProcessIsDocumentedAndTestableTests";

export const COMPANION_FLYWAY_TEST_CLASS =
  "com.bayerwestphalian.campaign.database.FlywayMigrationIntegrationTests";

export const COMPANION_POSTGRES_TEST_CLASS =
  "com.bayerwestphalian.campaign.PostgreSqlConnectionIntegrationTests";

export const BACKUP_AND_RESTORE_DOC_PATH = "docs/deployment/backup-and-restore.md";

export const DOCKER_README_DOC_PATH = "docker/README.md";

export const MIGRATION_STRATEGY_DOC_PATH = "docs/database/migration-strategy.md";

export const PRODUCTION_SECURITY_CHECKLIST_DOC_PATH =
  "docs/deployment/production-security-checklist.md";

/** Logical backup tools referenced by the runbook. */
export const BACKUP_TOOLS = ["pg_dump", "pg_restore", "psql"] as const;

/** Default local Compose database identity (must stay aligned with docker README). */
export const LOCAL_POSTGRES = {
  database: "bwc_campaign",
  user: "bwc_app",
  volume: "bwc_postgres_data",
  service: "postgres",
} as const;

/** Required sections in the backup-and-restore runbook. */
export const REQUIRED_RUNBOOK_SECTION_MARKERS = [
  "NFR-013",
  "pg_dump",
  "pg_restore",
  "Operator Checklist",
  "Testability",
  "Flyway",
  "bwc_postgres_data",
] as const;

/**
 * True when documentation text covers the core backup/restore contract for item 666.
 */
export function runbookCoversBackupAndRestore(doc: string): boolean {
  if (doc == null || doc.trim() === "") {
    return false;
  }
  return REQUIRED_RUNBOOK_SECTION_MARKERS.every((marker) => doc.includes(marker));
}

/**
 * True when a path is an acceptable dump artifact location (outside source control secrets).
 * Dumps should not be committed; operators keep them outside Git.
 */
export function isSafeOperatorDumpFilename(filename: string): boolean {
  const lower = filename.toLowerCase();
  if (lower.includes(".git/") || lower.startsWith("backend/src/")) {
    return false;
  }
  return (
    lower.endsWith(".sql") ||
    lower.endsWith(".dump") ||
    lower.endsWith(".backup") ||
    lower.endsWith(".tgz")
  );
}
