# Production Deployment Screenshot Evidence

**Sprint 18 item 743** requires screenshots of the deployed Bayer-Westphalian Campaign Management
Platform. Screenshots are release evidence, not decoration: each image must identify the intended
release and environment without exposing secrets or customer data.

## Current Capture Status

| Field | Value |
| --- | --- |
| Attempt time | `2026-07-12T20:59:29Z` |
| Git branch / base commit | `dev` / `7cb7b01543fa533c38d2935cfe1236c8f20cecf2` |
| Status | **BLOCKED - no production deployment available** |
| Production Compose project | Not running |
| Production HTTPS | `https://localhost/` unreachable (`curl` exit 7) |
| Production readiness | `https://localhost/readyz` unreachable (`curl` exit 7) |
| Backend readiness | `http://localhost:8080/actuator/health/readiness` unreachable (`curl` exit 7) |
| Running Docker workload | Development `agileapp-postgres-1` only; healthy PostgreSQL 16 |
| Other reachable UI | `http://localhost:5173/` HTTP 200, but it is a Vite development server and is not release evidence |
| Valid deployment screenshots captured | **0** |

No screenshot was captured because doing so would misrepresent a development dependency or Vite
page as a deployed production release. Item 743 remains blocked, and the v1.0 release gate remains
unchanged. This record contains no secret values or customer data.

## Required Screenshot Set

Capture these images only after the exact release images are deployed and item 737 prerequisites are
available. Use the filename pattern `743-<NN>-<short-description>-<UTC>.png`.

| ID | Screenshot | Required visible evidence | Prohibited content |
| --- | --- | --- | --- |
| DEP-01 | Production Compose services | `postgres`, `database-backup`, `backend`, `frontend`, `reverse-proxy`; healthy/running state; image names/digests where safely visible | Environment values, inspect output, credentials, host user directories |
| DEP-02 | Public HTTPS application | Correct production hostname, valid browser TLS indicator, application shell, release-safe synthetic/empty data | Browser history, tokens, real customer names, personal bookmarks |
| DEP-03 | Readiness endpoint | HTTPS `/readyz`, HTTP 200, aggregate `UP` only | Internal component details, DB URL/user, filesystem paths, exception text |
| DEP-04 | Role-aware dashboard | Approved smoke account role, navigation, dashboard loading successfully | Email unless approved/redacted, real KPI/customer data, session information |
| DEP-05 | Application logs | Bounded successful startup/readiness lines, release identity and request ID | Authorization/cookie headers, secrets, payloads, stack traces with sensitive data |
| DEP-06 | Scheduler logs | `run_started` and terminal event sharing one `schedulerRunId`, sanitized counts/duration | Customer details, message bodies, provider credentials |
| DEP-07 | Backup evidence | Healthy backup worker, successful artifact filename/UTC/size/checksum result | Dump contents, database password, mounted secret values |
| DEP-08 | Smoke result | Completed item 737 decision `PASS`, release SHA/digests, operator/approver, UTC execution | Synthetic record contents beyond approved identifiers, credentials |
| DEP-09 | Rollback readiness | Last known-good immutable image digests and verified recovery-point reference in approved record | Secret-bearing `.env.production`, database dump, consent files |

Optional product screenshots may show synthetic customer, segment UUID, campaign product selector,
recipient exclusions, compliance approval, reminders, AI explanation/human approval, and audit history.
They do not replace DEP-01 through DEP-09.

## Local production-like stack (university / Sprint 18 terminal screenshots)

Use this path when you need **terminal and config screenshots** of a production-profile Compose
stack on a developer machine. Self-signed TLS is for local reverse-proxy only; it is **not** a
public CA deployment and does not by itself satisfy a real release gate.

### One-time / each capture session

```powershell
cd D:\Documents\IU\AgileApp

# 1) Ensure Docker Desktop is running
docker info

# 2) Create/update self-signed PEMs + absolute TLS paths in .env.production
powershell -ExecutionPolicy Bypass -File .\scripts\Ensure-LocalProductionTls.ps1

# 3) Start full stack (postgres, database-backup, backend, frontend, reverse-proxy)
#    -EvidenceLocal shortens backup interval + reminder cron for scheduler/backup screenshots
powershell -ExecutionPolicy Bypass -File .\scripts\Start-ProductionStack.ps1 -EvidenceLocal

# 4) Guided capture: status, health, logs, doc paths (add -StartStack to boot inside the helper)
powershell -ExecutionPolicy Bypass -File .\scripts\Capture-Sprint18DeploymentEvidence.ps1
# or boot + wait + capture:
powershell -ExecutionPolicy Bypass -File .\scripts\Capture-Sprint18DeploymentEvidence.ps1 -StartStack -WaitSeconds 180
```

Required files:

| Path | Role |
| --- | --- |
| `.env.production` | Uncommitted secrets + `CORS_ALLOWED_ORIGINS` + absolute `TLS_*` paths |
| `docker/tls/fullchain.pem` / `privkey.pem` | Local self-signed (gitignored `*.pem`) |
| `docker-compose.prod.yml` | Production Compose model |
| `docker-compose.prod.evidence-local.yml` | Optional faster backup/scheduler for screenshots only |

### Terminal commands mapped to evidence

