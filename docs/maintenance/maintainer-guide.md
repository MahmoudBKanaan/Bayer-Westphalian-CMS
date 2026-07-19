# Maintainer Guide

This guide is the maintenance entry point for KB item **864**. Read `README.md` and the final report
first, then use this guide to locate ownership boundaries and make changes safely.

## System Map

| Concern | Primary location | Supporting documentation |
| --- | --- | --- |
| Backend domains and API | `backend/src/main/java/com/bayerwestphalian/campaign` | `docs/modules`, `docs/api/openapi.md` |
| Database schema | `backend/src/main/resources/db/migration` | `docs/database/migration-strategy.md` |
| Frontend pages and shell | `frontend/src/pages`, `frontend/src/app` | user guides and `docs/development/ui-style-notes.md` |
| Authentication/authorization | backend `auth`, frontend auth/route modules | architecture role/auth/security guides |
| Eligibility and consent | backend eligibility/consent modules | `docs/architecture/eligibility-rules.md` |
| Production runtime | Dockerfiles, `docker-compose.prod.yml`, `docker` | deployment and operations manuals |
| Automated verification | backend/frontend test trees, `.github/workflows` | `docs/testing` and CI/CD guide |

## Safe Change Workflow

1. Read the relevant module guide, tests and decision log before editing.
2. Confirm the business role, acceptance conditions, authorization and audit implications.
3. Preserve package boundaries and existing DTO/service/repository patterns.
4. Add a new Flyway migration for schema changes; never edit an applied migration.
5. Update backend validation and authorization before relying on frontend controls.
6. Add focused tests proportional to the behavior and shared-surface risk.
7. Update OpenAPI, user/operator documentation and diagrams when contracts change.
8. Run `npm run verify`, `mvn verify` and applicable E2E/Docker checks.
9. Review `git diff` for unrelated changes, generated churn, secrets and personal data.
10. Merge through exact-commit green CI; follow the separate production gate for releases.

## Critical Invariants

Changes must preserve these controls:

- AI, UI and scheduler workflows cannot bypass `EligibilityService`.
- Consent, DNC, opt-out, guardian consent and monthly contact limits remain authoritative.
- AI copy approval and campaign compliance approval remain separate human actions.
- A campaign cannot launch before compliance approval.
- Sensitive actions remain auditable; normal users cannot modify audit history.
- Backend authorization remains authoritative even when frontend menus hide an action.
- Production fails safely on missing secrets, unsafe CORS, provider ambiguity and unsafe errors.

## Database Maintenance

- Add migrations using the next version under `backend/src/main/resources/db/migration`.
- Keep migrations forward-only, deterministic and compatible with PostgreSQL 16.
- Review constraints/indexes and data migration impact before deployment.
- Back up before rollout; rehearse restore in non-production for release-critical changes.
- Never run `flyway clean`, destructive ad-hoc SQL or `docker compose down -v` in production.

## Dependency Maintenance

- Update one ecosystem or coherent dependency group at a time.
- Read release notes and security advisories; avoid blind major-version upgrades.
- Preserve Java 21, the Maven build, Node/npm lock resolution and supported PostgreSQL baseline unless
  an approved decision changes them.
- Run full lint, tests and build after lock-file or plugin changes.
- Rebuild Docker images and validate Compose after runtime dependency changes.
- Record architecture-affecting decisions in `docs/agile/decision-log.md`.

## API and Frontend Maintenance

- Treat request/response DTOs and `docs/api/openapi.json` as contracts.
- Prefer backward-compatible additions; document intentional breaking changes and version impact.
- Keep create/edit workflows consistent, including validation and selectors.
- Preserve loading, empty, error and success states, labels, keyboard operation and confirmations.
- Test role visibility and direct unauthorized API access separately.

## Operations and Incident Maintenance

- Use `docs/operations/operations-guide.md` for routine lifecycle and ownership.
- Use the monitoring, logging, backup/restore, rollback and incident runbooks under `docs/deployment`.
- Keep logs bounded and free of credentials, tokens, customer data and stack traces in production.
- Test scheduler/provider changes with sending disabled or approved synthetic recipients.
- Preserve evidence before recovery actions and record the exact commit/image/environment.

## Documentation Maintenance Rule

A behavior change is incomplete until the relevant audience can discover it:

| Change | Required documentation review |
| --- | --- |
| Developer setup/build | README and developer setup guide |
| Endpoint or DTO | API guide and OpenAPI export |
| Role/workflow/UI | user/admin manuals and role guides |
| Business rule | module/architecture guide, test map and UML where applicable |
| Environment/security | environment, secrets and security guides |
| Deployment/runtime | deployment and operations manuals, smoke/rollback procedures |
| Release behavior | release notes, gate evidence and handover checklist |

## Handover Health Check

A new maintainer should be able to explain the architecture, start the dev stack, run verification,
locate a domain rule, trace an API endpoint to its service/repository, add a migration, diagnose a
failed health check, and identify why `v1.0` is or is not releasable. Use the documentation-readiness
map to perform this walkthrough during handover.
