import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_CRITICAL_TEST_CLASS,
  BACKUP_AND_RESTORE_DOC_PATH,
  BACKUP_AND_RESTORE_PROCESS_IS_DOCUMENTED_AND_TESTABLE_ITEM,
  BACKUP_AND_RESTORE_PROCESS_IS_DOCUMENTED_AND_TESTABLE_NFR,
  BACKUP_AND_RESTORE_PROCESS_IS_DOCUMENTED_AND_TESTABLE_STATEMENT,
  BACKUP_TOOLS,
  COMPANION_FLYWAY_TEST_CLASS,
  COMPANION_POSTGRES_TEST_CLASS,
  DOCKER_README_DOC_PATH,
  LOCAL_POSTGRES,
  MIGRATION_STRATEGY_DOC_PATH,
  PRODUCTION_SECURITY_CHECKLIST_DOC_PATH,
  REQUIRED_RUNBOOK_SECTION_MARKERS,
  isSafeOperatorDumpFilename,
  runbookCoversBackupAndRestore,
} from "@/features/ops/backupAndRestoreProcessIsDocumentedAndTestable";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("backupAndRestoreProcessIsDocumentedAndTestable (item 666)", () => {
  it("locks the critical KB rule identity", () => {
    expect(BACKUP_AND_RESTORE_PROCESS_IS_DOCUMENTED_AND_TESTABLE_ITEM).toBe(666);
    expect(BACKUP_AND_RESTORE_PROCESS_IS_DOCUMENTED_AND_TESTABLE_STATEMENT).toBe(
      "Backup and restore process is documented and testable",
    );
    expect(BACKUP_AND_RESTORE_PROCESS_IS_DOCUMENTED_AND_TESTABLE_NFR).toEqual(["NFR-013"]);
    expect(BACKUP_TOOLS).toEqual(["pg_dump", "pg_restore", "psql"]);
    expect(LOCAL_POSTGRES.database).toBe("bwc_campaign");
    expect(LOCAL_POSTGRES.volume).toBe("bwc_postgres_data");
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain(
      "BackupAndRestoreProcessIsDocumentedAndTestableTests",
    );
    expect(COMPANION_FLYWAY_TEST_CLASS).toContain("FlywayMigrationIntegrationTests");
    expect(COMPANION_POSTGRES_TEST_CLASS).toContain("PostgreSqlConnectionIntegrationTests");
    expect(REQUIRED_RUNBOOK_SECTION_MARKERS).toContain("pg_dump");
  });

  it("classifies dump filenames and requires full runbook coverage", () => {
    expect(isSafeOperatorDumpFilename("backup-bwc.sql")).toBe(true);
    expect(isSafeOperatorDumpFilename("backup-bwc.dump")).toBe(true);
    expect(isSafeOperatorDumpFilename("backend/src/main/resources/secret.sql")).toBe(false);
    expect(runbookCoversBackupAndRestore("")).toBe(false);
  });

  it("documents backup and restore in runbook, docker, migration, and checklist docs", () => {
    const runbookPath = path.join(repoRoot, BACKUP_AND_RESTORE_DOC_PATH);
    const dockerPath = path.join(repoRoot, DOCKER_README_DOC_PATH);
    const migrationPath = path.join(repoRoot, MIGRATION_STRATEGY_DOC_PATH);
    const checklistPath = path.join(repoRoot, PRODUCTION_SECURITY_CHECKLIST_DOC_PATH);

    expect(existsSync(runbookPath)).toBe(true);
    expect(existsSync(dockerPath)).toBe(true);
    expect(existsSync(migrationPath)).toBe(true);
    expect(existsSync(checklistPath)).toBe(true);

    const runbook = readRepoFile(BACKUP_AND_RESTORE_DOC_PATH);
    expect(runbook).toContain("666");
    expect(runbook).toContain("BackupAndRestoreProcessIsDocumentedAndTestableTests");
    expect(runbookCoversBackupAndRestore(runbook)).toBe(true);

    const docker = readRepoFile(DOCKER_README_DOC_PATH);
    expect(docker).toContain("666");
    expect(docker).toContain("bwc_postgres_data");

    const migration = readRepoFile(MIGRATION_STRATEGY_DOC_PATH);
    expect(migration).toMatch(/Flyway|migration/i);

    const checklist = readRepoFile(PRODUCTION_SECURITY_CHECKLIST_DOC_PATH);
    expect(checklist).toContain("666");
    expect(checklist).toContain("backup-and-restore.md");
  });
});
