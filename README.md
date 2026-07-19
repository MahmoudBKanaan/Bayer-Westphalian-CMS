# Bayer-Westphalian Campaign Management Platform

[![CI](https://github.com/MahmoudBKanaan/Bayer-Westphalian-CMS/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/MahmoudBKanaan/Bayer-Westphalian-CMS/actions/workflows/ci.yml)

<!-- Sprint 17 item 692: CI badge reflects .github/workflows/ci.yml on releasable main. -->

Internal enterprise CRM, campaign management, and marketing automation system for
Bayer-Westphalian Insurance. It is for authorized employees, not public customer signup.

## Project Identity

| Item | Value |
| --- | --- |
| Project name | Bayer-Westphalian Campaign Management Platform |
| Business domain | Insurance marketing and business intelligence |
| Main business case | Manage customers, beneficiaries, consent, products, and compliant employee-operated campaigns |
| Architecture | React and Spring Boot modular monolith with PostgreSQL |
| Delivery model | Solo-adapted Scrum; no fictional multi-person project team |
| Product status | v1.0 candidate; production release remains blocked pending the item 770 evidence gate |
| Release status | `v1.0` is **DRAFT - NOT RELEASED** |

## Current Status

The application and production-oriented runbooks are implemented. That does not prove a production
release. Exact-commit CI, a deployed HTTPS environment, critical role workflows, off-host backup and
restore evidence, full smoke execution, and human approval must all be recorded.

Production is allowed only after the fail-closed
[item 770 release gate](docs/deployment/production-release-gate.md) passes. The
[v1.0 release notes](docs/releases/v1.0-draft.md) record current blocked and pending gates.

## Core Capabilities

- Employee authentication, JWT access/refresh tokens, account controls, backend authorization,
  protected frontend routes, and role-filtered navigation.
- Customer/prospect, beneficiary, consent, opt-out, product, ownership, payment, segment, campaign,
  communication, reminder, and follow-up management.
- Central eligibility checks for consent, do-not-contact, uninterested, retry, and monthly contact
  limits. AI and UI cannot bypass `EligibilityService`.
- Recipient preview, exclusion explanations, human compliance approval, and controlled launch.
- Dashboards, reports, fuzzy AI search, recommendation explanations, default-risk scoring,
  duplicate-contact warnings, and human-approved campaign-copy suggestions.
- Immutable audit logging for sensitive actions, with restricted read-only audit access.
- Production Compose, Nginx HTTPS, health checks, bounded logs, scheduler observability, persistent
  consent evidence, PostgreSQL backups, restore rehearsal, smoke, rollback, and incident runbooks.

## System Roles

| Role | Main responsibility |
| --- | --- |
| Admin | Employee accounts, roles, settings, and administration |
| Campaign Manager | Segments, campaign drafts, preview, and approved launch |
| Compliance Officer | Consent/compliance review and human campaign approval |
| Customer Service Agent / Sales Agent | Customer service, contacts, and follow-up work |
| Product Manager | Products, ownership, and product-change workflows |
| BI Analyst / Marketing Analyst | Analytics, reports, and campaign insight |
| Executive Viewer | Read-only executive dashboards and reports |
| System Auditor | Read-only audit, consent, approval, and user-activity review |

## Technology

| Layer | Technology |
| --- | --- |
| Frontend | React, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, Recharts |
| Backend | Java 21, Spring Boot, Spring Security, Spring Data JPA/Hibernate |
| API and auth | REST JSON, Springdoc OpenAPI, JWT access and refresh tokens |
| Database | PostgreSQL 16 with Flyway migrations |
| Tests | JUnit, Mockito, Spring Boot Test, Testcontainers, Vitest, RTL, Playwright |
| Delivery | GitHub Actions, Docker, Docker Compose, Nginx TLS reverse proxy |

## Repository Structure

```text
.
+-- backend/                 Spring Boot API, migrations, and tests
+-- frontend/                React application and tests
+-- config/                  Safe configuration/evidence examples
+-- docker/                  Proxy, PostgreSQL, backup, and TLS assets
+-- docs/                    Architecture, module, user, test, and operations guides
+-- scripts/                 Local CI and production verification helpers
+-- .github/workflows/       CI and deployment-placeholder workflows
+-- docker-compose.yml       Local PostgreSQL
+-- docker-compose.prod.yml  Production stack model
```

## Quick Start

Prerequisites: Node.js 22+, npm, Java 21, Maven 3.9+, Docker Engine/Desktop, and Docker Compose v2.

1. Start local PostgreSQL from the repository root:

   ```powershell
   docker compose up -d postgres
   docker compose ps
   ```

2. Start the backend in another terminal:

   ```powershell
   Set-Location backend
   mvn spring-boot:run
   ```

3. Install and start the frontend in another terminal:

   ```powershell
   Set-Location frontend
   npm install
   npm run dev
   ```

4. Open the Vite URL printed by the frontend. Development Swagger UI is normally available at
   `http://localhost:8080/swagger-ui.html` when enabled by the active profile.

Environment templates contain placeholders only. Never commit real `.env` files or secrets. Follow
the [Developer Setup Guide](docs/development/developer-setup.md),
[Environment Variable Guide](docs/deployment/environment-variables.md), and
[Secrets Guide](docs/deployment/secrets.md).

## Development Commands

Run from the indicated package directory:

```powershell
### Frontend
Set-Location frontend
npm install
npm run verify

### Backend
Set-Location ../backend
mvn verify
```

Useful focused commands:

| Area | Command |
| --- | --- |
| Frontend lint | `cd frontend; npm run lint` |
| Frontend tests | `cd frontend; npm run test` |
| Frontend build | `cd frontend; npm run build` |
| Backend tests | `cd backend; mvn test` |
| Backend package/full gate | `cd backend; mvn verify` |
| Local database | `docker compose up -d postgres` |
| Compose verification | `.\scripts\test-docker-compose-config.ps1` |

The local PostgreSQL defaults are database `bwc_campaign`, user `bwc_app`, and host port `5432`.

## Quality Gates

The repository contains backend unit, integration, security, persistence, and documentation tests;
frontend unit, integration, accessibility, and Playwright coverage; lint/build checks; Docker checks;
and production configuration contracts. A historical or local pass is not release evidence for a
newer commit.

GitHub Actions runs on pull requests and pushes to `main`/`dev`. Releasable `main` requires green CI
for the exact commit. Production additionally requires item 770 evidence for smoke tests, backups,
security and environment configuration, provider policy, rollback readiness, critical workflows,
and human approval. See [CI/CD](docs/deployment/ci-cd.md).

## Environments

| Environment | Purpose |
| --- | --- |
| `dev` | Local development with development-only conveniences |
| `test` | Automated isolated verification |
| `prod` | Fail-fast secure configuration, HTTPS, restricted CORS, safe errors, and provider policy |

## Production Rules

- Core authorization, consent, eligibility, approval, audit, and customer-data rules must be real.
- Mocking is limited to development, tests, synthetic demos, or replaceable provider adapters.
- Secrets and customer/consent data must never be committed to Git.
- Production uses HTTPS, HSTS, explicit CORS origins, safe errors, and bounded secret-safe logging.
- Sensitive actions are auditable; normal users cannot edit audit logs.
- Database changes are version-controlled, forward-only Flyway migrations.
- Real email/SMS sending must be approved and configured, or explicitly disabled.
- A green CI run alone does not authorize production; every item 770 gate must pass.

## Architecture And API

React calls REST endpoints exposed by Spring Boot; Spring Security enforces authorization;
PostgreSQL is the system of record; Flyway owns schema evolution; and Nginx is the production HTTPS
edge. See [Architecture](docs/architecture/initial-architecture.md),
[Role-Based Access](docs/architecture/role-based-access.md), and
[OpenAPI/Swagger](docs/api/openapi.md).

Production OpenAPI and Swagger are disabled by default. Do not expose them publicly without an
approved access decision.

## Project Process

The KB is delivered with solo-adapted Scrum. The developer performs Product Owner, Scrum Master,
development, and QA responsibilities transparently. Work moves through Product Backlog, Sprint
Backlog, In Progress, Blocked, Self Review, Testing, and Done. Jira/GitHub board references remain
project metadata until live hosted URLs are recorded.

## Release Strategy

| Release | Goal |
| --- | --- |
| v0.1 | Project foundation |
| v0.2 | Secure access |
| v0.3 | CRM and compliance base |
| v0.4 | Products and segmentation |
| v0.5 | Campaign lifecycle |
| v0.6 | Communication and reminders |
| v0.7 | Analytics and AI |
| v0.8 | Audit and hardening |
| v0.9 | Production candidate |
| v1.0 | Production-ready MVP |

## Documentation

- [Complete documentation index](docs/README.md)
- [Developer setup](docs/development/developer-setup.md)
- [Documentation readiness map](docs/documentation-readiness.md)
- [Maintainer guide](docs/maintenance/maintainer-guide.md)
- [User guides](docs/README.md#user-guides) and [admin guides](docs/README.md#admin)
- [API documentation](docs/api/openapi.md)
- [Production deployment guide](docs/deployment/production-deployment-guide.md)
- [Production operations manual](docs/operations/operations-guide.md)
- [Backup guide](docs/deployment/backup-guide.md) and [restore guide](docs/deployment/restore-guide.md)
- [Smoke checklist](docs/deployment/production-smoke-test-checklist.md),
  [rollback plan](docs/deployment/rollback-plan.md), and
  [incident notes](docs/deployment/incident-response-notes.md)
- [v1.0 release notes](docs/releases/v1.0-draft.md) (**draft, not released**)
- [Final demo dataset](docs/demo/final-demo-dataset.md) and
  [20-minute demo script](docs/demo/final-demo-script.md)
- [Final delivery package checklist](docs/handover/final-delivery-package-checklist.md)

## Security And Data Handling

Use synthetic data for development, tests, screenshots, and demonstrations. Never commit customer
data, consent evidence, database dumps, passwords, JWTs, API keys, private keys, or production
environment files. Report suspected exposure through the
[incident-response process](docs/deployment/incident-response-notes.md).

## License And Use

No public open-source license is declared. Treat the project and its data as internal/university
work unless the owner provides separate license and usage terms.
[![CI](https://github.com/MahmoudBKanaan/Bayer-Westphalian-CMS/actions/workflows/ci.yml/badge.svg?branch=dev)](https://github.com/MahmoudBKanaan/Bayer-Westphalian-CMS/actions/workflows/ci.yml)
