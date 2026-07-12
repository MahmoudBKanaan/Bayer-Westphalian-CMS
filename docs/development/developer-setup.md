# Developer Setup Guide

This guide prepares a local development environment for the Bayer-Westphalian Campaign Management Platform.

Frontend visual conventions (shell, colors, badges, forms, responsive layout) are documented in
[UI Style Notes](ui-style-notes.md) (Sprint 15 item **610**). Accessibility baseline guidance
(landmarks, keyboard, labels, contrast, testing map) is documented in
[Accessibility Notes](accessibility-notes.md) (Sprint 15 item **611**).

## Prerequisites

Install these tools before starting:

| Tool | Purpose |
| --- | --- |
| Git | Source control |
| Node.js 22 or later | Frontend development and tests |
| npm | Frontend package management |
| Java 21 | Backend runtime and compilation |
| Maven 3.9 or later | Backend build, tests, formatting, and linting |
| Docker Desktop | Local PostgreSQL database |

The backend currently supports a local Maven installation. A Maven wrapper can be added later for fully pinned build execution.

Continuous integration runs the same major build/test steps via GitHub Actions (Sprint 17 items
**677–687**, **690–701**: workflow, backend package/tests/integration, frontend
install/lint/test/build, Docker images, Compose validation, production config static checks,
release artifact uploads for the backend JAR and frontend `dist/`, a **CI** status badge on the
root README, fail-on-red / pass-on-green contracts, **branch protection** for releasable **`main`**,
a **release tagging** process for KB versions `v0.1`…`v1.0`, a **deployment workflow placeholder**,
**CI on pull request** / **push to main**, and acceptance that **backend build** and **backend tests
pass**). Environment variable templates are item **688**
([environment-variables.md](../deployment/environment-variables.md)); secrets ops guide is item
**689** ([secrets.md](../deployment/secrets.md)); branch protection is item **695**
([branch-protection.md](../deployment/branch-protection.md)); release tagging is item **696**
([release-tagging.md](../deployment/release-tagging.md)); deploy placeholder is item **697**
([ci-cd.md](../deployment/ci-cd.md#deployment-workflow-placeholder-item-697) /
[`.github/workflows/deploy-placeholder.yml`](../../.github/workflows/deploy-placeholder.yml));
PR CI is item **698**
([ci-cd.md](../deployment/ci-cd.md#ci-runs-on-pull-request-item-698)); main-branch CI is item **699**
([ci-cd.md](../deployment/ci-cd.md#ci-runs-on-main-branch-item-699)); backend build pass is item **700**
([ci-cd.md](../deployment/ci-cd.md#backend-build-passes-item-700)); backend tests pass is item **701**
([ci-cd.md](../deployment/ci-cd.md#backend-tests-pass-item-701)). See
[CI/CD — GitHub Actions](../deployment/ci-cd.md) and
[`.github/workflows/ci.yml`](../../.github/workflows/ci.yml).

## Repository Setup

From the project root:

```powershell
git status
```

Expected project folders:

```text
frontend/
backend/
docs/
docker/
scripts/
.github/
```

## Environment Files

Sprint 17 item **688** provides checked-in **environment variable templates** (no real secrets):

| Template | Purpose |
| --- | --- |
| [`.env.example`](../../.env.example) | Full-stack reference checklist |
| [`backend/.env.example`](../../backend/.env.example) | Spring Boot backend |
| [`frontend/.env.example`](../../frontend/.env.example) | Vite frontend |

Catalog: [Environment Variable Template](../deployment/environment-variables.md).

Frontend:

```powershell
Copy-Item frontend\.env.example frontend\.env
```

Backend:

```powershell
Copy-Item backend\.env.example backend\.env
```

Do not commit real `.env` files. The KB requires production secrets to be supplied by the deployment
environment or a secret manager — see [Secrets Documentation](../deployment/secrets.md) (item
**689**).

## Local Database

Start PostgreSQL from the project root:

```powershell
docker compose up -d postgres
docker compose ps
```

Validate the Docker Compose configuration and PostgreSQL startup:

```powershell
.\scripts\test-docker-compose-config.ps1
.\scripts\test-docker-compose-postgres.ps1
```

Default local database settings:

| Setting | Value |
| --- | --- |
| Database | `bwc_campaign` |
| User | `bwc_app` |
| Password | `bwc_app` |
| Port | `5432` |
| JDBC URL | `jdbc:postgresql://localhost:5432/bwc_campaign` |

Stop local services:

```powershell
docker compose down
```

Remove the local database volume only when a full reset is intended:

```powershell
docker compose down -v
```

## Frontend Setup

From the frontend folder:

```powershell
cd frontend
npm install
npm run dev
```

Default Vite URL:

```text
http://localhost:5173
```

Frontend quality commands:

```powershell
npm run lint
npm run format:check
npm run test
npm run build
npm run verify
```

Use this command to apply frontend formatting:

```powershell
npm run format
```

## Backend Setup

From the backend folder:

```powershell
cd backend
mvn spring-boot:run
```

The backend dev profile is configured for local PostgreSQL. If needed, set:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
```

Backend quality commands:

```powershell
mvn test
mvn spotless:check
mvn checkstyle:check
mvn verify
```

Use this command to apply backend formatting:

```powershell
mvn spotless:apply
```

The PostgreSQL integration test connects to the local Docker Compose database on port `5432`. Start PostgreSQL before running the complete backend test suite when you want that integration check to execute.

## Unit and Integration Tests

Use this matrix during local development and before commits:

| Area | Command | Coverage |
| --- | --- | --- |
| Frontend unit tests | `cd frontend; npm run test` | Format helpers and API request behavior |
| Frontend integration tests | `cd frontend; npm run test` | App routing, dashboard redirect, campaigns page, and login shell |
| Frontend full gate | `cd frontend; npm run verify` | ESLint, Prettier, Vitest, and production build |
| Backend unit tests | `cd backend; mvn test` | Spring context and OpenAPI metadata |
| Backend integration tests | `cd backend; mvn test` | Actuator health and PostgreSQL connectivity |
| Backend full gate | `cd backend; mvn verify` | Tests, package, Spotless, and Checkstyle |
| Docker config test | `.\scripts\test-docker-compose-config.ps1` | Compose service, network, volume, and health check model |
| Docker integration test | `.\scripts\test-docker-compose-postgres.ps1` | PostgreSQL startup, readiness, and SQL smoke query |

Current test files:

| Area | Files |
| --- | --- |
| Frontend | `src/app/App.test.tsx`, `src/app/router.integration.test.tsx`, `src/api/client.test.ts`, `src/utils/format.test.ts` |
| Backend | `CampaignApplicationTests.java`, `OpenApiConfigurationTests.java`, `HealthEndpointIntegrationTests.java`, `PostgreSqlConnectionIntegrationTests.java` |

## API Documentation

When the backend is running:

| Resource | URL |
| --- | --- |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Health Check | `http://localhost:8080/actuator/health` |

## Development Workflow

Use this sequence for normal local work:

1. Pull the latest changes.
2. Start PostgreSQL with Docker Compose.
3. Start the backend with the `dev` profile.
4. Start the frontend Vite server.
5. Make focused changes.
6. Run frontend and backend verification commands.
7. Review `git status` before committing.

## Quality Baseline

Frontend:

- ESLint covers React, TypeScript, React Hooks, and Vite React Refresh rules.
- Prettier owns frontend formatting.
- `npm run verify` runs linting, formatting checks, tests, and production build.

Backend:

- Spotless formats Java and checks selected resource/docs files.
- Checkstyle enforces import hygiene, naming, line length, tabs, braces, and statement basics.
- `mvn verify` runs tests, formatting checks, and lint checks.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Frontend cannot reach backend | Confirm `VITE_API_BASE_URL` in `frontend/.env` |
| Backend cannot connect to database | Confirm Docker is running and `docker compose ps` shows `postgres` healthy |
| Port `5432` is busy | Set `POSTGRES_PORT` before starting Docker Compose |
| Port `5173` is busy | Start Vite with `npm run dev -- --port 5174` |
| Maven command not found | Install Maven or add it to the system `PATH` |
| Java version mismatch | Confirm `java -version` reports Java 21 |
