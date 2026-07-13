# Production Backup Guide

**Sprint 18 item 761** defines the production backup runbook for the Bayer-Westphalian Campaign
Management Platform and supports KB **NFR-013 (backup/recovery)**. PostgreSQL is the system of
record. Consent evidence files are a second persistent data set and must be protected at a recovery
point consistent with the database.

This guide covers backup creation, storage, monitoring, verification, and evidence. Destructive
recovery belongs to the [backup and restore process](backup-and-restore.md) and requires an approved
maintenance window.

## Service objectives and ownership

The deployment owner must record approved RPO and RTO values before go-live. The project defaults
are:

| Control | Project baseline |
| --- | --- |
| PostgreSQL backup interval / RPO target | Every 24 hours; reduce for the deployment's data-loss tolerance |
| Local retention | 7 daily recovery points by default |
| Off-host retention | At least the local retention period; follow institutional policy if longer |
| Freshness alert | No successful backup within 26 hours |
| Restore rehearsal | Before release and after material PostgreSQL or Flyway changes |
| RTO target | Within the approved operator shift for this project-scale deployment |

Assign an operator to monitor and copy backups and an approver to review failures and restore
rehearsals. For the solo project one person may hold both responsibilities, but both decisions and
timestamps must still be recorded.

## Recovery scope

Back up all of the following:

1. PostgreSQL logical archive containing users, roles, customers, consent metadata, products,
   campaigns, communications, analytics source records, audit logs, and Flyway history.
2. The `bwc_consent_evidence` volume at a matching recovery point. Database rows that reference
   evidence files are not a complete recovery without those files.
3. Deployment configuration definitions and release identifiers from Git. Secret **values** are
   recovered from the secret manager, never from a dump or repository.

A Docker data volume, running replica, source tree, `.env` file, screenshot, or successful backup
log alone is not a verified backup. Never commit dumps or evidence files to Git.

## Automated PostgreSQL backups

The `database-backup` service in `docker-compose.prod.yml` runs
`docker/postgres/backup.sh` immediately after PostgreSQL becomes healthy and then every
`BACKUP_INTERVAL_SECONDS` (default `86400`). It writes PostgreSQL custom-format archives to the
`bwc_postgres_backups` volume.

Publication is fail-safe:

1. `pg_dump --format=custom` writes a `.partial` archive.
2. `pg_restore --list` verifies readability.
3. The archive is atomically renamed to `<database>-<UTC timestamp>.dump`.
4. A SHA-256 manifest is created and `.last-success` is refreshed.
5. Completed archives older than `BACKUP_RETENTION_DAYS` are removed.

The service health check detects stale `.last-success` state. It does not prove checksum validity,
restore success, off-host replication, or consent-evidence coverage.

## Initial setup

1. Set `BACKUP_INTERVAL_SECONDS`, `BACKUP_RETENTION_DAYS`, and
   `BACKUP_HEALTH_MAX_AGE_MINUTES` in the deployment environment. Retention must fit available
   storage with safety margin.
2. Keep `bwc_postgres_backups` separate from `bwc_postgres_prod_data`. Do not publish a backup
   service port or mount the backup volume into the public frontend/backend.
3. Configure encrypted off-host storage in a separate failure domain. Use a dedicated least-
   privilege service identity that can create recovery objects but cannot administer production.
4. Configure encrypted snapshots or file-level backup for `bwc_consent_evidence`; record its
   recovery-point identifier beside the matching database archive.
5. Enable alerts for unhealthy `database-backup`, stale backup age, failed off-host copy, storage
   exhaustion, checksum failure, and failed restore rehearsal.

Backups contain personal and audit data. Encrypt them in transit and at rest, restrict access,
record access events, and apply secure deletion at retention expiry. Do not place database
credentials in archive names, command arguments, logs, tickets, or evidence.

## Create and verify a recovery point

Run from the tagged release directory against the intended production-like stack:

```powershell
.\scripts\test-production-backup.ps1 -EnvFile .env.production
```

The non-destructive verifier triggers a new archive, requires non-zero content and a manifest,
checks SHA-256, and confirms `pg_restore` can read the custom archive. Retain its sanitized output;
do not attach the dump itself.

Verify the scheduled recovery point remains fresh without modifying it:

```powershell
.\scripts\test-production-backup-exists.ps1 `
  -BackupVolume bwc_postgres_backups `
  -MaximumAgeHours 26
```

After local validation, copy the completed `.dump`, its `.sha256` manifest, and the corresponding
consent-evidence backup to encrypted off-host storage. Copy only completed files, never `.partial`
archives. Verify the destination checksum and record destination object identifiers without secret
URLs or credentials.

## Daily operations

The operator reviews:

- backup-service health, latest successful UTC timestamp, archive size trend, and available space;
- checksum and `pg_restore --list` verification for the newest archive;
- off-host copy completion and destination integrity;
- matching consent-evidence recovery point and retention state;
- alerts, failed attempts, and any unexpected archive-size reduction.

Investigate immediately when the service is unhealthy, freshness exceeds the RPO threshold, an
archive or manifest is empty/missing, checksum verification fails, off-host copy is late, consent
evidence has no matching recovery point, or storage approaches capacity. Preserve sanitized logs,
create a new verified backup after correction, and escalate under the
[incident response notes](incident-response-notes.md) when recovery objectives or data security may
be affected.

## Restore rehearsal and release gate

At least one selected off-host recovery point must be restored in an isolated non-production
environment before release. Use:

```powershell
.\scripts\test-production-restore.ps1 -BackupVolume bwc_postgres_backups
```

The rehearsal validates the checksum, restores with PostgreSQL 16, verifies Flyway history and core
tables, and removes its temporary container. Also verify the paired consent-evidence backup can be
recovered and sampled through authorized application behavior.

A release is blocked when no fresh verified archive exists, the off-host copy is unconfirmed, the
consent-evidence recovery point is missing, or the latest required restore rehearsal failed. Never
lower the gate because the live database is currently healthy.

## Evidence record

Record only sanitized metadata:

- UTC creation time, archive filename, byte size, checksum result, and PostgreSQL major version;
- release tag/commit, database/Flyway version, operator, and verification timestamp;
- off-host object identifier and encryption/retention policy name, not credentials or signed URLs;
- paired consent-evidence recovery-point identifier;
- restore-rehearsal result, measured RPO/RTO, approver, incidents, and corrective actions.

Keep evidence in the approved operations location. Backup contents, customer data, secrets, and
private storage URLs must not appear in Git, CI artifacts, screenshots, or tickets.

## Related procedures

- [Production deployment guide](production-deployment-guide.md)
- [Backup and restore process](backup-and-restore.md)
- [PostgreSQL production volume](postgres-production-volume.md)
- [Consent evidence storage](consent-evidence-storage.md)
- [Secrets documentation](secrets.md)
- [Operational monitoring notes](operational-monitoring-notes.md)

Automated documentation evidence: `ProductionBackupGuideDocumentationTests`.
