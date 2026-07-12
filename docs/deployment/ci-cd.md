# CI/CD — GitHub Actions

**Sprint 17 item 677** — Add GitHub Actions workflow.  
**Sprint 17 item 678** — Add backend build job.  
**Sprint 17 item 679** — Add backend test job.  
**Sprint 17 item 680** — Add backend integration test job if feasible.  
**Sprint 17 item 681** — Add frontend install job.  
**Sprint 17 item 682** — Add frontend lint job.  
**Sprint 17 item 683** — Add frontend test job.  
**Sprint 17 item 684** — Add frontend build job.  
**Sprint 17 item 685** — Add Docker backend image build.  
**Sprint 17 item 686** — Add Docker frontend image build.  
**Sprint 17 item 687** — Add Docker Compose validation.  
**Sprint 17 item 690** — Add production config validation step.  
**Sprint 17 item 691** — Add release artifact generation.  
**Sprint 17 item 692** — Add CI badge to README.  
**Sprint 17 item 693** — Verify pipeline fails when tests fail.  
**Sprint 17 item 694** — Verify pipeline passes on clean main branch.  
**Sprint 17 item 695** — Add branch protection recommendation.  
**Sprint 17 item 696** — Add release tagging process.  
**Sprint 17 item 697** — Add deployment workflow placeholder.  
**Sprint 17 item 698** — CI runs on pull request.  
**Sprint 17 item 699** — CI runs on main branch.  
**Sprint 17 item 700** — Backend build passes.  
**Sprint 17 item 701** — Backend tests pass.  
**Sprint 17 item 706** — Pipeline fails on intentionally broken test.  
**Sprint 17 item 707** — Pipeline passes on clean main branch.  
**Sprint 17 item 708** — CI/CD documentation.  
**Sprint 17 item 711** — Release tagging guide.

The Bayer-Westphalian Campaign Management Platform uses **GitHub Actions** to automate build,
test, packaging, and release preparation (KB DevOps: Docker + GitHub Actions; epic **E25** /
Sprint 17 goal: *Automate build, test, packaging, and release preparation*).

## Workflow file

| Path | Purpose |
| --- | --- |
| [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) | Primary continuous integration pipeline |

Automated documentation evidence:

| Item | Backend test | Frontend catalog |
| --- | --- | --- |
| **677** | `GitHubActionsWorkflowDocumentationTests` | `githubActionsWorkflow.ts` |
| **678** | `BackendBuildJobDocumentationTests` | `BACKEND_BUILD_JOB` / `workflowYamlDefinesBackendBuildJob` |
| **679** | `BackendTestJobDocumentationTests` | `BACKEND_TEST_JOB` / `workflowYamlDefinesBackendTestJob` |
| **680** | `BackendIntegrationTestJobDocumentationTests` | `BACKEND_INTEGRATION_TEST_JOB` / `workflowYamlDefinesBackendIntegrationTestJob` |
| **681** | `FrontendInstallJobDocumentationTests` | `FRONTEND_INSTALL_JOB` / `workflowYamlDefinesFrontendInstallJob` |
| **682** | `FrontendLintJobDocumentationTests` | `FRONTEND_LINT_JOB` / `workflowYamlDefinesFrontendLintJob` |
| **683** | `FrontendTestJobDocumentationTests` | `FRONTEND_TEST_JOB` / `workflowYamlDefinesFrontendTestJob` |
| **684** | `FrontendBuildJobDocumentationTests` | `FRONTEND_BUILD_JOB` / `workflowYamlDefinesFrontendBuildJob` |
| **685** | `DockerBackendImageBuildDocumentationTests` | `DOCKER_BACKEND_IMAGE_JOB` / `workflowYamlDefinesDockerBackendImageJob` |
| **686** | `DockerFrontendImageBuildDocumentationTests` | `DOCKER_FRONTEND_IMAGE_JOB` / `workflowYamlDefinesDockerFrontendImageJob` |
| **687** | `DockerComposeValidationDocumentationTests` | `DOCKER_COMPOSE_VALIDATION_JOB` / `workflowYamlDefinesDockerComposeValidationJob` |
| **690** | `ProductionConfigValidationStepDocumentationTests` | `PRODUCTION_CONFIG_VALIDATION_JOB` / `workflowYamlDefinesProductionConfigValidationJob` |
| **691** | `ReleaseArtifactGenerationDocumentationTests` | `RELEASE_ARTIFACT_GENERATION` / `workflowYamlDefinesReleaseArtifactGeneration` |
| **692** | `CiBadgeDocumentationTests` | `CI_BADGE` / `readmeDefinesCiBadge` |
| **693** | `PipelineFailsWhenTestsFailDocumentationTests` | `PIPELINE_FAILS_WHEN_TESTS_FAIL` / `workflowYamlDefinesPipelineFailsWhenTestsFail` |
| **694** | `PipelinePassesOnCleanMainBranchDocumentationTests` | `PIPELINE_PASSES_ON_CLEAN_MAIN` / `workflowYamlDefinesPipelinePassesOnCleanMain` |
| **695** | `BranchProtectionRecommendationDocumentationTests` | `branchProtectionRecommendation.ts` |
| **696** | `ReleaseTaggingProcessDocumentationTests` | `releaseTaggingProcess.ts` |
| **697** | `DeploymentWorkflowPlaceholderDocumentationTests` | `deploymentWorkflowPlaceholder.ts` |
| **698** | `CiRunsOnPullRequestDocumentationTests` | `CI_RUNS_ON_PULL_REQUEST` / `workflowYamlDefinesCiRunsOnPullRequest` |
| **699** | `CiRunsOnMainBranchDocumentationTests` | `CI_RUNS_ON_MAIN_BRANCH` / `workflowYamlDefinesCiRunsOnMainBranch` |
| **700** | `BackendBuildPassesDocumentationTests` | `BACKEND_BUILD_PASSES` / `workflowYamlDefinesBackendBuildPasses` |
| **701** | `BackendTestsPassDocumentationTests` | `BACKEND_TESTS_PASS` / `workflowYamlDefinesBackendTestsPass` |
| **706** | `PipelineFailsOnIntentionallyBrokenTestDocumentationTests` | `PIPELINE_FAILS_ON_INTENTIONALLY_BROKEN_TEST` / `scriptDefinesPipelineFailsOnIntentionallyBrokenTest` |
| **707** | `PipelinePassesOnCleanMainRuntimeDocumentationTests` | `PIPELINE_PASSES_ON_CLEAN_MAIN_RUNTIME` / `scriptDefinesPipelinePassesOnCleanMainRuntime` |
| **708** | `CiCdDocumentationExpansionTests` | `CI_CD_DOCUMENTATION` / `markdownDefinesCiCdDocumentation` |
| **711** | `ReleaseTaggingGuideDocumentationTests` | `RELEASE_TAGGING_GUIDE_REQUIRED_MARKERS` / `releaseTaggingDocDefinesGuideMarkers` |

