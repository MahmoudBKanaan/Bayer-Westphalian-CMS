# Production Rollback Plan

**Sprint 18 items 739 and 764** define how to return the Bayer-Westphalian Campaign Management
Platform to a known-good release after a failed deployment, failed smoke gate, security incident,
data-integrity problem, or unacceptable operational regression. Rollback is a human-approved
production action; automation and AI may provide evidence but may not approve it.

## Objectives And Ownership

| Objective | Rule |
| --- | --- |
| Contain harm | Stop new writes, sends, scheduler work, or public traffic before changing state |
| Preserve evidence | Record UTC times, release digests, request IDs, logs, symptoms, decisions, and command exit codes |
| Recover safely | Prefer application-image rollback when the current schema remains backward-compatible |
| Protect data | Use a verified database plus matching consent-evidence recovery point when state restoration is required |
| Fail closed | Keep maintenance mode active until health, Flyway, security, and critical smoke checks pass |

Required roles: a **rollback operator** executes commands, a **release/incident approver** selects the
target and authorizes destructive recovery, and the **System Auditor** retains sanitized evidence.
For the university deployment one person may hold multiple roles, but each decision must still be
recorded separately.

## Rollback Record

Record these values before execution:

| Field | Value |
| --- | --- |
| Change / incident identifier | |
| Environment and base URL | |
| Failed release tag, commit, backend digest, frontend digest | |
| Last known-good tag, commit, backend digest, frontend digest | |
| Current and target Flyway versions | |
| Last verified database backup and checksum | |
| Matching consent-evidence recovery point | |
| Failure start / containment / decision times (UTC) | |
| Operator / approver / auditor | |
| Selected path | `A`, `B`, or `C` |
| Expected and actual RPO/RTO | |
| Final decision | `RESTORED`, `FORWARD-FIX`, or `ESCALATED` |

Never record secret values, JWTs, database URLs with credentials, customer payloads, or consent-file
contents. Preserve logs before container replacement and use request IDs for correlation.

## Pre-Release Rollback Readiness

A production release is not deployable until the operator and approver confirm:

1. The last known-good backend/frontend image digests are immutable, pullable, and recorded.
2. Current and target Flyway migration sets have been compared and the compatible recovery path is
   understood; image rollback is not assumed to reverse a database migration.
3. A fresh database backup and matching consent-evidence recovery point passed integrity checks,
   and the selected restore candidate passed a non-production rehearsal.
4. Maintenance-mode and provider-disable controls are available, and the rollback operator can
   access Docker, logs, monitoring, backups, and the secret manager without sharing credentials.
5. The smoke checklist, incident contacts, recovery approver, expected RPO/RTO, and observation
   window are assigned for the release.

Record this readiness review in the deployment evidence. A missing image, unverified recovery
point, unknown schema compatibility, or unavailable decision owner blocks deployment.

## Trigger And Immediate Containment

Rollback evaluation is mandatory when any Critical item 737 smoke check fails, readiness remains
down, containers restart repeatedly, unauthorized access is possible, secrets or stack traces are
exposed, consent/eligibility can be bypassed, sends reach unintended recipients, audit events are
missing, Flyway fails, or data is corrupted.

1. Declare maintenance mode and identify the incident/change record.
2. Disable external sends at the provider when unintended communication is possible.
3. Stop public traffic, application writers, scheduler processing, and backup rotation while
   preserving PostgreSQL for diagnosis:

```bash
export COMPOSE_FILE=docker-compose.prod.yml
export ENV_FILE=.env.production

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" \
  stop reverse-proxy frontend backend database-backup
```

4. Capture sanitized `docker compose ps`, image digests, recent backend/proxy/scheduler logs, current
   Flyway version, backup filename/checksum result, and failure timeline.
5. Take a final pre-rollback logical backup when database integrity and the incident allow it. Mark
   it as incident evidence and prevent retention cleanup. Do not overwrite the known-good backup.

## Choose The Recovery Path

| Condition | Path |
| --- | --- |
| Application or frontend regression; no destructive/incompatible migration; data remains valid | **A: image/config rollback** |
| Database corruption, bad data mutation, or schema incompatible with last known-good application | **B: paired state restore** |
| No verified compatible image/backup, active compromise, unclear blast radius, or restore rehearsal failed | **C: hold and escalate / forward-fix** |

Do not guess schema compatibility. Compare the failed and target release migration sets and
`flyway_schema_history`. If the target application cannot validate the current schema, Path A is
forbidden.

## Path A: Application Image And Configuration Rollback

Use immutable registry digests recorded by the last successful release. Tags such as `latest` or
mutable local image names are not acceptable rollback targets.

1. Restore the last known-good **non-secret configuration version**. Retrieve secrets from the
   secret manager; never restore secrets from Git or a database dump.
