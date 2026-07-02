# Bayer-Westphalian Campaign Management Platform

Internal enterprise CRM, campaign management, and marketing automation system for Bayer-Westphalian Insurance.

The platform is for authorized internal employees only. It supports customer and prospect management, beneficiaries, consent and opt-out handling, products, segmentation, campaigns, recipient preview, communication tracking, follow-ups, reminders, analytics, reports, audit logging, role-based access control, and AI-assisted recommendations.

## Project Identity

| Item | Value |
| --- | --- |
| Project name | Bayer-Westphalian Campaign Management Platform |
| System type | Internal enterprise CRM, campaign management, and marketing automation system |
| Business domain | Insurance marketing and business intelligence |
| Primary users | Internal Bayer-Westphalian employees |
| Main business case | Market insurance and investment products to grandchildren or beneficiaries of life-insurance payout customers while supporting automated campaigns |
| Product status | Operation-ready business system; mock providers allowed only for development and testing |

## Stack

| Layer | Technology |
| --- | --- |
| Frontend | React, TypeScript, Vite |
| Routing | React Router |
| Data fetching | TanStack Query |
| Forms | React Hook Form |
| Frontend validation | Zod |
| UI | Tailwind CSS or MUI |
| Charts | Recharts |
| Backend | Java 21, Spring Boot |
| API | REST JSON |
| Security | Spring Security |
| Auth | JWT access token plus refresh token, or secure session |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Mapping | MapStruct or manual mappers |
| Backend tests | JUnit, Mockito, Spring Boot Test, Testcontainers |
| Frontend tests | Vitest, React Testing Library, Playwright |
| DevOps | Docker, Docker Compose, GitHub Actions |
| Deployment | Docker containers with Nginx or Caddy |

## Repository Structure

```text
.
+-- frontend/
|   +-- public/
|   +-- src/
|       +-- app/
|       +-- api/
|       +-- components/
|       +-- features/
|       +-- pages/
|       +-- types/
|       +-- utils/
+-- backend/
|   +-- src/
|       +-- main/
|       |   +-- java/com/bayerwestphalian/campaign/
|       |   +-- resources/db/migration/
|       +-- test/
+-- docs/
+-- docker/
+-- scripts/
+-- .github/
```

## Setup Plan

1. Create the repository folder structure.
2. Generate the React + TypeScript + Vite frontend in `frontend/`.
3. Generate the Spring Boot backend in `backend/` with Java 21.
4. Add Docker Compose for PostgreSQL.
5. Configure backend profiles: `dev`, `test`, and `prod`.
6. Configure frontend environment files.
7. Add OpenAPI/Swagger documentation.
8. Add code formatting, linting, and test baselines.
9. Add GitHub Actions for build, test, and package checks.

## Planning Boards

The KB defines Scrum delivery with a Jira project, sprint backlog issues, and planned sprints. Use these references for project tracking:

| Tool | Reference |
| --- | --- |
| GitHub repository | `bayer-westphalian-campaign-platform` |
| GitHub project | `Bayer-Westphalian Campaign Platform Delivery` |
| Jira project key | `BWC` |
| Jira board | `BWC Scrum Board` |
| Jira workflow | `Product Backlog -> Sprint Backlog -> In Progress -> Blocked -> Self Review -> Testing -> Done` |
| Sprint plan | `Sprint 1` through `Sprint 19` |

Replace these placeholders with live GitHub Project and Jira board URLs once the hosted tools are created. Keep Jira issue IDs, sprint numbers, release versions, and pull request references aligned during delivery.

## Development Commands

Run commands from the project root unless noted.

### Prerequisites

- Node.js 22 or later
- npm
- Java 21
- Maven 3.9 or later
- Docker Desktop

### Git

```bash
git status
```

### Frontend

Install dependencies and run the frontend checks:

```bash
cd frontend
npm install
npm run test
npm run build
```

Run the local Vite development server:

```bash
cd frontend
npm run dev
```

### Backend

Install Maven locally or add a Maven wrapper before replacing these commands with `./mvnw`.

```bash
cd backend
mvn test
mvn package
```

Run the local Spring Boot development server:

```powershell
cd backend
mvn spring-boot:run
```

### Docker

```bash
docker compose up -d
docker compose ps
docker compose logs -f
docker compose down
```

Run Docker verification scripts on Windows PowerShell:

```powershell
.\scripts\test-docker-compose-config.ps1
.\scripts\test-docker-compose-postgres.ps1
```

The local PostgreSQL service uses `docker-compose.yml` and defaults to:

- database: `bwc_campaign`
- user: `bwc_app`
- port: `5432`

## Environments

| Environment | Purpose |
| --- | --- |
| dev | Local development with developer-friendly settings |
| test | Automated tests and isolated verification |
| prod | Production configuration with secure secrets, HTTPS, real providers, and hardened error handling |

## Production Rules

- Core compliance, permission, consent, eligibility, campaign approval, audit, and customer data logic must be fully implemented.
- Mocking is allowed only for development, testing, demonstration data, or replaceable external provider adapters before real integration.
- Secrets must never be committed to Git.
- Production must use HTTPS and restricted CORS.
- Sensitive actions must be audited.
- Database changes must be version-controlled through migrations.

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