## Triggers

The `CI` workflow runs when:

| Event | Branches | Backlog |
| --- | --- | --- |
| `push` | `main`, `dev` | **699** (main), **694** |
| `pull_request` | targeting `main` or `dev` | **698** |

Concurrency: one active run per workflow + ref; newer runs cancel in-progress ones on the same ref.

Permissions: `contents: read` only (no write tokens required for CI).

## Jobs

| Job id | Name | Purpose | Related backlog |
| --- | --- | --- | --- |
| `backend-build` | Backend build | Java 21 + Maven package (skip tests); upload JAR; pass = **700** | **678**, **691**, **700** |
| `backend-test` | Backend test | Java 21 + full Maven Surefire suite; pass = **701** | **679**, **701** |
| `backend-integration-test` | Backend integration test | Surefire filter `*IntegrationTests` | **680** |
| `frontend-install` | Frontend install | Node 22 + `npm ci` (lockfile install only) | **681** |
| `frontend-lint` | Frontend lint | Node 22 + `npm ci` + `npm run lint` | **682** |
| `frontend-test` | Frontend test | Node 22 + `npm ci` + `npm test` (Vitest) | **683** |
| `frontend-build` | Frontend build | Node 22 + `npm ci` + `npm run build` + assert `dist/`; upload dist | **684**, **691** |
| `docker-backend` | Docker backend image | `docker build` of `backend/Dockerfile` → `bwc-backend:ci` | **685** |
| `docker-frontend` | Docker frontend image | `docker build` of `frontend/Dockerfile` → `bwc-frontend:ci` | **686** |
| `docker-compose-validate` | Docker Compose validation | `docker compose config` model checks (no `up`) | **687** |
| `production-config-validate` | Production config validation | Static prod YAML + validators + templates/docs | **690** |

### Backend build job (item **678**)

Job id: **`backend-build`** (display name: **Backend build**).

Working directory: `backend/`.

Steps:

1. Checkout repository (`actions/checkout@v4`)
2. Setup Temurin JDK **21** with Maven cache on `backend/pom.xml` (`actions/setup-java@v4`)
3. **Build backend** — `mvn -B -DskipTests package` (step id: `backend-build`)
4. **Assert JAR artifact** — fails the job if no packaged `*.jar` exists under `backend/target`

