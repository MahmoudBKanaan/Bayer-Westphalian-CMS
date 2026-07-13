# Backup and Restore Process

**Sprint 16 item 666** / **NFR-013** — Database backup strategy for the Bayer-Westphalian
Campaign Management Platform.

This document defines a **documented and testable** PostgreSQL backup and restore process for
project-scale and production-candidate environments. There is **no in-app backup UI**; operators
use standard PostgreSQL tooling against the system-of-record database.

Primary automated evidence:

| Layer | Location |
| --- | --- |
| Critical JUnit suite | `BackupAndRestoreProcessIsDocumentedAndTestableTests` |
| Companion integrity | `FlywayMigrationIntegrationTests`, `PostgreSqlConnectionIntegrationTests` |
| Frontend catalog | `frontend/src/features/ops/backupAndRestoreProcessIsDocumentedAndTestable.ts` |
| Local volume / Compose | `docker-compose.yml`, `docker/README.md` |
| Schema versioning | `docs/database/migration-strategy.md` |

## Scope

| In scope | Out of scope (MVP) |
| --- | --- |
| Logical backups of PostgreSQL (`pg_dump` / `pg_restore`) | Application-level “export everything” UI |
| Docker volume awareness (`bwc_postgres_data`) | Continuous multi-region replication |
| Restore to empty or replacement database | Point-in-time recovery (PITR) cloud products |
| Flyway re-apply after schema restore when required | Backing up secrets from the repo (secrets are never committed) |
| Verification steps (connection, Flyway, health) | Customer self-service restore |

## Why Backups Matter

PostgreSQL is the system of record (KB). Campaign, consent, audit, payment, and user data must be
recoverable after:

- Operator error or accidental `docker compose down -v`
- Host disk failure
- Failed upgrade / migration experiment
- Environment rebuild for production-candidate demos

Schema evolution remains **version-controlled via Flyway** only — restores must not rely on manual
production SQL outside migrations.

## What To Back Up

| Asset | Location / notes |
| --- | --- |
| **Database data** | PostgreSQL database (default local: `bwc_campaign`) |
| **Consent evidence files** | Production volume `bwc_consent_evidence`; back up with the matching database recovery point |
| **Docker volume** (local) | Named volume `bwc_postgres_data` (Compose) |
| **Migration scripts** | `backend/src/main/resources/db/migration` (in Git — not a DB dump) |
| **Environment / secrets** | Ops secret store only (`JWT_SECRET`, `DB_PASSWORD`, …) — **never** in Git ([secrets.md](secrets.md), item **689**) |

Do **not** treat Git history as a data backup. Do not treat Git history as a data backup. Do **not** commit dump files containing real
customer or credential data.

## Local Development Backup (Docker Compose)

Prerequisites:

- Compose Postgres is healthy (`docker compose ps`)
- Default service name: `postgres`
- Defaults: DB `bwc_campaign`, user `bwc_app` (see `docker/README.md`)

### Logical dump (recommended)

```bash
# From repository root — creates a plain-SQL dump
docker compose exec -T postgres pg_dump -U bwc_app -d bwc_campaign --clean --if-exists > backup-bwc.sql
```

Custom-format dump (supports selective restore / parallel restore):

```bash
docker compose exec -T postgres pg_dump -U bwc_app -d bwc_campaign -Fc -f /tmp/bwc.dump
docker compose cp postgres:/tmp/bwc.dump ./backup-bwc.dump
```

### Volume-level snapshot (optional, coarse)

```bash
docker compose stop postgres
docker run --rm -v bwc_postgres_data:/data -v "%CD%:/backup" alpine tar czf /backup/bwc_postgres_data.tgz -C /data .
docker compose start postgres
```

Use volume tarballs only when you understand they capture the entire data directory and must not be
mixed with concurrent writes.

## Restore Process

### A. Restore into a clean local database (logical dump)

1. Stop application backends that hold open connections (optional but recommended).
2. Ensure Postgres is running.
3. Recreate empty database **or** restore with `--clean` dump:

```bash
# Example: drop/recreate database (destroys current data)
docker compose exec -T postgres psql -U bwc_app -d postgres -c "DROP DATABASE IF EXISTS bwc_campaign;"
docker compose exec -T postgres psql -U bwc_app -d postgres -c "CREATE DATABASE bwc_campaign OWNER bwc_app;"

# Restore plain SQL dump
docker compose exec -T postgres psql -U bwc_app -d bwc_campaign < backup-bwc.sql
```

Custom format:

```bash
docker compose cp ./backup-bwc.dump postgres:/tmp/bwc.dump
docker compose exec -T postgres pg_restore -U bwc_app -d bwc_campaign --clean --if-exists /tmp/bwc.dump
```

4. Start the Spring Boot app with the correct JDBC URL so **Flyway** validates the restored schema
   version against `db/migration`.
5. Verify health: `GET /actuator/health` (or project health endpoint) returns up.

### B. After restore — schema and application rules

| Step | Rule |
| --- | --- |
| Flyway | Restored `flyway_schema_history` must match migration files in the deployed build |
| No manual prod DDL | If schema drift is required, add a **new** Flyway migration — never edit applied scripts |
| Secrets | Restore DB only; re-apply environment secrets from the secret store (item **665**) |
| Soft-delete data | Soft-deleted rows remain in dumps; active lists still filter `deletedAt` |

## Production-Candidate Strategy

The production Compose stack uses the stable volume name `bwc_postgres_prod_data`; see
[PostgreSQL Production Volume](postgres-production-volume.md) (item **720**). Production commands
must include `--env-file .env.production -f docker-compose.prod.yml`. Never use
`docker compose down -v` on the production stack.

For a production or release-candidate deployment:

1. **Schedule** regular logical backups (e.g. nightly `pg_dump -Fc`) to secure storage.
2. Retain at least **one successful full dump** plus the previous period (project policy: keep ≥ 7
   days of nightly dumps when disk allows).
3. Store dumps **encrypted at rest** and access-controlled (same trust boundary as `DB_PASSWORD`).
4. Document restore RTO/RPO targets for the university/project demo environment (MVP targets are
   project-scale: restore within operator shift; not multi-AZ SLA).
5. After restore, run application smoke: login, customer list, campaign list, health.

Automated cloud-native PITR is **optional** and out of MVP scope; logical dumps remain the
required baseline (KB: scheduled PostgreSQL backups).

## Automated Production Backup (Item 733)

`docker-compose.prod.yml` runs the isolated `database-backup` service after PostgreSQL becomes
healthy. The service executes `docker/postgres/backup.sh` immediately and then every
`BACKUP_INTERVAL_SECONDS` (24 hours by default). Each recovery point is:

1. Written to a `.partial` file using `pg_dump --format=custom`.
2. Validated with `pg_restore --list` before publication.
3. Atomically renamed to `<database>-<UTC timestamp>.dump`.
4. Accompanied by a SHA-256 manifest.
5. Retained for `BACKUP_RETENTION_DAYS` (7 days by default).

Backups live in the `bwc_postgres_backups` volume, separate from the live database volume. The
health check requires a recent `.last-success` marker. Container health and local retention do not
replace monitoring or an encrypted off-host copy; the volume is labeled
`com.bayer-westphalian.off-host-copy-required=true` to make that responsibility explicit.

```bash
docker compose --env-file .env.production -f docker-compose.prod.yml logs database-backup
docker run --rm -v bwc_postgres_backups:/backups alpine ls -lh /backups
```

Do not expose the backup service on a host port. `PGPASSWORD` is injected from `DB_PASSWORD`, is
not written into dump files, and must come from the deployment secret manager.

### Backup creation verification (Item 735)

Run the non-destructive verification from the repository root against a production-like environment:

```powershell
.\scripts\test-production-backup.ps1 -EnvFile .env.production
```

The script records the existing dump names, starts PostgreSQL, restarts `database-backup` to trigger
an immediate backup, and waits up to 180 seconds for a new uniquely named `.dump`. It then verifies
that the artifact and manifest are non-empty, checks SHA-256 integrity, and confirms the custom
archive is readable with `pg_restore --list`. It prints only the filename, byte size, and validation
status; it does not read rows or expose credentials. The successful dump remains in
`bwc_postgres_backups` as test evidence and is removed later by configured retention.

