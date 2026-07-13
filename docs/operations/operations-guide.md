# Production Operations Manual

**Sprint 18 item 765** defines day-to-day production operation of the Bayer-Westphalian Campaign
Management Platform. It supports KB **NFR-004 (availability)**, **NFR-013 (backup/recovery)**, and
**NFR-014 (observability)**. Detailed deployment, security, recovery, and incident runbooks remain
authoritative; this guide connects them into one operator workflow.

**Item 778** finalizes this document as the production Operations Manual. It is not a substitute for
the organization's infrastructure, privacy, security, or business-continuity policy.

## Operating principles

1. Protect consent, do-not-contact, eligibility, auditability, and customer data before availability
   or deadlines. Never bypass safeguards to clear an alert or complete a campaign.
2. Use least privilege, named operator accounts, UTC timestamps, immutable release/image identities,
   and approved change/incident records.
3. AI and automation may collect sanitized evidence or recommend actions, but humans approve user
   access, campaign actions, provider enablement, rollback, restore, and incident closure.
4. Production secrets, JWTs, credentials, full customer records, message bodies, consent evidence,
   database dumps, and unrestricted logs never belong in Git, tickets, screenshots, or chat.
5. Operational logs support diagnosis; immutable application `AuditLog` records sensitive business
   actions. One does not replace the other.

## Roles and handover

| Role | Operational responsibility |
| --- | --- |
| Operations owner | Availability, host/containers, TLS, capacity, logs, alerts, maintenance |
| Application owner | Releases, errors, performance, scheduler, workflow behavior |
| Database/storage owner | PostgreSQL, consent storage, backups, restore rehearsal, capacity |
| Security/compliance owner | Access, consent/eligibility, audit gaps, exposure assessment |
| Provider owner | Email/SMS policy, credentials, quotas, failures, disable/re-enable |
| Incident commander/recovery approver | Severity, containment, recovery choice, return to service |

For the solo project one person may fill several roles, but every decision is recorded under the
responsible role. Handover records environment, release tag/commit/image digests, active alerts and
incidents, maintenance windows, provider/scheduler state, latest backup and rehearsal, capacity/TLS
risks, temporary access, pending changes, and the next owner/check time.

## Start-of-shift checks

1. Review open incidents, alerts, approved maintenance, recent deployments, and unresolved smoke
   failures. Confirm monitoring and paging routes are themselves healthy.
2. Verify public HTTPS, `/livez`, `/readyz`, and internal readiness; inspect container health and
   restart counts without printing rendered secret-bearing configuration.
3. Confirm the deployed release and image digests match the approved deployment record.
4. Review bounded backend/proxy error logs by request ID and scheduler terminal events by
   `schedulerRunId`; check for secret/customer-data leakage.
5. Confirm PostgreSQL, consent-evidence storage, host disk, backup worker, newest archive freshness,
   checksum/off-host copy, TLS expiry, and provider state.
6. Record anomalies with owner and deadline. Any security, consent, audit, recoverability, or
   critical health failure invokes incident handling immediately.

Project-scale commands:

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml ps
docker compose --env-file .env.production -f docker-compose.prod.yml logs --since 15m backend
.\scripts\test-production-backup-exists.ps1 -MaximumAgeHours 26
```

Do not paste raw command output into shared evidence. Retain bounded, redacted metadata and request
or run identifiers in the approved operations system.

## Service lifecycle

Use the exact checked-out release directory and uncommitted production environment file. Confirm
the host and Compose project before every lifecycle command. Never run lifecycle commands from a
local development checkout while believing it is production.

### Inspect without changing state

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml config --quiet
docker compose --env-file .env.production -f docker-compose.prod.yml ps
docker compose --env-file .env.production -f docker-compose.prod.yml images
```

`config --quiet` validates configuration without printing expanded secret-bearing values.

### Planned start

