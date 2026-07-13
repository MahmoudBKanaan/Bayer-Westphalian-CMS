# Production Operational Monitoring Notes

**Sprint 18 item 741** defines the minimum production monitoring model for the Bayer-Westphalian
Campaign Management Platform. It implements the operational intent of **NFR-004** (99% project-level
availability target) and **NFR-014** (logs, health endpoints, and error tracking).

## Current Capability And Required External Services

The repository provides aggregate liveness/readiness endpoints, Docker health checks and bounded
container logs, request IDs, safe error logging, scheduler run IDs/events, backup health, immutable
application audit records, and production smoke/incident runbooks. It does **not** bundle a hosted
uptime monitor, centralized log/error platform, metrics/time-series database, paging service, host
disk monitor, or certificate-expiry service.

Production operators must connect those external capabilities according to organizational policy.
Until then, the Docker and command-line checks below are a project-scale fallback, not unattended
24/7 monitoring and not proof that the 99% target is met.

## Ownership And Alert Lifecycle

| Role | Monitoring responsibility |
| --- | --- |
| Operations owner | Uptime, containers, host capacity, TLS, backups, alert routing, and runbook links |
| Application owner | Error rate, request correlation, release regressions, scheduler, and performance |
| Database/storage owner | PostgreSQL health/capacity, backup freshness, restore rehearsal, consent storage |
| Provider owner | Email/SMS enablement, delivery failures, quotas, credentials, and provider incidents |
| Security/compliance owner | Authentication abuse, authorization, consent/eligibility, audit gaps, and data exposure |
| Incident commander | Owns active alert severity, containment, updates, recovery, and closure |

Every actionable alert needs a stable name, environment, severity, UTC start, current value,
threshold/window, release image digest, owner, dashboard/query, runbook, and incident link. Alerts
must resolve only when the underlying signal remains healthy for the configured recovery window;
manual acknowledgement does not equal recovery.

## Initial Service-Level Signals

These are initial project-scale thresholds. Review them after representative traffic and record any
change through normal change control. Never weaken consent, eligibility, audit, security, backup, or
data-integrity alerts merely to reduce noise.

| Signal | Initial alert condition | Severity | Response |
| --- | --- | --- | --- |
| Public HTTPS availability | Two consecutive failures one minute apart | `SEV-1` when all users blocked; otherwise `SEV-2` | Check proxy/TLS, readiness, containers; enter incident process |
| Backend liveness `/livez` | Any sustained failure for 2 minutes or restart loop | `SEV-1`/`SEV-2` by impact | Preserve logs and image digest; contain/rollback |
| Backend readiness `/readyz` | Down for 5 minutes, or 3 transitions in 15 minutes | `SEV-2` | Correlate DB, disk, consent storage, and backend logs |
| HTTP 5xx ratio | Above 5% for 5 minutes with at least 20 requests | `SEV-2`; `SEV-1` if critical workflows broadly fail | Group by safe error code/request ID and release |
| Latency | Customer/product search p95 above 1 second for 10 minutes | `SEV-3`; raise if workflow unavailable | Compare database load/indexes and release baseline |
| PostgreSQL | Unhealthy/unreachable for 2 minutes, restart, or failed Flyway startup | `SEV-1`/`SEV-2` | Stop writers as needed; preserve evidence; do not alter Flyway history |
| Host/volume capacity | Above 80% warning; above 90% critical; unexpected rapid growth | `SEV-3` / `SEV-2` | Identify DB, logs, backup, or evidence growth safely |
| Consent-evidence storage | Readiness reports unwritable or mount unavailable | `SEV-1` for affected consent workflow, otherwise `SEV-2` | Stop evidence mutations; protect DB/file consistency |
| Backup freshness | No successful `.last-success` within `BACKUP_HEALTH_MAX_AGE_MINUTES` | `SEV-2` | Inspect worker, capacity, checksum; preserve last good copy |
| Backup integrity | Dump/checksum/archive validation fails | `SEV-1` if no other verified recovery point; otherwise `SEV-2` | Block release; run item 735/736 response |
| Scheduler completion | `run_failed`, nonzero `failedCount`, or no scheduled completion for two expected intervals | `SEV-2`/`SEV-3` by customer impact | Correlate `schedulerRunId`; stop retries if duplication risk |
| Provider delivery | Real sending enabled but provider unavailable, auth fails, quota exhausted, or failure ratio rises materially | `SEV-2`; `SEV-1` for unintended sends | Disable real sending and preserve provider event IDs |
| Authentication abuse | Account lock/rate-limit events rise materially above baseline or privileged login is anomalous | `SEV-2`; `SEV-1` if compromise suspected | Lock/revoke/rotate as approved; review audit history |
| Audit trail | Expected sensitive action lacks an audit event, repository errors, or normal user can edit/view improperly | `SEV-1` | Stop affected sensitive workflow and preserve DB/log evidence |
| TLS certificate | 30-day warning, 14-day urgent, 7-day critical | `SEV-3` / `SEV-2` / `SEV-1` | Renew and verify chain/hostname before expiry |

`SEV-1` and `SEV-2` alerts invoke the
[Production Incident Response Notes](incident-response-notes.md). A single zero-work scheduler run,
an empty business list, or low traffic is not automatically an incident.

## Health And Availability Monitoring

Monitor from outside the deployment host and from the internal network:

| Endpoint | View | Success contract |
| --- | --- | --- |
| `https://<host>/proxy-healthz` | External proxy process | HTTP 200 |
| `https://<host>/livez` | External backend process | HTTP 200 and aggregate `UP` |
| `https://<host>/readyz` | External traffic readiness | HTTP 200 and aggregate `UP` |
| `/actuator/health/readiness` | Internal backend/container | HTTP 200; never publicly expose details |

