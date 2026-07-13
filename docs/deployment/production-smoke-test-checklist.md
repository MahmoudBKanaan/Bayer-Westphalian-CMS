# Production Smoke Test Checklist

**Sprint 18 items 737 and 763** define the production smoke gate and final operator checklist for
the Bayer-Westphalian Campaign Management Platform. Complete this checklist after deployment, after
any production restore, and after correcting a failed critical check. Item 738 records an execution;
this document defines the checks and acceptance criteria.

Item 738 execution history:

- [2026-07-12 - BLOCKED: production deployment unavailable](smoke-test-executions/2026-07-12-item-738.md)

## Execution Record

| Field | Value |
| --- | --- |
| Environment and base URL | |
| Release version / image digest | |
| Git commit | |
| Deployment timestamp (UTC) | |
| Test start and finish (UTC) | |
| Browser / client version | |
| Operator | |
| Approver | |
| Change / incident identifier | |
| Previous failed execution / retest reference | |
| Test-data prefix | `SMOKE-<UTC timestamp>-` |
| Backup recovery point | |
| Consent-evidence recovery point | |
| Final decision | `PASS` / `BLOCKED` |

## Preflight

Do not begin workflow checks until the release identity, environment, operator, and approver are
recorded. Confirm CI passed for the deployed commit, the production deployment guide was followed,
the rollback owner and known-good image digests are available, a fresh verified backup and matching
consent-evidence recovery point exist, and no unresolved critical incident is active. A failed
preflight is `BLOCKED`, not `N/A`.

## Safety Rules

- Use approved synthetic customers, addresses, consent evidence, segments, and campaigns carrying
  the execution's unique test-data prefix. Never alter a real customer to make a check pass.
- Keep real email and SMS sending disabled unless provider activation is separately approved and
  the destination is an organization-controlled test address or number.
- Do not launch a campaign to real recipients. A launch check requires an approved synthetic-only
  segment and human approval.
- Do not capture passwords, JWTs, database URLs, customer data, consent evidence contents, or stack
  traces in screenshots or logs. Redact request IDs only when policy requires it; they are useful
  for correlation.
- Stop and mark the release `BLOCKED` on a security, consent, eligibility, audit, backup, migration,
  health, or critical workflow failure. Do not work around authorization or data safeguards.

For every row record `PASS`, `FAIL`, `BLOCKED`, or `N/A`, the UTC time, tester, evidence reference,
and incident/defect identifier. `N/A` requires approver justification and is forbidden for checks
marked **Critical**.

## A. Deployment And Transport

| ID | Critical | Check | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| SMK-001 | Yes | Inspect deployed image/version and active profile | Intended immutable release is running with `prod`; no local/demo profile | |
| SMK-002 | Yes | Open the public HTTPS base URL | Valid trusted certificate; expected hostname; no browser TLS warning | |
| SMK-003 | Yes | Request the HTTP URL | Redirects to HTTPS; API requests cannot bypass HTTPS enforcement | |
| SMK-004 | Yes | Inspect a secure response | HSTS and security headers are present; server does not disclose secrets or stack traces | |
| SMK-005 | Yes | Request from the configured frontend origin, then an unapproved origin | Approved CORS origin works; unapproved origin receives no access-control grant | |
| SMK-006 | Yes | Load and refresh a deep frontend route | Application shell renders and reverse proxy preserves SPA routing | |

## B. Runtime Health And Observability

| ID | Critical | Check | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| SMK-010 | Yes | `GET /actuator/health/liveness` through the internal probe path | HTTP 200 and `UP` | |
| SMK-011 | Yes | `GET /actuator/health/readiness` through the internal probe path | HTTP 200 and `UP`, including required database readiness | |
| SMK-012 | Yes | Inspect `postgres`, `backend`, `frontend`, and `reverse-proxy` container health | All required services are running and healthy; no restart loop | |
| SMK-013 | Yes | Correlate one browser/API request with backend logs | Request ID is present and searchable; no credential or payload leakage | |
| SMK-014 | No | Inspect scheduler startup and most recent run logs | Scheduler identity, start, completion/outcome, duration, and counts are visible without sensitive data | |
| SMK-015 | Yes | Request an unknown API route and trigger a safe validation error | Structured safe error; no Java stack trace, SQL detail, filesystem path, or secret | |

## C. Authentication And Authorization

| ID | Critical | Check | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| SMK-020 | Yes | Sign in as the approved smoke-test account | Login succeeds over HTTPS and the correct user/roles appear | |
| SMK-021 | Yes | Attempt login with an invalid password | Generic failure; no account/password disclosure; rate-limit behavior remains enabled | |
| SMK-022 | Yes | Sign out and reuse the former session | Session/token is no longer accepted according to the configured token policy | |
| SMK-023 | Yes | Check menus with Campaign Manager, Compliance Officer, Admin, and System Auditor accounts | Each role sees only its authorized navigation | |
| SMK-024 | Yes | Directly request an Admin API/UI route as Campaign Manager | Access is denied even when bypassing menu navigation | |
| SMK-025 | Yes | Check bootstrap state | `ADMIN_BOOTSTRAP_ENABLED=false`; bootstrap password removed; seeded `.test` users disabled | |

## D. Core Read Workflows