| Evidence | Command / artifact to screenshot |
| --- | --- |
| Production Docker Compose | `docker-compose.prod.yml` services section, or `docker compose ... config --services` |
| Deployment running (DEP-01) | `docker compose --env-file .env.production -f docker-compose.prod.yml ps` |
| Reverse proxy / HTTPS | **Best:** browser padlock on real `https://your-domain` → `03-reverse-proxy-https.png`. **Local only:** screenshot [reverse-proxy-https.md](reverse-proxy-https.md) → `03-reverse-proxy-https-config-note.png` (do not claim localhost is production CA TLS) |
| Health endpoints | `curl.exe -k -i https://localhost/livez` and `/readyz` (expect `{"status":"UP"}`) |
| Application logs | `docker compose ... logs --tail=60 backend` (startup/readiness only; no secrets) |
| Scheduler logs | `docker compose ... logs backend \| Select-String schedulerEvent=` |
| Backup | `docker compose ... logs --tail=30 database-backup` and `ps database-backup` |
| Smoke checklist | `docs/deployment/production-smoke-test-checklist.md` |
| Rollback plan | `docs/deployment/rollback-plan.md` |
| Release | `docs/releases/v1.0-draft.md` |

**Note:** Compose project name is `bwc-production`. Container names look like
`bwc-production-postgres-1`, not `campaign-postgres`. That is correct for this repository.

Self-signed HTTPS requires `curl.exe -k`. Browser warnings are expected for local PEMs.

**CORS:** production rejects `localhost` / `127.0.0.1` in `CORS_ALLOWED_ORIGINS`. Use a fake
HTTPS origin such as `https://campaign.bayer-westphalian.example`. The SPA is same-origin via
Nginx (`VITE_API_BASE_URL=/api`), so browser UI at `https://localhost/` still works for local
screenshots without listing localhost in CORS.

**Backend exit code 137:** Docker killed the JVM (SIGKILL), almost always **out of memory**. Prefer:

```powershell
.\scripts\Start-ProductionStack.ps1 -EvidenceLocal
```

or sequential:

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml build backend
docker compose --env-file .env.production -f docker-compose.prod.yml build frontend
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --no-build
```

Avoid parallel `up -d --build` on small Docker Desktop VMs (~8 GiB). Optionally raise Docker Desktop
memory, stop other stacks (`docker stop agileapp-postgres-1`), and keep defaults
`BACKEND_MEM_LIMIT=1536m` + `JAVA_TOOL_OPTIONS` MaxRAMPercentage in Compose.

### Stop stack

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml down
```

Backend-only (no proxy/frontend/backup) remains:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Start-ProductionBackend.ps1
```

## Capture Preconditions

1. The intended commit is on green `main`, and backend/frontend immutable image digests are recorded.
2. Production Compose or the approved production-like environment is running and healthy.
3. HTTPS hostname/certificate, liveness, readiness, database/storage, and reverse proxy pass preflight.
4. Approved role-specific smoke accounts and uniquely prefixed synthetic data are available.
5. Item 735 backup evidence and item 736 restore rehearsal evidence are current.
6. The browser profile, terminal, desktop, and notification area contain no unrelated sensitive data.
7. Capture owner and independent reviewer are assigned.

## Safe Capture Procedure

1. Set the display to a readable resolution and capture only the relevant application/window region.
2. Prefer application UI and bounded command output. Never open `.env.production`, a secret manager,
   browser storage/devtools tokens, database rows, dump files, or raw consent evidence for a screenshot.
3. Use synthetic data. Hide or redact personal email, phone, address, identifiers, and unrelated
   notifications before capture; do not rely on redaction after distribution.
4. Ensure UTC timestamp, release SHA/digest reference, environment, and evidence ID are recorded in
   the manifest. They need not all appear inside each image when the manifest binds them unambiguously.
5. Review at original resolution for secrets, personal data, internal paths/hostnames, browser chrome,
   and background windows. A second person approves every image.
6. Store approved PNG files in the controlled release-evidence location. This repository may contain
   sanitized university-demo images only after review; production screenshots stay outside Git.

## Evidence Manifest Template

| Field | Value |
| --- | --- |
| Evidence ID / filename | |
| Capture UTC | |
| Environment / public hostname | |
| Release tag / commit | |
| Backend/frontend image digests | |
| Checklist/check ID | |
| Synthetic data prefix | |
| Captured by | |
| Reviewed by / review UTC | |
| Redactions applied before distribution | |
| Approved storage reference / checksum | |
| Notes / linked defect | |

## Acceptance Rule

Item 743 passes only when DEP-01 through DEP-09 exist for the same release/environment, each manifest
entry is complete, every image passes privacy/security review, and the associated smoke execution is
`PASS`. Missing, stale, mixed-release, development-only, secret-bearing, or customer-data-bearing
images block release. Screenshots cannot convert a failed health or smoke check into a pass.

Related documents: [Production Smoke Test Checklist](production-smoke-test-checklist.md),
[Blocked Item 738 Execution](smoke-test-executions/2026-07-12-item-738.md),
[Backup And Restore](backup-and-restore.md), [Rollback Plan](rollback-plan.md), and
[v1.0 Release Notes Draft](../releases/v1.0-draft.md).