This job proves the backend **compiles and packages** on a clean CI agent. It intentionally does
**not** run `mvn test` (owned by items **679–680**).

Local parity:

```powershell
cd backend
mvn -B -DskipTests package
```

### Backend test job (item **679**)

Job id: **`backend-test`** (display name: **Backend test**).

Working directory: `backend/`.

Steps:

1. Checkout repository (`actions/checkout@v4`)
2. Setup Temurin JDK **21** with Maven cache on `backend/pom.xml` (`actions/setup-java@v4`)
3. **Run backend tests** — `mvn -B test` (step id: `backend-test`)

This job runs the **full** Maven Surefire suite (unit + integration classes). Integration tests that
use **Testcontainers** rely on Docker (available on `ubuntu-latest` GitHub-hosted runners). The job
does **not** run `mvn -DskipTests package` — that remains **678**.

Local parity:

```powershell
cd backend
mvn -B test
```

### Backend integration test job (item **680** — feasible)

Job id: **`backend-integration-test`** (display name: **Backend integration test**).

**Feasibility decision: yes.** The repository already has a consistent integration suite:

- Class names end with `IntegrationTests` (e.g. repository, Flyway, campaign, segment suites)
- Many use `@Testcontainers` + PostgreSQL containers (`disabledWithoutDocker = true`)
- Docker is available on GitHub-hosted `ubuntu-latest` runners

Working directory: `backend/`.

Steps:

1. Checkout repository (`actions/checkout@v4`)
2. Setup Temurin JDK **21** with Maven cache on `backend/pom.xml` (`actions/setup-java@v4`)
3. **Run backend integration tests** — `mvn -B test -Dtest='*IntegrationTests'` (step id: `backend-integration-test`)

This job provides an **explicit integration-only CI signal** separate from the full suite (**679**).
It does **not** package the application (that remains **678**) and does **not** replace the full
suite job.

Local parity:

```powershell
cd backend
mvn -B test "-Dtest=*IntegrationTests"
```

### Frontend install job (item **681**)

Job id: **`frontend-install`** (display name: **Frontend install**).

Working directory: `frontend/`.

Steps:

1. Checkout repository (`actions/checkout@v4`)
2. Setup Node.js **22** with npm cache on `frontend/package-lock.json` (`actions/setup-node@v4`)
3. **Install frontend dependencies** — `npm ci` (step id: `frontend-install`)
4. **Assert `node_modules`** — fails if install did not produce `frontend/node_modules`

This job proves lockfile-based dependency installation succeeds on a clean CI agent. It intentionally
does **not** run lint, Vitest, or production build (owned by items **682–684**).

Local parity:

```powershell
cd frontend
npm ci
```

### Frontend lint job (item **682**)

Job id: **`frontend-lint`** (display name: **Frontend lint**).

Working directory: `frontend/`.

Steps:

1. Checkout repository (`actions/checkout@v4`)
2. Setup Node.js **22** with npm cache on `frontend/package-lock.json` (`actions/setup-node@v4`)
3. **Install frontend dependencies** — `npm ci` (each job is independent on GitHub-hosted runners)
4. **Lint frontend** — `npm run lint` (step id: `frontend-lint`)

This job proves ESLint static analysis passes on a clean CI agent. It intentionally does **not** run
Vitest or production build (owned by items **683–684**).

Local parity:

```powershell
cd frontend
npm ci
npm run lint
```

### Frontend test job (item **683**)

Job id: **`frontend-test`** (display name: **Frontend test**).

Working directory: `frontend/`.

Steps:

1. Checkout repository (`actions/checkout@v4`)
2. Setup Node.js **22** with npm cache on `frontend/package-lock.json` (`actions/setup-node@v4`)
3. **Install frontend dependencies** — `npm ci` (each job is independent on GitHub-hosted runners)
4. **Run frontend unit tests** — `npm test` (Vitest; step id: `frontend-test`)

This job proves the Vitest unit/component suite passes on a clean CI agent. It intentionally does
**not** run ESLint or production build (owned by items **682** and **684**). Playwright E2E is
**not** part of this job.

Local parity:

```powershell
cd frontend
npm ci
npm test
```

### Frontend build job (item **684**)

Job id: **`frontend-build`** (display name: **Frontend build**).

Working directory: `frontend/`.

Steps:

1. Checkout repository (`actions/checkout@v4`)
2. Setup Node.js **22** with npm cache on `frontend/package-lock.json` (`actions/setup-node@v4`)
3. **Install frontend dependencies** — `npm ci` (each job is independent on GitHub-hosted runners)
4. **Build frontend** — `npm run build` (TypeScript project references + Vite production build; step id: `frontend-build`)
5. **Assert `dist/`** — fails if `frontend/dist` or `dist/index.html` is missing