Start the database/backend path first and require readiness/Flyway success before the public edge:

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --no-build postgres backend
docker compose --env-file .env.production -f docker-compose.prod.yml ps
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --no-build frontend reverse-proxy
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --no-build database-backup
```

Verify internal readiness, public HTTPS, expected image digests, stable restart counts, Flyway, logs,
scheduler/provider policy, and backup health. Run the required post-start smoke checks before ending
maintenance mode.

### Planned restart

A restart requires an approved change reason; do not use restart as diagnosis. Preserve bounded logs
and current state first. Restart only the affected stateless service when safe. For backend restart,
consider in-flight requests, scheduler work, provider sends, database connections, and token/session
behavior. After restart, verify readiness and the affected critical workflow.

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml restart backend
docker compose --env-file .env.production -f docker-compose.prod.yml ps backend
```

Repeated restart, restart loops, or unexplained recovery invokes incident response rather than
another restart.

### Planned shutdown

Enter maintenance mode and disable/stop new provider and scheduler work as required. Stop the public
and writer layers before PostgreSQL, preserving volumes:

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml stop reverse-proxy frontend backend database-backup
docker compose --env-file .env.production -f docker-compose.prod.yml stop postgres
```

Confirm containers are stopped and record the final backup/recovery state. Never use
`docker compose down -v`; never remove production database, consent-evidence, or backup volumes as a
shutdown shortcut.

### Emergency containment

For unauthorized access, unintended sending, consent/eligibility bypass, missing audit, corruption,
or active exposure, follow incident command. Contain the smallest unsafe surface, preserve evidence,
disable providers/scheduler where applicable, and do not execute broad shutdown or recovery commands
without the incident commander/recovery approver.

## Routine schedule

| Cadence | Required check |
| --- | --- |
| Continuous / each minute | External HTTPS/readiness, critical alerts, restart loops |
| Every scheduler interval | Matching start and terminal event, duration/counts, no repeated-contact risk |
| Daily | Errors, auth abuse, providers, DB/storage capacity, backup freshness/integrity/off-host copy |
| Weekly | Volume growth, TLS horizon, accounts/temporary access, provider quotas, alert-route test |
| Before release | CI gate, production config validation, fresh backup, restore evidence, rollback readiness |
| After deploy/restore | Full smoke checklist, audit/log review, observation window, new backup |
| Monthly | Availability against 99% target, incidents, access/retention, alert quality, RPO/RTO evidence |

Signal thresholds and alert routing are defined in
[operational monitoring](../deployment/operational-monitoring-notes.md). A healthy container does
not prove business safeguards, off-host backup, or successful communication-provider behavior.

## Controlled changes and maintenance

All production changes require a record with scope, risk, release/config identity, operator,
approver, start/end UTC, test evidence, rollback trigger, and communication plan. Follow the
[production deployment guide](../deployment/production-deployment-guide.md). Do not build from an
uncommitted production working tree or deploy mutable `latest` tags.

During maintenance:

- announce and time-bound the window; silence only approved availability alerts with fallback
  observation, never security/consent/audit/backup-integrity alerts;
- stop public traffic, providers, scheduler, or writers only as required and preserve evidence;
- keep secrets in the secret manager and avoid environment dumps;
- run the complete [smoke checklist](../deployment/production-smoke-test-checklist.md) before
  reopening traffic;
- invoke the [rollback plan](../deployment/rollback-plan.md) when a rollback trigger occurs.

Emergency changes still require an incident/change identifier, human authorization, evidence, and
post-change review. Urgency never permits direct database edits or safeguard bypass.

## Access and administrator operations

Use the application user-management workflow for account creation, activation/deactivation, and
role changes. Admin operations must create immutable audit records. Review privileged accounts and
temporary access regularly; remove access when its approved period ends. Normal users cannot view
or edit audit logs.

The first production administrator uses the controlled
[admin bootstrap](admin-bootstrap.md). Enable it once, inject a strong password from the secret
manager, verify the audit record and login, then disable bootstrap and remove its password. Never
use demo `.test` accounts in production.

Suspected account compromise requires session/account containment, evidence preservation, approved
credential rotation, and security/compliance review. Do not delete audit history to remove evidence.

## Scheduler and provider operations

Monitor reminder processing for one start and one terminal event per `schedulerRunId`, expected
counts/duration, failures, and duplicate processing. Use the documented manual trigger only in an
approved admin/test context; production manual execution requires a change/incident record and must
respect idempotency, retry limits, consent, do-not-contact, and eligibility.

Email/SMS real sending remains disabled unless a human-approved provider configuration, test
destination policy, credentials, quota monitoring, and incident disable procedure exist. Disable
providers immediately for unintended sending, authorization failures, or unsafe recipient behavior.
Never automatically replay sends after recovery. See [scheduler logging](../deployment/scheduler-logging.md),
[email](../deployment/email-provider.md), and [SMS](../deployment/sms-provider.md).

## Data, backup, and recovery operations

PostgreSQL and consent-evidence files form a paired recovery scope. Each day confirm a fresh,
non-empty, checksum-valid, `pg_restore`-readable database archive, encrypted off-host copy, matching
consent-evidence recovery point, retention state, and available capacity. Follow the
[backup guide](../deployment/backup-guide.md).

Restores are destructive, human-approved incident/change operations. Rehearse the exact archive in
isolated non-production, accept expected RPO, enter maintenance mode, restore paired state, validate
Flyway and all critical workflows, reconcile post-recovery activity, and create a new recovery
point. Follow the [restore guide](../deployment/restore-guide.md). Never run `flyway clean`, edit
applied migrations/history, or use `docker compose down -v` in production.

## Alert and incident response

Classify alerts by confirmed/potential impact. `SEV-1` and `SEV-2` invoke the
[incident response process](../deployment/incident-response-notes.md). First actions are to assign
an incident commander, contain the smallest unsafe surface, preserve bounded evidence, establish a
UTC timeline, and schedule the next update.

Immediately escalate unauthorized access, secret exposure, consent/eligibility bypass, unintended
sending, missing audit events, destructive loss, failed recovery, broad outage, or critical smoke
failure. AI cannot set final severity, approve communications/recovery, or close an incident.

Return to service requires human approval, health/Flyway success, all critical smoke checks, safe
provider/scheduler state, current backup evidence, reconciliation, and a stable observation window.
Create root-cause and prevention follow-ups with owners and due dates.

## End-of-shift and evidence

Before handover, confirm no unowned critical alert, verify current service/provider/scheduler/backup
state, update incident/change timelines, and identify the next required check. Store only sanitized:

- release/image identity, health and availability summaries, alert/incident IDs;
- request/scheduler run IDs, bounded log references, and command exit status;
- backup filename/size/checksum/freshness and restore-rehearsal result;
- change approvals, smoke result, actual RPO/RTO, owners, and follow-up dates.

Completed runtime records belong in access-controlled operations systems, not this repository.

## Runbook selection

| Situation | Required procedure |
| --- | --- |
| New release or approved update | Production Deployment Guide plus item 770 release gate |
| Routine health, capacity, logs, TLS, scheduler, provider, or backup alert | Operational Monitoring Notes and this manual |
| Account/role/settings administration | Administrator Manual; use audited UI/API workflows |
| Fresh backup creation or retention/off-host verification | Production Backup Guide |
| Data corruption/loss requiring destructive recovery | Production Restore Guide with human approval |
| Bad release with compatible data | Rollback Plan, path A |
| Bad/corrupt state or incompatible schema | Rollback Plan path B or C; Restore Guide if approved |
| Security/privacy/availability incident | Incident Response Notes |
| Post-deploy/restore/rollback acceptance | Complete Production Smoke Test Checklist |

When more than one row applies, incident containment and human recovery approval take priority. Do
not combine fragments of separate runbooks into an unreviewed procedure.

## Runbook index

- [Production logging](../deployment/production-logging.md)
- [Health endpoints](../deployment/health-endpoints.md)
- [Production security checklist](../deployment/production-security-checklist.md)
- [Production deployment guide](../deployment/production-deployment-guide.md)
- [Smoke test checklist](../deployment/production-smoke-test-checklist.md)
- [Rollback plan](../deployment/rollback-plan.md)
- [Backup guide](../deployment/backup-guide.md)
- [Restore guide](../deployment/restore-guide.md)
- [Incident response](../deployment/incident-response-notes.md)
- [Administrator manual](../admin/admin-manual.md)
- [Production release gate](../deployment/production-release-gate.md)

Automated documentation evidence: `ProductionOperationsGuideDocumentationTests`.
Item 778 evidence: `FinalOperationsManualDocumentationTests`.