| ID | Critical | Check | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| SMK-030 | Yes | Open dashboard and user-appropriate navigation | Shell, top bar, page heading, loading/empty/error states render correctly | |
| SMK-031 | Yes | Search customers and open one synthetic customer | Relevant results and customer details load without an unexpected error | |
| SMK-032 | Yes | Open products and product details | Product list and ownership/status data load | |
| SMK-033 | Yes | Open segments and segment details as Campaign Manager | Segments load and UUID is visible; add/edit controls follow authorization | |
| SMK-034 | Yes | Open campaigns and one campaign detail | Status, selected product, segment, approval state, and recipient information agree | |
| SMK-035 | No | Open reminders and follow-up tasks | Reminder-level badges, assignments, completion state, and scheduler data load | |
| SMK-036 | No | Open analytics/report views with authorized roles | Aggregates load; unauthorized roles cannot access them | |

## E. Controlled Mutation Workflow

| ID | Critical | Check | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| SMK-040 | Yes | Create a prefixed synthetic customer | Validation works; success notification appears; valid UUID is assigned | |
| SMK-041 | Yes | Add valid synthetic consent evidence, then inspect status | Evidence persists in configured storage; consent status and audit event agree | |
| SMK-042 | Yes | Create and edit a prefixed segment as Campaign Manager | UUID is automatic and valid; criteria persist; details show the same UUID | |
| SMK-043 | Yes | Create and edit a prefixed campaign | Product selector exists in both forms; product and segment persist; campaign remains `DRAFT` | |
| SMK-044 | Yes | Submit campaign and review as Compliance Officer | Human approval/rejection is required and creates audit history | |
| SMK-045 | Yes | Preview recipients for a consented synthetic-only segment | Eligibility, consent, do-not-contact, frequency, and uninterested exclusions are applied | |
| SMK-046 | Yes | Attempt preview/launch with a synthetic opted-out or do-not-contact customer | Customer is excluded; AI and UI cannot bypass `EligibilityService` | |
| SMK-047 | Conditional | Launch an approved synthetic-only campaign after confirmation | Only run with release approver permission; status becomes `ACTIVE`, audit event exists, no real recipient contacted | |
| SMK-048 | No | Create, assign, and complete a prefixed follow-up task | Assignee and completion persist; success feedback and audit/operational trace are available | |

## F. AI And Compliance Safeguards

| ID | Critical | Check | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| SMK-050 | No | Run fuzzy customer search with a safe query | Relevant customers and score explanation appear | |
| SMK-051 | No | Request product and segment recommendations | Recommendation, confidence when available, and explanation are stored/displayed | |
| SMK-052 | Yes | Generate campaign-copy suggestion | Suggestion remains pending until a human approves it; AI cannot approve campaign | |
| SMK-053 | Yes | Exercise duplicate-contact warning on synthetic repeated contact history | Warning appears and cannot override consent, do-not-contact, or eligibility | |
| SMK-054 | Yes | Inspect audit logs as System Auditor, then as unauthorized role | Auditor can filter/view immutable events; unauthorized user is denied; no edit action exists | |

## G. Providers, Storage, Backup, And Recovery

| ID | Critical | Check | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| SMK-060 | Yes | Inspect email/SMS provider policy | Real sending is correctly configured and approved, or remains explicitly disabled | |
| SMK-061 | Yes | Inspect consent-evidence volume and one synthetic file | Persistent volume is mounted; authorized retrieval works; unauthorized retrieval fails | |
| SMK-062 | Yes | Inspect database-backup health and latest sanitized logs | Backup worker is healthy; recent successful backup has no credential leakage | |
| SMK-063 | Yes | Run item 735 backup creation verification | New non-empty dump, valid SHA-256 manifest, and readable archive; record artifact name only | |
| SMK-064 | Yes | Review latest item 736 non-production restore rehearsal | Verified restore evidence exists for this release or approved current recovery point | |
| SMK-065 | Yes | Confirm backup off-host handling | Encrypted, access-controlled off-host copy and retention owner are recorded | |

## H. Cleanup And Gate Decision

| ID | Critical | Check | Expected result | Result / evidence |
| --- | --- | --- | --- | --- |
| SMK-070 | Yes | Remove or deactivate prefixed test records according to retention policy | No synthetic campaign can contact recipients; cleanup actions are audited | |
| SMK-071 | Yes | Recheck readiness, logs, and container state after mutations | System remains healthy; no new unexplained errors or restart loops | |
| SMK-072 | Yes | Review all failed, blocked, conditional, and `N/A` checks | Defects and approvals are linked; no Critical check is `FAIL`, `BLOCKED`, or `N/A` | |
| SMK-073 | Yes | Record operator and independent approver decision | Both sign off on the same release, environment, evidence set, and UTC execution window | |

## Acceptance Rule

The production smoke gate is `PASS` only when every Critical check passes, every executed mutation
uses synthetic data, backup and restore evidence is current, no unexplained high-severity log event
remains, cleanup is complete, and both operator and approver sign the execution record. Otherwise
the decision is `BLOCKED`; follow the rollback or incident process and rerun the complete checklist
after correction. Partial execution is never release evidence.

## Evidence Handling

Store the completed checklist, sanitized command output, screenshots, request IDs, image digests,
backup filename/checksum result, restore-rehearsal result, defect links, and approval in the approved
release evidence location. Evidence must be timestamped and traceable to one environment and commit.
Do not commit runtime evidence containing production information to this repository.

Related controls: [Production Security Checklist](production-security-checklist.md),
[Production Deployment Guide](production-deployment-guide.md), [Production Backup Guide](backup-guide.md),
[Production Restore Guide](restore-guide.md), [Health Endpoints](health-endpoints.md),
[Production Logging](production-logging.md), [Production Rollback Plan](rollback-plan.md), and
[Sprint 15 Production Gate](../agile/sprint-15-production-gate.md).

Automated item 763 evidence: `FinalSmokeTestChecklistDocumentationTests`.
