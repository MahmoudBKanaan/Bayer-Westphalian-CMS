# Production Deployment Guide

**Sprint 18 item 760** provides the operator runbook for deploying the Bayer-Westphalian Campaign
Management Platform. The deployment is a Docker Compose modular monolith: Nginx terminates HTTPS,
serves the React frontend, and proxies `/api`; Spring Boot runs with the `prod` profile; PostgreSQL
and consent evidence use persistent volumes.

**Items 775 and 854** finalize this document as the project deployment guide. Local development setup is
documented separately; commands here are for an approved production or production-like host.

This guide does not prove that production is deployed. Record every deployment and its evidence;
the release remains blocked until CI, HTTPS, smoke tests, backup verification, and approval pass.

## Supported deployment model

The supported project deployment is one Docker Compose application stack on a controlled Linux
Docker host with externally managed DNS, TLS certificate material, secret storage, monitoring, and
encrypted off-host backup storage. `docker-compose.prod.yml` defines PostgreSQL, database backup,
Spring Boot backend, React/Nginx frontend, and the Nginx TLS edge.

This guide does not define Kubernetes, multi-region failover, zero-downtime database migration,
public Swagger access, or automatic provider activation. Adaptations require a reviewed deployment
design, security assessment, test evidence, and rollback plan.

### First deployment versus update

- **First deployment:** provision volumes/TLS/secrets, validate DNS and host controls, run the
  one-time Admin bootstrap, disable bootstrap immediately, and create the first verified off-host
  recovery point.
- **Update:** preserve existing volumes, create a pre-update backup, compare Flyway migrations and
  rollback compatibility, deploy immutable approved images, and never re-enable bootstrap.
- **Restore/rollback:** do not improvise from this startup sequence; use the dedicated restore or
  rollback guide under human approval.

## 1. Preconditions and ownership

Assign a deployment operator and an independent approver, even when one person performs both roles
for the solo university project. Record the release tag, image digests, target host, change window,
operator, approver, database migration version, backup identifier, and rollback decision owner.

Before touching production:

1. Confirm the release commit is on `main` and its required CI workflow passed. A failed or unknown
   CI state is a no-go; follow the [release tagging guide](release-tagging.md).
2. Review the [production security checklist](production-security-checklist.md), open incidents,
   migration compatibility, provider state, expected downtime, and [rollback plan](rollback-plan.md).
3. Confirm the host has supported Docker Engine with Compose v2, adequate disk space, DNS pointing
   to the edge host, and inbound TCP 80/443 only as required. Do not publish PostgreSQL or backend
   container ports.
4. Confirm a recent PostgreSQL backup exists, its off-host copy is accessible, and a restore has
   been rehearsed in non-production according to [backup and restore](backup-and-restore.md).
5. Obtain a valid TLS certificate and private key for the production hostname. Follow the
   [HTTPS guide](https.md); never commit private keys.
6. Validate the item 770 [production release gate](production-release-gate.md) for the exact commit,
   tag, environment, and human-approved evidence manifest. Green CI alone is insufficient.

## 2. Prepare runtime configuration

Create an uncommitted `.env.production` on the deployment host from the documented variable names
in [environment variables](environment-variables.md). Store secret values in the deployment secret
manager and inject them at runtime as described in [secrets](secrets.md).

At minimum, set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
`CORS_ALLOWED_ORIGINS`, `TLS_CERTIFICATE_PATH`, and `TLS_PRIVATE_KEY_PATH`. Production requirements:

- `SPRING_PROFILES_ACTIVE=prod`, `HTTPS_REQUIRED=true`, and explicit HTTPS CORS origins.
- `VITE_API_BASE_URL=/api`; Vite values are compiled into the frontend image and are public.
- OpenAPI/Swagger stays disabled unless access is explicitly approved.
- Real email/SMS sending stays disabled until provider credentials, consent controls, and an
  operational approval are complete. See [email](email-provider.md) and [SMS](sms-provider.md).
- Configure persistent consent evidence storage and backup using
  [consent evidence storage](consent-evidence-storage.md).
- Enable admin bootstrap only for the first controlled startup, inject its password from the secret
  manager, verify login, then disable and remove that bootstrap secret. See
  [admin bootstrap](../operations/admin-bootstrap.md).

Restrict `.env.production`, certificate, and key permissions to the deployment account. Commands,
logs, tickets, and screenshots must not expose passwords, tokens, API keys, private keys, or the
expanded Compose configuration.

## 3. Validate before rollout

### Select immutable artifacts

Use backend/frontend images built from the exact green release commit. Prefer registry digests in
`BACKEND_IMAGE` and `FRONTEND_IMAGE`; record repository, digest, provenance/CI run, and scan result.
Do not use `latest`, rebuild an uncommitted tree on the host, or silently substitute a different
image after approval. When this project-scale deployment must build locally, use a clean annotated
release checkout, record the resulting image IDs, and treat those IDs as the deployment identity.