This job proves the production frontend bundle builds on a clean CI agent. It intentionally does
**not** run ESLint or Vitest (owned by items **682–683**). The combined monolithic `frontend` job
was removed once install/lint/test/build were fully split (**681–684**). Playwright E2E is **not**
part of this workflow yet.

Local parity:

```powershell
cd frontend
npm ci
npm run build
```

### Docker backend image build (item **685**)

Job id: **`docker-backend`** (display name: **Docker backend image**).

Dockerfile: [`backend/Dockerfile`](../../backend/Dockerfile) (build context: `backend/`).

Steps:

1. Checkout repository (`actions/checkout@v4`)
2. **Build backend Docker image** — `docker build -t bwc-backend:ci -f backend/Dockerfile backend` (step id: `docker-backend`)
3. **Assert image exists** — `docker image inspect bwc-backend:ci`

The multi-stage Dockerfile:

1. **Build stage** — `maven:3.9-eclipse-temurin-21-alpine` runs `mvn -B -DskipTests package`
2. **Runtime stage** — `eclipse-temurin:21-jre-alpine` runs the fat JAR as non-root user on port **8080**

The job **builds only** (no registry push). Production secrets must not be baked into the image;
runtime env (`JWT_SECRET`, `DB_*`, `SPRING_PROFILES_ACTIVE`, etc.) is supplied when the container
starts. See also [`docker/README.md`](../../docker/README.md).

Local parity:

```powershell
docker build -t bwc-backend:ci -f backend/Dockerfile backend
docker image inspect bwc-backend:ci
```

### Docker frontend image build (item **686**)

Job id: **`docker-frontend`** (display name: **Docker frontend image**).

Dockerfile: [`frontend/Dockerfile`](../../frontend/Dockerfile) (build context: `frontend/`).  
SPA nginx config: [`frontend/nginx.docker.conf`](../../frontend/nginx.docker.conf).

Steps:

1. Checkout repository (`actions/checkout@v4`)
2. **Build frontend Docker image** — `docker build -t bwc-frontend:ci -f frontend/Dockerfile frontend` (step id: `docker-frontend`)
3. **Assert image exists** — `docker image inspect bwc-frontend:ci`

The multi-stage Dockerfile:

1. **Build stage** — `node:22-alpine` runs `npm ci` then `npm run build` (Vite production bundle)
2. **Runtime stage** — `nginx:1.27-alpine` serves `dist/` with SPA `try_files` fallback on port **80**

The job **builds only** (no registry push). API secrets are not required for the static image.
Host/proxy routing to the backend remains documented under [`docker/nginx/nginx.conf`](../../docker/nginx/nginx.conf) for full-stack deployment (Compose validation is item **687**).

Local parity:

```powershell
docker build -t bwc-frontend:ci -f frontend/Dockerfile frontend
docker image inspect bwc-frontend:ci
```

### Docker Compose validation (item **687**)

Job id: **`docker-compose-validate`** (display name: **Docker Compose validation**).

Compose file: [`docker-compose.yml`](../../docker-compose.yml) (local PostgreSQL for development).

Steps:

1. Checkout repository (`actions/checkout@v4`)
2. **Validate Docker Compose configuration** (step id: `docker-compose-validate`):
   - `docker compose -f docker-compose.yml config` (YAML/model parse, no containers)
   - JSON assertions via `jq` that:
     - `services.postgres` exists with image `postgres:16-alpine`
     - postgres has a **healthcheck**
     - named network **`bwc_local`** exists
     - named volume **`bwc_postgres_data`** exists
     - postgres joins **`bwc_local`**

This job **does not** start containers (`docker compose up`) and **does not** push images. Full stack
production Compose is Sprint 18; this item locks the existing local Compose model in CI.

Local parity:

```powershell
# PowerShell (repo scripts)
.\scripts\test-docker-compose-config.ps1

# Cross-platform (same intent as CI)
docker compose -f docker-compose.yml config
```

### Production config validation step (item **690**)

Job id: **`production-config-validate`** (display name: **Production config validation**).

Static repository checks (no Spring Boot start, no real secrets, no Docker push):