Failure is explicit when no new archive appears, the dump is empty, its manifest is missing or
invalid, or PostgreSQL cannot parse the archive. Capture the sanitized output and backup-worker logs
in the release evidence. Do not attach the dump itself because it contains production data.

### Backup existence verification (Item 757)

The release gate must prove a recent recovery artifact exists, not merely that backup configuration
is present:

```powershell
.\scripts\test-production-backup-exists.ps1 `
  -BackupVolume bwc_postgres_backups `
  -MaximumAgeHours 26
```

The verifier is read-only, has networking disabled, ignores `.partial` files, selects the newest
completed `.dump`, and requires non-zero size, a matching non-empty `.sha256` manifest, successful
checksum verification, `pg_restore --list` readability, and age within the configured threshold. It
prints only filename, size, freshness window, and validation status.

Execution at `2026-07-13T00:27:39+03:00` is **BLOCKED**: Docker contains no backup-named volume,
including no `bwc_postgres_backups`, so no completed dump/checksum can be verified. Existing database
data volumes are not backup evidence. Run item 735 after the production stack is configured, copy the
artifact off-host, then rerun item 757 and retain sanitized output.

### Non-production restore rehearsal (Item 736)

After item 735 has produced a verified backup, test restoration in an ephemeral environment:

```powershell
.\scripts\test-production-restore.ps1 -BackupVolume bwc_postgres_backups
# To select a specific recovery point:
.\scripts\test-production-restore.ps1 -BackupVolume bwc_postgres_backups `
  -DumpName bwc_campaign-YYYYMMDDTHHMMSSZ.dump
```

The verifier never connects to or mutates the production PostgreSQL service. It validates the dump
and SHA-256 manifest, starts a PostgreSQL 16 rehearsal container with networking disabled, stores
its disposable data directory in `tmpfs`, and mounts the backup volume read-only. It restores with
`--exit-on-error --no-owner --no-privileges`, confirms successful Flyway history and the `users`,
`customers`, `campaigns`, and `audit_logs` tables, and removes the container in a `finally` block.

Run this rehearsal before release and after material PostgreSQL or migration changes. Record the
sanitized artifact name, successful Flyway-entry count, core-schema result, execution time, operator,
and release/change identifier. A failed checksum, failed restore, absent migration history, or missing
core table blocks release until investigated. Never attach the source dump or temporary database.

## Production Database Restore (Item 734)

This procedure is destructive and must be performed by an authorized operator during an approved
maintenance window. Restore into a non-production rehearsal database first whenever possible.
Record the incident/change identifier, selected UTC recovery point, operator, start time, and
expected RPO before continuing.

### Preconditions and recovery-point verification

1. Confirm the target environment and database name. Never rely on an implicit Compose project.
2. Confirm a matching consent-evidence recovery point is available when restoring records that
   reference files in `bwc_consent_evidence`.
3. Select one completed `.dump` file; never restore a `.partial` file.
4. Verify its checksum and archive table of contents without printing database rows:

```bash
export COMPOSE_FILE=docker-compose.prod.yml
export ENV_FILE=.env.production
export RESTORE_DUMP=/backups/bwc_campaign-YYYYMMDDTHHMMSSZ.dump

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" run --rm --no-deps \
  --entrypoint sh database-backup -c \
  'sha256sum -c "$1.sha256" && pg_restore --list "$1" >/dev/null' -- "$RESTORE_DUMP"
```

Stop immediately if checksum or archive validation fails. Preserve the current database and select
another verified recovery point; do not attempt to repair a dump in place.

### Controlled production restore

1. Put the service in maintenance mode at the load balancer or reverse proxy.
2. Stop writers and the backup scheduler. Keep PostgreSQL running:

```bash
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" \
  stop reverse-proxy frontend backend database-backup