Run from the checked-out, tagged release directory:

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml config --quiet
docker compose --env-file .env.production -f docker-compose.prod.yml build --pull
docker compose --env-file .env.production -f docker-compose.prod.yml images
```

Confirm the Compose project name/host/environment before any command. `config --quiet` validates
without printing expanded secret values. Keep the environment file and TLS private key outside
captured terminal output and deployment artifacts.

Prefer approved immutable image digests for repeatable deployment. Confirm the rendered service
model without publishing its secret-bearing output, scan images according to local policy, and
verify the image identifiers match the deployment record. Any missing variable, unsafe CORS origin,
failed build, critical vulnerability, or unavailable rollback artifact is a no-go.

## 4. Back up and deploy

Create and verify a pre-deployment backup before applying a release that may run Flyway migrations.
Do not use `docker compose down -v`; production volumes must survive application replacement.

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --no-build
docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

Compose waits for PostgreSQL, backend, and frontend health before the edge becomes healthy. Watch
startup through the secret-safe logging process in [production logging](production-logging.md).
Confirm Flyway completed successfully and did not report validation or checksum errors. Do not
manually alter Flyway history or improvise a schema downgrade.

## 5. Verify and release traffic

For the exact deployed image identifiers:

1. Confirm every required container is running and healthy, restart counts are stable, persistent
   volumes are mounted, and database-backup health is current.
2. Verify the public application and health endpoints over HTTPS using
   [health endpoints](health-endpoints.md). Confirm HTTP redirects to HTTPS, the certificate chain
   and hostname are valid, HSTS/security headers are present, and backend/database ports are not
   publicly reachable.
3. Run and record the complete [production smoke test checklist](production-smoke-test-checklist.md),
   including authentication, role restrictions, consent/eligibility, campaign approval and launch,
   contact history, analytics, audit logs, safe errors, and unauthorized page/API access.
4. Verify logs and scheduler logs are accessible only to authorized operators, contain correlation
   context, and do not contain secrets or customer message bodies. Review
   [monitoring notes](operational-monitoring-notes.md).
5. Capture approved, redacted evidence and obtain the go-live approver's sign-off. Never use demo
   data or a local Vite server as production evidence.

## 6. Failure and rollback

Stop rollout and invoke the [rollback plan](rollback-plan.md) for failed health checks, migration
errors, TLS/CORS failures, authentication or authorization regressions, data-integrity issues,
missing audit records, secret exposure, or any critical smoke-test failure. Preserve logs and
timestamps, notify the decision owner, and follow [incident response](incident-response-notes.md)
when security, privacy, or availability may be affected.

Application rollback uses the recorded known-good image digests. Database restore is a separately
approved destructive operation and is used only when forward recovery is not safe. After rollback,
repeat health and smoke checks and record the final state.

## 7. Deployment record and handover

Store the following outside Git in the approved operations/evidence location:

- release tag, commit, image digests, deployment start/end time, operator, and approver;
- configuration version and secret references by name only, never values;
- pre-deployment backup identifier and restore-rehearsal reference;
- Flyway version, health output, smoke checklist, redacted screenshots, and monitoring links;
- incidents, deviations, rollback actions, and final go/no-go decision.

Handover includes active alerts, provider state, scheduler status, backup status, support contact,
and the next review/secret-rotation dates. The release is complete only after evidence is retained
and operational ownership is accepted.

## Operator completion checklist

- [ ] Exact `main` commit, annotated tag, CI run, backend digest, and frontend digest recorded.
- [ ] Item 770 production release gate passed with operator and human approver.
- [ ] Host, DNS, ports, Docker/Compose versions, capacity, and time synchronization checked.
- [ ] Production environment validated; secrets/TLS restricted and absent from logs/evidence.
- [ ] Provider policy is approved real configuration or explicitly disabled.
- [ ] Fresh database backup, off-host copy, matching consent-evidence recovery point, and restore
      rehearsal verified.
- [ ] Flyway compatibility and rollback target reviewed before startup.
- [ ] PostgreSQL, backend, frontend, backup worker, and proxy are healthy without restart loops.
- [ ] HTTPS, certificate, redirect, HSTS, CORS, safe errors, and private service ports verified.
- [ ] Full production smoke checklist and critical role workflows passed using synthetic data.
- [ ] Logs, scheduler, monitoring, alerts, backup freshness, and provider state verified.
- [ ] Synthetic cleanup, screenshots/evidence, go-live approval, and operations handover completed.

Any unchecked critical item leaves the deployment `BLOCKED`; do not relabel partial execution as a
successful production release.

Related final guides: [local setup](../development/developer-setup.md),
[operations](../operations/operations-guide.md), [backup](backup-guide.md),
[restore](restore-guide.md), [smoke](production-smoke-test-checklist.md),
[rollback](rollback-plan.md), and [incident response](incident-response-notes.md).

Automated documentation evidence: `ProductionDeploymentGuideDocumentationTests`.
Item 775 evidence: `FinalDeploymentGuideDocumentationTests`.