1. Checkout repository
2. **Validate production configuration artifacts** (step id: `production-config-validate`):
   - Required files exist: `application-prod.yml`, `EnvironmentVariableValidator`,
     `SecretPresenceValidator`, `ProductionEnvironmentPostProcessor`, env templates, secrets +
     checklist docs
   - `application-prod.yml` hides stack traces, injects `DB_*` / `JWT_SECRET` / `CORS_ALLOWED_ORIGINS`
     from the environment, and does **not** embed localhost CORS defaults
   - Secret strength constants remain `MIN_JWT_SECRET_LENGTH = 32` and
     `MIN_DB_PASSWORD_LENGTH = 8`
   - Env templates document required production keys
   - Secrets/checklist docs still name `JWT_SECRET` / `DB_PASSWORD`

Runtime fail-fast on missing secrets remains an application concern when
`SPRING_PROFILES_ACTIVE=prod` (items **542–543** / **665**). See [secrets.md](secrets.md) and
[security-hardening.md](../architecture/security-hardening.md).

Local parity (inspect artifacts; optional full suite covers validators):

```powershell
# Confirm prod YAML and validators are present
Test-Path backend\src\main\resources\application-prod.yml
Select-String -Path backend\src\main\resources\application-prod.yml -Pattern 'JWT_SECRET|include-stacktrace'
```

### Release artifact generation (item **691**)

CI publishes downloadable **GitHub Actions artifacts** after successful packaging (no container
registry push, no secrets in artifact paths):

| Artifact name | Produced by job | Contents | Retention |
| --- | --- | --- | --- |
| **`bwc-backend-jar`** | `backend-build` | Spring Boot JAR under `backend/target/*.jar` (excludes `*.jar.original`) | 14 days |
| **`bwc-frontend-dist`** | `frontend-build` | Vite production bundle under `frontend/dist` | 14 days |

Implementation: `actions/upload-artifact@v4` with `if-no-files-found: error` so a missing package
fails the job. Download artifacts from the Actions run summary UI after a green build.

Local parity (produce the same files without uploading):

```powershell
# Backend JAR
cd backend
mvn -B -DskipTests package
Get-ChildItem target\*.jar | Where-Object { $_.Name -notlike '*.jar.original' }

# Frontend dist
cd frontend
npm ci
npm run build
Test-Path dist\index.html
```

### CI badge on README (item **692**)

The root [`README.md`](../../README.md) shows a GitHub Actions status badge for the **CI**
workflow (`.github/workflows/ci.yml`), defaulting to branch **`main`** (releasable line; `dev`
also runs CI):

```markdown
[![CI](https://github.com/MahmoudBKanaan/Bayer-Westphalian-CMS/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/MahmoudBKanaan/Bayer-Westphalian-CMS/actions/workflows/ci.yml)
```

The badge image reports pass/fail for recent runs; the link opens the Actions runs for that
workflow. Evidence tests: `CiBadgeDocumentationTests` (backend) and `readmeDefinesCiBadge` /
`CI_BADGE` in `githubActionsWorkflow.ts` (frontend catalog).

### Verify pipeline fails when tests fail (item **693**)

**Fail-on-red** is a configuration contract: quality-gate jobs must surface test/lint failures as
job and workflow failures. GitHub Actions fails a step when the shell command exits non-zero unless
the step sets `continue-on-error: true` or the command swallows failure (`|| true`).

| Job | Command | Fail-on-red rule |
| --- | --- | --- |
| `backend-test` | `mvn -B test` | Surefire failure → job red → workflow red |
| `backend-integration-test` | `mvn -B test -Dtest='*IntegrationTests'` | Same |
| `frontend-lint` | `npm run lint` | ESLint non-zero → job red |
| `frontend-test` | `npm test` | Vitest non-zero → job red |

Verification (static, in-repo):

- Workflow comments mark **693** / fail-on-red on those jobs.
- Documentation tests assert the jobs still run the real commands and do **not** enable
  `continue-on-error: true` or `|| true` on the gate.
- Backend: `PipelineFailsWhenTestsFailDocumentationTests`
- Frontend catalog: `workflowYamlDefinesPipelineFailsWhenTestsFail`

This item does **not** commit a deliberately broken test. Intentional break evidence is backlog
item **706**. Local parity of fail-on-red (optional manual check): temporarily break a unit test,
run `mvn -B test` or `npm test`, confirm non-zero exit, then revert.

### Pipeline fails on intentionally broken test (item **706**)

Runtime evidence for **706** is local and repeatable without committing a failing test:

```powershell
.\scripts\verify-pipeline-fails-on-broken-test.ps1
```

The script creates `frontend/src/__pipeline_broken__.test.ts`, runs
`npm test -- --reporter=dot --silent=true src/__pipeline_broken__.test.ts`, and expects a
non-zero exit code. If the broken test unexpectedly passes, the script exits non-zero and reports
the failure. A `finally` block removes the temporary probe, so no intentionally broken test is
committed or left behind after the check.

