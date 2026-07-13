# Production Restore Guide

**Sprint 18 item 762** defines the controlled restore runbook for the Bayer-Westphalian Campaign
Management Platform and supports KB **NFR-013 (backup/recovery)**. A production restore is
destructive: it can remove all writes after the selected recovery point. Only an authorized human
operator may execute it after an approver accepts the expected RPO and recovery scope.

Use this guide for database or persistent consent-evidence loss/corruption. For an application-only
regression with compatible data, prefer image rollback under the [rollback plan](rollback-plan.md).

## Roles and restore record

Assign a restore operator, recovery approver, business/data owner, and evidence reviewer. One person
may fill multiple roles in this solo project, but each decision must be recorded separately.

Before execution, record:

| Field | Required value |
| --- | --- |
| Incident/change ID and target environment | Unambiguous production identifier |
| Failure and containment times | UTC |
| Selected database archive | Filename, UTC recovery point, size, checksum result |
| Consent-evidence recovery point | Matching snapshot/object identifier |
| Current/target release and Flyway versions | Tag, commit, immutable image digests |
| Expected RPO/RTO and accepted data loss | Human approver decision |
| Operator, approver, and business owner | Named identities |
| Restore start/end and outcome | `RESTORED`, `ABORTED`, or `ESCALATED` |

Never record dump contents, customer data, passwords, tokens, connection strings, signed storage
URLs, or consent-evidence contents.

## 1. Decide and contain

Restore only for confirmed corruption, unrecoverable deletion, failed incompatible data migration,
or infrastructure loss where forward repair is less safe. Do not restore merely because a container
or application image failed.

1. Declare maintenance mode and stop public traffic, application writers, scheduler processing,
   communication providers, and backup retention/rotation. Keep PostgreSQL available for diagnosis
   when safe.
2. Preserve sanitized logs, request IDs, audit/provider evidence, image digests, Compose state,
   current Flyway history, and the incident timeline.
3. Take a final pre-restore backup when database integrity and incident conditions permit. Mark it
   as incident evidence and copy it off-host.
4. Confirm reconciliation for post-recovery customer updates, consent changes, approvals, audit
   events, and communications. Never automatically replay campaign sends.

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml `
  stop reverse-proxy frontend backend database-backup
```

## 2. Select and verify the recovery point

Select a completed `.dump`, never a `.partial` file. It must have passed backup creation verification,
freshness checks, off-host copy verification, and a non-production restore rehearsal. Select the
consent-evidence snapshot from the same recovery point whenever database records reference files.

Verify the archive again using a read-only mount before touching production:

```bash
export COMPOSE_FILE=docker-compose.prod.yml
export ENV_FILE=.env.production
export RESTORE_DUMP=/backups/bwc_campaign-YYYYMMDDTHHMMSSZ.dump

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" run --rm --no-deps \
  --entrypoint sh database-backup -c \
  'sha256sum -c "$1.sha256" && pg_restore --list "$1" >/dev/null' -- "$RESTORE_DUMP"
```

Stop if the manifest is absent, checksum fails, archive is unreadable, PostgreSQL major-version
compatibility is unknown, Flyway/application compatibility is uncertain, or consent evidence does
not match. Never repair or edit a dump in place.

## 3. Rehearse outside production

Restore the exact selected archive in the isolated non-production verifier before approval:

```powershell
.\scripts\test-production-restore.ps1 `
  -BackupVolume bwc_postgres_backups `
  -DumpName bwc_campaign-YYYYMMDDTHHMMSSZ.dump
```

The script mounts the backup read-only, uses an ephemeral PostgreSQL 16 container with no network,
runs `pg_restore --exit-on-error --no-owner --no-privileges`, and verifies successful Flyway history
plus the users, customers, campaigns, and audit-log tables. It does not prove production recovery;
retain sanitized output as the prerequisite evidence.

Also rehearse recovery of the matching consent-evidence snapshot and verify authorized application
access to representative evidence metadata/files without copying personal data into test evidence.

## 4. Restore production state

Obtain final human approval. Confirm maintenance mode and the target database name before running
destructive commands. Never rely on an implicit Compose project or default environment.

Recreate the target database using the configured production owner:

```bash
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T postgres sh -eu -c '
  dropdb --force --if-exists -U "$POSTGRES_USER" "$POSTGRES_DB"
  createdb -U "$POSTGRES_USER" -O "$POSTGRES_USER" "$POSTGRES_DB"
'
```

Restore the verified custom archive:

```bash
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" run --rm --no-deps \
  --entrypoint sh database-backup -eu -c \
  'pg_restore --exit-on-error --no-owner --no-privileges --dbname="$PGDATABASE" "$1"' \
  -- "$RESTORE_DUMP"
```

If `pg_restore` exits non-zero, keep maintenance mode active. Preserve logs, recreate a clean empty
database before any retry, and use only another verified approved archive. Do not treat a partial
restore as usable.

Restore the paired consent-evidence snapshot before reopening consent/customer workflows. Apply
secret values from the secret manager; secrets are not restored from Git, database dumps, or file
snapshots.

## 5. Validate before traffic

Deploy the backend image compatible with the restored Flyway version and start it first:

```bash
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --no-build backend
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T backend \
  wget -qO- http://localhost:8080/actuator/health/readiness
```

Confirm Flyway validation succeeds and `flyway_schema_history` contains no failed migration. Never
run `flyway clean`, edit/delete Flyway history, edit an applied migration, or improvise reverse DDL.

Start frontend and proxy only after backend readiness:

```bash
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --no-build frontend reverse-proxy
```

While maintenance remains active, run the complete
[production smoke test checklist](production-smoke-test-checklist.md). At minimum verify HTTPS,
health, login and role restrictions, customer lookup, consent and do-not-contact enforcement,
evidence access, segments/campaigns, human approval, contact history, analytics, immutable audit
records, safe errors, and unauthorized page/API denial.

Reconcile restored data against preserved audit/provider evidence. Obtain business-owner approval
before re-enabling providers, scheduler jobs, or public traffic. Start `database-backup` only after
the restored state is accepted, then verify a new post-restore recovery point.

## Abort criteria

Keep maintenance mode active and escalate when:

- archive/checksum validation or `pg_restore` fails;
- the selected database and consent-evidence recovery points do not match;
- Flyway or image/schema compatibility fails or is uncertain;
- readiness, HTTPS, authorization, consent, eligibility, audit, or a critical smoke check fails;
- reconciliation finds unexplained writes, communications, or evidence gaps;
- no verified alternative recovery point exists.

A deadline or demonstration is not approval to bypass these controls. Follow the
[incident response notes](incident-response-notes.md) and choose forward recovery when restore is
not demonstrably safe.

## Return to service and evidence

After all checks pass, record actual RPO/RTO, Flyway version, image digests, health and smoke results,
paired recovery identifiers, reconciliation result, command exit codes, operator/approver sign-off,
and monitoring start time. End maintenance mode only after explicit approval.

Monitor readiness, restart counts, error/authentication rates, scheduler/provider activity,
database performance, and backup health through the documented observation window. Record a new
verified off-host backup and create follow-up work for root cause and prevention.

Related procedures: [production backup guide](backup-guide.md),
[backup and restore reference](backup-and-restore.md), [production deployment guide](production-deployment-guide.md),
[migration strategy](../database/migration-strategy.md), and [operational monitoring](operational-monitoring-notes.md).

Automated documentation evidence: `ProductionRestoreGuideDocumentationTests`.