2. Set image references in the deployment environment to approved digests:

```text
BACKEND_IMAGE=registry.example/bwc-backend@sha256:<approved-digest>
FRONTEND_IMAGE=registry.example/bwc-frontend@sha256:<approved-digest>
```

3. Validate resolved Compose configuration without printing its rendered secret-bearing output into
   shared evidence. Pull and start PostgreSQL/backend only; do not build from the working tree:

```bash
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" config --quiet
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull backend frontend
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --no-build postgres backend
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps
```

4. Confirm backend readiness and Flyway validation. If either fails, stop; return to maintenance mode
   and select Path B or C.
5. Start frontend and reverse proxy, then execute the complete item 737 smoke checklist, finalized
   for delivery under item 763:

```bash
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --no-build frontend reverse-proxy
```

6. Restart `database-backup` only after acceptance. Re-enable providers/scheduler only when their
   relevant checks pass and a human approver authorizes it.

## Path B: Database And Consent-Evidence Restore

State rollback is destructive and loses writes newer than the selected recovery point. The approver
must accept the stated RPO and confirm that application-level reconciliation is insufficient.

1. Select a backup that passed item 735 creation verification and item 736 non-production restore
   rehearsal. Verify its SHA-256 manifest again.
2. Select the consent-evidence snapshot from the same recovery point. Database references and files
   must move together; do not restore one without the other when evidence records are affected.
3. Follow [Production Database Restore](backup-and-restore.md#production-database-restore-item-734)
   exactly, including `--exit-on-error`, Flyway checks, health checks, smoke tests, and evidence.
4. Deploy the application image compatible with the restored Flyway version. Do not allow a newer
   backend to auto-migrate the restored database until the rollback target is confirmed.
5. Reconcile post-recovery writes and communications from audit/provider evidence under an approved
   business process. Never replay campaign sends automatically.

### Flyway prohibition

Never run `flyway clean`, delete or edit `flyway_schema_history`, reverse DDL manually, edit an
applied migration, or invent an ad hoc down migration in production. Flyway migrations are
forward-only. Use a verified full logical restore for state rollback, or ship a reviewed new forward
migration as Path C.

## Path C: Hold, Escalate, Or Forward-Fix

Keep maintenance mode active when there is no safe rollback target, compromise may still be active,
schema compatibility is unknown, backups/checksums fail, consent evidence is mismatched, or a restore
rehearsal has not passed. Rotate affected secrets, preserve evidence, engage the system owner and
data/compliance owner, and create a reviewed forward fix. A deadline or demonstration does not
justify bypassing consent, eligibility, audit, migration, or security controls.

## Validation And Return To Service

A rollback is not complete merely because containers are running. Before reopening traffic:

1. Verify exact backend/frontend image digests and the expected production configuration version.
2. Verify PostgreSQL readiness, Flyway history, backend liveness/readiness, frontend health, reverse
   proxy HTTPS, CORS, HSTS, and absence of restart loops.
3. Run all Critical item 737 checks (the final item 763 checklist), including authentication/RBAC,
   customer/segment/campaign reads,
   consent and do-not-contact exclusion, human approval, audit visibility, provider policy, backup
   health, and synthetic-data cleanup.
4. Compare error and scheduler logs before/after rollback and account for every high-severity event.
5. Obtain operator and approver sign-off before ending maintenance mode.
6. Monitor readiness, error rate, authentication failures, scheduler outcomes, provider activity,
   and database-backup health for the documented observation window.

Any failed Critical check returns the process to containment. Do not chain repeated rollback attempts
without reassessing the target and preserving new evidence.

## Abort Criteria

Stop and escalate when an image digest is unavailable or unverified, Compose resolves an unexpected
image, schema compatibility is uncertain, checksum/archive verification fails, `pg_restore` or
Flyway fails, consent evidence does not match, health remains down, unauthorized access persists,
audit logging is absent, or a critical smoke check fails.

## Completion And Follow-Up

Store the sanitized rollback record, approvals, image digests, backup checksum result, Flyway
version, command exit codes, logs/request IDs, smoke execution, actual RPO/RTO, and monitoring result
in the approved incident/release evidence location. Create follow-up work for root cause, missing
automation, data reconciliation, provider reconciliation, and prevention. Do not delete the failed
release tag or move immutable tags; mark the release withdrawn and publish a corrected version.

Related documentation: [Production Smoke Test Checklist](production-smoke-test-checklist.md),
[Backup and Restore Process](backup-and-restore.md), [Release Tagging](release-tagging.md),
[Production Security Checklist](production-security-checklist.md), and
[Secrets Documentation](secrets.md). Incident command, severity, communication, and evidence handling
are defined in [Production Incident Response Notes](incident-response-notes.md) (item **740**).

Automated item 764 evidence: `FinalRollbackPlanDocumentationTests`.