Evidence tests:

- Backend: `PipelineFailsOnIntentionallyBrokenTestDocumentationTests`
- Frontend catalog: `PIPELINE_FAILS_ON_INTENTIONALLY_BROKEN_TEST` /
  `scriptDefinesPipelineFailsOnIntentionallyBrokenTest`

### Verify pipeline passes on clean main branch (item **694**)

**Pass-on-green** is the complementary configuration contract to fail-on-red (**693**): when the
repository tree on **`main`** is clean and the local suite is green, a push to `main` (or a PR into
`main`) must run the **full** CI job set and can produce a green workflow.

| Check | Expectation |
| --- | --- |
| Trigger | `push` and `pull_request` include branch **`main`** (and `dev`) |
| Scope | No `paths` / `paths-ignore` filters that skip the workflow |
| Jobs | Full matrix (build, test, lint, Docker, Compose, prod config) without job-level `if:` skips |
| Visibility | Root README CI badge tracks `branch=main` (item **692**) |

Verification (static, in-repo):

- Workflow comments mark **694** / pass-on-green on the `main` trigger.
- Documentation tests assert triggers, full job ids, absence of path filters and skip `if:`.
- Backend: `PipelinePassesOnCleanMainBranchDocumentationTests`
- Frontend catalog: `workflowYamlDefinesPipelinePassesOnCleanMain`