Use a separate external location so host/network failure is visible. Check at least once per minute
for the project deployment. Calculate monthly availability from successful external readiness
observations, excluding only approved maintenance recorded before it begins:

```text
availability % = successful readiness observations / eligible observations * 100
```

The 99% target permits about 7 hours 18 minutes of eligible downtime in a 30.4-day month, but this
budget does not permit ignoring security, consent, or data-integrity failures. Record outage start
from the first failed eligible observation and recovery only after the recovery window passes.

## Log And Error Monitoring

Centralize stdout/stderr in an access-controlled system with bounded retention, redaction, searchable
`requestId`, `schedulerRunId`, service, environment, release digest, level, logger, and UTC timestamp.
Alert on rates and grouped safe error signatures, not one alert per stack trace. Production API
responses remain stack-trace-free even when internal error telemetry is retained securely.

Never ingest passwords, JWTs, authorization/cookie headers, environment dumps, SQL parameters,
complete customer records, recipient lists, message bodies, consent evidence bytes, or secret values.
Restrict log access and audit log exports separately; operational logs do not replace `AuditLog`.

Project-scale fallback queries:

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml ps
docker compose --env-file .env.production -f docker-compose.prod.yml logs --since 15m backend `
  | Select-String " level=ERROR |schedulerEvent=run_failed|failedCount="
docker compose --env-file .env.production -f docker-compose.prod.yml logs --since 15m reverse-proxy
```

Do not paste rendered Compose configuration or unrestricted logs into tickets. Record bounded,
redacted lines and request/run IDs in the approved evidence system.

## Backup, Database, Storage, And Scheduler Checks

| Cadence | Operator check | Evidence |
| --- | --- | --- |
| Continuous / 5 minutes | Container/readiness state and restart count | Alert event and service/image identity |
| Every scheduler interval | One matching `run_started` and terminal `run_completed`/`run_failed` by `schedulerRunId` | Run ID, counts, duration, outcome |
| Daily | Backup health, latest completed dump age/size, checksum manifest, worker errors, volume capacity | Filename only, UTC time, size, checksum result |
| Before each release | Item 735 creates a new verified backup | Sanitized script result |
| Before release and after material DB changes | Item 736 restore rehearsal passes | Artifact name, Flyway count, core-schema result |
| Weekly | PostgreSQL/consent/backup volume growth and host free space | Capacity trend without customer data |
| Monthly | Availability, incident/alert trends, restore evidence age, access/retention review | Approved operations review |

Monitor off-host-copy success separately from local backup creation. A healthy backup container does
not prove encrypted off-host retention or restorability.

## Business And Compliance Canaries

Infrastructure `UP` does not prove campaign safeguards. After deployment and on the approved cadence,
run synthetic canaries through the item 737 smoke process:

- Authorized login and direct-route denial for an unauthorized role.
- Customer/segment/campaign read with known synthetic data.
- Opted-out/do-not-contact synthetic recipient remains excluded by `EligibilityService`.
- Human approval remains mandatory before campaign launch or AI copy use.
- Sensitive synthetic mutation creates an immutable audit event visible only to authorized roles.
- Provider policy is real-and-approved or explicitly disabled; never send a canary to a real customer.

Canary data uses a unique prefix, cannot target real recipients, and is cleaned up through audited
application workflows. Do not make production mutations solely to keep a dashboard green.

## Alert Routing, Deduplication, And Silence Rules

- Route `SEV-1` immediately to the incident commander and relevant security/compliance/provider/data
  owner; route `SEV-2` to the on-call operational owner with prompt escalation.
- Group duplicate alerts by environment, service, signal, and incident. Preserve the first-failure
  timestamp while updating impact.
- Silence only for an approved maintenance window with owner, reason, start/end UTC, affected alerts,
  and fallback observation. Never silence security, consent, audit, backup-integrity, or data-loss
  signals merely because a deployment is in progress.
- Detect dead-man failures: alert when expected monitor heartbeats, scheduler terminals, backup
  success markers, or log ingestion disappear.
- Test alert routes in non-production and periodically record delivery/acknowledgement evidence.

## Dashboard And Review Notes

The minimum operational dashboard should show release/image identity, external availability and
latency, liveness/readiness, container restarts, 4xx/5xx trend, safe error groups, database/storage
capacity, scheduler outcomes/duration, provider state/failures, backup age/integrity/off-host copy,
TLS expiry, open incidents, and current maintenance windows. Access follows least privilege.

At least monthly, review threshold usefulness, false positives, missed incidents, alert ownership,
stale silences, retention/access, availability budget, backup/restore evidence, provider quotas, and
follow-up completion. Threshold changes require rationale and before/after values.

## Alert Response Record

```text
Alert ID / name / environment / severity:
First observed UTC / acknowledged UTC / recovered UTC:
Current and threshold values / evaluation window:
Release tag / commit / image digest:
Owner / incident ID:
Affected workflow and confirmed impact:
Request ID / scheduler run ID / safe evidence references:
Containment / diagnosis / recovery actions and outcomes:
Smoke, backup, restore, and monitoring validation:
False-positive or threshold-change decision and approver:
```

Store runtime records in the approved monitoring/incident system, not this repository. This page is
the monitoring contract and operator guide only.

Related documentation: [Health Endpoints](health-endpoints.md),
[Production Logging](production-logging.md), [Scheduler Logging](scheduler-logging.md),
[Backup and Restore](backup-and-restore.md), [Production Smoke Checklist](production-smoke-test-checklist.md),
[Rollback Plan](rollback-plan.md), and [Incident Response Notes](incident-response-notes.md).