```

3. Take a final pre-restore backup when the incident permits it. Copy it off-host and do not allow
   retention cleanup to remove it until the restore is accepted.
4. Recreate the target database using the database name and owner already injected into the
   PostgreSQL service. The `--force` option terminates remaining sessions:

```bash
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T postgres sh -eu -c '
  dropdb --force --if-exists -U "$POSTGRES_USER" "$POSTGRES_DB"
  createdb -U "$POSTGRES_USER" -O "$POSTGRES_USER" "$POSTGRES_DB"
'
```

5. Restore the verified custom-format archive. `--exit-on-error` prevents a partially failed
   restore from being mistaken for success:

```bash
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" run --rm --no-deps \
  --entrypoint sh database-backup -eu -c \
  'pg_restore --exit-on-error --no-owner --no-privileges --dbname="$PGDATABASE" "$1"' \
  -- "$RESTORE_DUMP"
```

Do not start the application if `pg_restore` exits non-zero. Preserve logs, recreate the empty
database, and retry only with a known-good archive or invoke the rollback/incident plan.

### Validation and return to service

1. Start the backend first. Its Flyway startup validation must succeed before other services start:

```bash
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d backend
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --since=10m backend
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T backend \
  wget -qO- http://localhost:8080/actuator/health/readiness
```

2. Confirm `flyway_schema_history` has no failed migration and record its current version:

```bash
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T postgres sh -eu -c '
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 -c \
  "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
'
```

3. Restore the matching consent-evidence snapshot when applicable, before reopening customer or
   consent workflows.
4. Start `frontend` and `reverse-proxy`, then run smoke checks for login, customer lookup, consent
   evidence access, campaign listing, audit history, and HTTPS health.
5. Start `database-backup` only after the restored database is accepted. Confirm it creates a new
   post-restore recovery point without overwriting the archive used for recovery.

```bash
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d frontend reverse-proxy
# Run and record the approved smoke checklist before restarting scheduled backups.
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d database-backup
```

6. End maintenance mode and record command exit codes, checksum result, Flyway version, health
   output, smoke-test result, restored timestamp, actual RPO, and approver in the incident/change
   record. Do not place dump contents, passwords, tokens, or connection strings in evidence.

### Abort criteria

Keep the system in maintenance mode and escalate when the archive checksum fails, `pg_restore`
fails, Flyway reports schema incompatibility, consent evidence does not match the recovery point,
health remains down, or a critical smoke test fails. A technically completed restore is not an
approved return to service until all validation and business checks pass.

## Testability (How This Process Is Proven Without Running Live Dumps in CI)

Items **666**, **733**, and **734** require the process to be documented and testable. Evidence layers:

| Evidence | What it proves |
| --- | --- |
| This document + docs index link | Process exists and is discoverable |
| `BackupAndRestoreProcessIsDocumentedAndTestableTests` | Required sections and commands are present |
| `FlywayMigrationIntegrationTests` | Schema is migration-controlled (restore integrity) |
| `PostgreSqlConnectionIntegrationTests` | Postgres connectivity contract for dump/restore targets |
| `docker/README.md` + Compose volume name | Local data location is known |
| Frontend catalog `backupAndRestoreProcessIsDocumentedAndTestable` | KB identity and doc paths locked |

CI does **not** need to execute destructive `pg_restore` against shared databases. The dump/restore
commands above are operator-runnable; documentation tests lock the runbook.

## Operator Checklist

- [ ] Identify database name, user, and host (Compose local vs production JDBC URL)
- [ ] Take a logical backup before risky migrations or demos
- [ ] Store backup outside the Git working tree
- [ ] Verify dump file is non-empty and dated
- [ ] Practice restore on a **non-production** database at least once before release candidate
- [ ] After restore: app starts, Flyway succeeds, health is UP, sample login works
- [ ] Secrets re-applied from secret store (never restored from Git)

## Related Documentation

- [Migration Strategy](../database/migration-strategy.md)
- [Docker / local PostgreSQL](../../docker/README.md)
- [Production Security Checklist](production-security-checklist.md) (env/secrets)
- [Developer Setup](../development/developer-setup.md)
- NFR map item **666** / **NFR-013**
