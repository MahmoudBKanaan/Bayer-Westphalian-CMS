# Developer Setup Guide

This guide prepares a local development environment for the Bayer-Westphalian Campaign Management Platform.

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

Frontend:

```powershell
Copy-Item frontend\.env.example frontend\.env
```

Backend:

```powershell
Copy-Item backend\.env.example backend\.env
```

Do not commit real `.env` files. The KB requires production secrets to be supplied by the deployment environment or a secret manager.

## Local Database

Start PostgreSQL from the project root:

```powershell
docker compose up -d postgres
docker compose ps
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