This item does **not** execute GitHub Actions against the remote repository. Runtime green evidence
on `main` is covered by acceptance items **699** / **707** when a clean commit is pushed. Local
parity: run the approximate suite in [Local parity](#local-parity-full-approximate) on a clean
checkout; non-zero exit means main would not go green until fixed (item **715** full suite).

### Pipeline passes on clean main branch (item **707**)

Runtime evidence for **707** is recorded from a clean `main` branch with a clean worktree only:

```powershell
.\scripts\verify-pipeline-passes-on-clean-main.ps1
```

The script refuses to run unless `git branch --show-current` returns `main` and
`git status --porcelain` is empty. It then runs the local CI parity gates: backend package, backend
tests, backend integration test filter, frontend install/lint/test/build, Docker backend/frontend
image builds, Docker Compose validation, and production configuration file presence checks. A
non-zero command exits the script non-zero, so a completed script run is the local evidence that a
clean `main` tree can pass the pipeline.

Use `-PlanOnly` to inspect the command list without claiming runtime pass evidence:

```powershell
.\scripts\verify-pipeline-passes-on-clean-main.ps1 -PlanOnly
```

Evidence tests:

- Backend: `PipelinePassesOnCleanMainRuntimeDocumentationTests`
- Frontend catalog: `PIPELINE_PASSES_ON_CLEAN_MAIN_RUNTIME` /
  `scriptDefinesPipelinePassesOnCleanMainRuntime`

### Branch protection recommendation (item **695**)

GitHub **branch protection** / **rulesets** for releasable **`main`** are documented in
[branch-protection.md](branch-protection.md). Summary:

- Require a pull request into `main` (solo: self-merge after green checks is fine).
- Require status checks for all **CI** job display names (Backend test, Frontend test, …).
- Disallow force push and branch deletion on `main`.
- Aligns with fail-on-red (**693**), pass-on-green (**694**), and item **714**.

Rules are applied in the GitHub UI (not by application code). Evidence tests:
`BranchProtectionRecommendationDocumentationTests` and `branchProtectionRecommendation.ts`.

### Release tagging process and guide (items **696** / **711**)

Git **release tags** for KB versions (`v0.1` … `v1.0`) are documented in
[release-tagging.md](release-tagging.md). Summary:

- Tag only green commits on **`main`** (not red CI, not `dev` as the official release).
- Prefer **annotated** tags (`git tag -a v0.9 -m "..."`) aligned with the KB release plan.
- Do not force-move published tags; optional GitHub Release notes without secrets.
- Preconditions: CI green, branch protection when available, artifacts **691** when needed.
- Item **711** expands the guide with release roles, verification commands, release notes template,
  evidence capture, and troubleshooting.

Evidence tests: `ReleaseTaggingProcessDocumentationTests`, `ReleaseTaggingGuideDocumentationTests`,
and `releaseTaggingProcess.ts`.

### Deployment workflow placeholder (item **697**)

| Path | Workflow name | Trigger |
| --- | --- | --- |
| [`.github/workflows/deploy-placeholder.yml`](../../.github/workflows/deploy-placeholder.yml) | **Deploy (placeholder)** | `workflow_dispatch` only |

This is a **placeholder** for future production deploy automation (KB E26 / Sprint 18). A green run
means the manual workflow executed documentation steps only — **not** that production was updated.

| Property | Behavior |
| --- | --- |
| Trigger | Manual (`workflow_dispatch`); optional inputs for logical env and release tag notes |
| Job | `deploy-placeholder` / **Deployment placeholder** |
| Deploys hosts? | **No** |
| Registry publish? | **No** |
| Secrets in YAML? | **No** (`contents: read` only) |
| Future steps (documented, not run) | Green CI → release tag (**696**) → artifacts (**691**) → prod Compose / smoke (Sprint 18) |

Evidence tests: `DeploymentWorkflowPlaceholderDocumentationTests` and
`deploymentWorkflowPlaceholder.ts` / `workflowYamlDefinesDeploymentPlaceholder`.

### CI runs on pull request (item **698**)

Acceptance: opening or updating a **pull request** that targets **`main`** or **`dev`** starts the
**CI** workflow (`.github/workflows/ci.yml`).

| Check | Expectation |
| --- | --- |
| Event | `on.pull_request` is defined |
| Target branches | `main`, `dev` |
| Scope | No `paths` / `paths-ignore` filters that skip PR CI |
| Jobs | Full quality matrix runs (build, test, lint, Docker, prod config, …) |
| Merge gate | Aligns with branch protection required checks (**695**) and fail-on-red (**693**) |

Verification is **static** (workflow + docs tests). Runtime PR evidence is produced when a PR is
opened against GitHub. Backend: `CiRunsOnPullRequestDocumentationTests`. Frontend:
`workflowYamlDefinesCiRunsOnPullRequest`.

### CI runs on main branch (item **699**)

Acceptance: a **push** to releasable **`main`** starts the **CI** workflow (also configured for
`dev`). Complements pass-on-green (**694**) and the README badge on `branch=main` (**692**).

| Check | Expectation |
| --- | --- |
| Event | `on.push` is defined |
| Branches | includes **`main`** (and `dev`) |
| Scope | No `paths` / `paths-ignore` that skip push CI on main |
| Jobs | Full quality matrix (build, test, lint, Docker, prod config, …) |
| Visibility | Root README CI badge tracks `main` status |

Verification is **static**. Runtime green evidence on `main` is item **707** when a clean commit is
pushed. Backend: `CiRunsOnMainBranchDocumentationTests`. Frontend:
`workflowYamlDefinesCiRunsOnMainBranch`.

### Backend build passes (item **700**)

Acceptance: the **`backend-build`** job is green only when packaging succeeds.

| Check | Expectation |
| --- | --- |
| Job | `backend-build` / **Backend build** (item **678**) |
| Command | `mvn -B -DskipTests package` (Java 21 / Temurin) |
| Artifact | Assert at least one `backend/target/*.jar` (excludes `*.jar.original`) |
| Soft-fail | No `continue-on-error: true` / `\|\| true` on package |
| Scope | Does **not** run `mvn test` (that is **701** / job **679**) |
| Artifacts | Optional upload `bwc-backend-jar` (**691**) after a successful package |

Local parity:

```powershell
cd backend
mvn -B -DskipTests package
Get-ChildItem target\*.jar | Where-Object { $_.Name -notlike '*.jar.original' }
```

Evidence tests: `BackendBuildPassesDocumentationTests` and `workflowYamlDefinesBackendBuildPasses`.

### Backend tests pass (item **701**)

Acceptance: the **`backend-test`** job is green only when the full Maven Surefire suite succeeds.

| Check | Expectation |
| --- | --- |
| Job | `backend-test` / **Backend test** (item **679**) |
| Command | `mvn -B test` (Java 21 / Temurin; unit + integration via Surefire) |
| Soft-fail | No `continue-on-error: true` / `\|\| true` (fail-on-red **693**) |
| Forbidden | `-DskipTests`, package-only, or `*IntegrationTests` filter (integration-only is **680**) |
| Scope | Separate from `backend-build` packaging (**700** / **678**) |

Local parity:

```powershell
cd backend
mvn -B test
```

Evidence tests: `BackendTestsPassDocumentationTests` and `workflowYamlDefinesBackendTestsPass`.

## CI/CD documentation (item **708**)

This page is the CI/CD operating guide and evidence index. It documents what the pipeline does,
how to reproduce the checks locally, what the pipeline deliberately does not do, and which tests
guard the documentation.

### Coverage

| Area | Documented here |
| --- | --- |
| Workflow identity | `.github/workflows/ci.yml`, workflow name **CI**, branch triggers, concurrency, permissions |
| Job matrix | Backend build/test/integration, frontend install/lint/test/build, Docker image build, Compose validation, production config validation |
| Quality gates | Fail-on-red (**693**), pass-on-green (**694**), PR/main trigger evidence (**698-699**) |
| Artifacts | `bwc-backend-jar`, `bwc-frontend-dist`, retention, no registry push |
| Runtime evidence | Intentionally broken test script (**706**) and clean-main parity script (**707**) |
| Local parity | PowerShell/Maven/npm/Docker commands that mirror CI expectations |
| Security notes | Least-privilege workflow permissions, no production secrets, no deployment in CI |

### Boundaries

- CI builds, tests, packages, validates Docker/Compose configuration, and uploads short-lived build artifacts.
- CI does not deploy production, does not push images to a registry, and does not embed production secrets.
- The deployment placeholder remains manual documentation-only until the Sprint 18 deployment work.
- Runtime green evidence must come from a clean `main` tree through item **707**; dirty worktrees can use `-PlanOnly` for inspection only.

### Related Docs

- [Developer setup](../development/developer-setup.md) for local tools and setup.
- [Environment variables](environment-variables.md) and [secrets](secrets.md) for configuration and secret handling.
- [Branch protection](branch-protection.md) for merge gates aligned to required checks.
- [Release tagging](release-tagging.md) for annotating green release commits.
- [Production security checklist](production-security-checklist.md) for production hardening expectations.

### Troubleshooting

| Symptom | First checks |
| --- | --- |
| Backend build red | Run `mvn -B -DskipTests package`; inspect missing JAR or compilation errors |
| Backend test red | Run `mvn -B test`; inspect Surefire reports under `backend/target/surefire-reports` |
| Frontend lint/test/build red | Run `npm run lint`, `npm test`, or `npm run build` in `frontend/` |
| Docker build red | Run the matching `docker build` command locally and inspect Dockerfile/context errors |
| Compose validation red | Run `.\scripts\test-docker-compose-config.ps1` or `docker compose -f docker-compose.yml config` |
| Production config validation red | Check `application-prod.yml`, validators, templates, and secret documentation |

### Maintenance checklist

- Update `.github/workflows/ci.yml`, this guide, `.github/README.md`, and the frontend CI catalog together.
- Add or update a backend documentation test whenever a CI/CD acceptance item gains a new contract.
- Keep local parity commands aligned with CI step commands.
- Keep runtime evidence scripts non-destructive and explicit about when evidence is or is not recorded.
- Preserve least-privilege `permissions: contents: read` unless a future workflow has a documented need.

Evidence tests: `CiCdDocumentationExpansionTests` and `markdownDefinesCiCdDocumentation`.

## Later Sprint 17 items

| Item | Topic |
| --- | --- |
| **688** | Environment variable template — [environment-variables.md](environment-variables.md) |
| **689** | Secrets documentation — [secrets.md](secrets.md) |
| **702** | Frontend lint passes |
| **703** | Frontend tests pass |
| **704** | Frontend build passes |
| **705** | Docker images build successfully |
| **706** | Pipeline fails on intentionally broken test (runtime evidence) |
| **707** | Pipeline passes on clean main branch (runtime evidence) |
| **709** | Environment variable documentation — [environment-variables.md](environment-variables.md) |
| **710** | Secrets documentation — [secrets.md](secrets.md) |
| **711** | Release tagging guide — [release-tagging.md](release-tagging.md) |

## Local parity (full approximate)

```powershell
# Backend build (678)
cd backend
mvn -B -DskipTests package

# Backend full suite (679)
mvn -B test

# Backend integration-only (680)
mvn -B test "-Dtest=*IntegrationTests"

# Frontend install (681)
cd frontend
npm ci

# Frontend lint (682)
npm run lint

# Frontend test (683)
npm test

# Frontend build (684)
npm run build

# Docker backend image (685)
docker build -t bwc-backend:ci -f backend/Dockerfile backend

# Docker frontend image (686)
docker build -t bwc-frontend:ci -f frontend/Dockerfile frontend

# Docker Compose validation (687)
.\scripts\test-docker-compose-config.ps1
# or: docker compose -f docker-compose.yml config

# Production config validation (690) is CI-static; see application-prod.yml + validators

# Release artifacts (691): after package/build, CI uploads bwc-backend-jar and bwc-frontend-dist
```

See also [Developer Setup](../development/developer-setup.md) and [Docker README](../../docker/README.md).

## Security notes

- CI must not embed production secrets in workflow YAML.
- Production secret validation remains an application concern (item **665** / `SecretPresenceValidator`).
- Secrets ops guide: [secrets.md](secrets.md) (item **689**).
- Workflow uses least-privilege `permissions: contents: read`.
