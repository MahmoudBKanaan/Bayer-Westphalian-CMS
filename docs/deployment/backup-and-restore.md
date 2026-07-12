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

## Testability (How This Process Is Proven Without Running Live Dumps in CI)

Item **666** requires the process to be **documented and testable**. Evidence layers:

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
