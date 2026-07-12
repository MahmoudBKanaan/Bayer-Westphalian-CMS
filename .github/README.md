# GitHub Configuration

GitHub project automation, issue templates, and CI/CD workflows for the Bayer-Westphalian Campaign
Management Platform.

## Workflows

| Workflow | Path | Purpose |
| --- | --- | --- |
| **CI** | [`workflows/ci.yml`](workflows/ci.yml) | Continuous integration on `push` / `pull_request` to `main` and `dev` (item **698** = PR) |
| **Deploy (placeholder)** | [`workflows/deploy-placeholder.yml`](workflows/deploy-placeholder.yml) | Manual `workflow_dispatch` placeholder (item **697**); does **not** deploy |

### CI jobs

| Job id | Item | Description |
| --- | --- | --- |
| `backend-build` | **678**, **691**, **700** | Java 21 / Maven `package` (skip tests); asserts JAR; uploads **`bwc-backend-jar`** (pass = **700**) |
| `backend-test` | **679**, **701** | Java 21 / Maven full Surefire suite (`mvn test`); pass = **701** |
| `backend-integration-test` | **680** | Surefire filter `*IntegrationTests` (feasible: naming + Testcontainers) |
| `frontend-install` | **681** | Node 22 / `npm ci` from lockfile; asserts `node_modules` |
| `frontend-lint` | **682** | Node 22 / `npm ci` + `npm run lint` (ESLint) |
| `frontend-test` | **683** | Node 22 / `npm ci` + `npm test` (Vitest) |
| `frontend-build` | **684**, **691** | Node 22 / `npm ci` + `npm run build`; asserts `dist/`; uploads **`bwc-frontend-dist`** |
| `docker-backend` | **685** | `docker build` of `backend/Dockerfile` → `bwc-backend:ci` (no push) |
| `docker-frontend` | **686** | `docker build` of `frontend/Dockerfile` → `bwc-frontend:ci` (no push) |
| `docker-compose-validate` | **687** | `docker compose config` model validation (no `up`) |
| `production-config-validate` | **690** | Static prod YAML + validators + env templates / secrets docs |

Release artifacts (item **691**): `actions/upload-artifact@v4` publishes **`bwc-backend-jar`** and
**`bwc-frontend-dist`** (14-day retention, fail if empty). No registry push; no secrets in artifact
paths.

Documentation: [`docs/deployment/ci-cd.md`](../docs/deployment/ci-cd.md).

Automated structure checks (do not execute the pipeline):

- `GitHubActionsWorkflowDocumentationTests` (item **677**)
- `BackendBuildJobDocumentationTests` (item **678**)
- `BackendTestJobDocumentationTests` (item **679**)
- `BackendIntegrationTestJobDocumentationTests` (item **680**)
- `FrontendInstallJobDocumentationTests` (item **681**)
- `FrontendLintJobDocumentationTests` (item **682**)
- `FrontendTestJobDocumentationTests` (item **683**)
- `FrontendBuildJobDocumentationTests` (item **684**)
- `DockerBackendImageBuildDocumentationTests` (item **685**)
- `DockerFrontendImageBuildDocumentationTests` (item **686**)
- `DockerComposeValidationDocumentationTests` (item **687**)
- `ProductionConfigValidationStepDocumentationTests` (item **690**)
- `ReleaseArtifactGenerationDocumentationTests` (item **691**)
- `CiBadgeDocumentationTests` (item **692**)
- `PipelineFailsWhenTestsFailDocumentationTests` (item **693**)
- `PipelinePassesOnCleanMainBranchDocumentationTests` (item **694**)
- `BranchProtectionRecommendationDocumentationTests` (item **695**)
- `ReleaseTaggingProcessDocumentationTests` (item **696**)
- `DeploymentWorkflowPlaceholderDocumentationTests` (item **697**)
- `CiRunsOnPullRequestDocumentationTests` (item **698**)
- `CiRunsOnMainBranchDocumentationTests` (item **699**)
- `BackendBuildPassesDocumentationTests` (item **700**)
- `BackendTestsPassDocumentationTests` (item **701**)
- `PipelineFailsOnIntentionallyBrokenTestDocumentationTests` (item **706**)
- `PipelinePassesOnCleanMainRuntimeDocumentationTests` (item **707**)
- `CiCdDocumentationExpansionTests` (item **708**)
- `ReleaseTaggingGuideDocumentationTests` (item **711**)
- `frontend/src/features/ops/githubActionsWorkflow.ts`
- `frontend/src/features/ops/branchProtectionRecommendation.ts`
- `frontend/src/features/ops/releaseTaggingProcess.ts`
- `frontend/src/features/ops/deploymentWorkflowPlaceholder.ts`

**CI badge (item **692**):** root [`README.md`](../README.md) embeds the Actions status badge for
workflow `ci.yml` (`branch=main`). See [ci-cd.md](../docs/deployment/ci-cd.md#ci-badge-on-readme-item-692).

**Fail-on-red (item **693**):** quality-gate jobs (`backend-test`, `backend-integration-test`,
`frontend-lint`, `frontend-test`) must not set `continue-on-error: true`. See
[ci-cd.md](../docs/deployment/ci-cd.md#verify-pipeline-fails-when-tests-fail-item-693).

**Pass-on-green (item **694**):** push to `main` runs the full job set without path filters or job
`if:` skips so a clean green tree can produce a green workflow. See
[ci-cd.md](../docs/deployment/ci-cd.md#verify-pipeline-passes-on-clean-main-branch-item-694).

**Branch protection (item **695**):** protect releasable `main` with required CI checks and no force
push — [branch-protection.md](../docs/deployment/branch-protection.md).

**Release tagging (item **696**):** annotate KB versions (`v0.1`…`v1.0`) on green `main` only —
[release-tagging.md](../docs/deployment/release-tagging.md).
Item **711** expands the release tagging guide with roles, verification commands, release notes,
evidence capture, and troubleshooting. Evidence: `ReleaseTaggingGuideDocumentationTests`.

**Deploy placeholder (item **697**):** [`deploy-placeholder.yml`](workflows/deploy-placeholder.yml)
is manual only and does not update production (Sprint 18 owns real deploy).

**CI on pull request (item **698**):** `pull_request` to `main`/`dev` runs the full **CI** job
matrix — [ci-cd.md](../docs/deployment/ci-cd.md#ci-runs-on-pull-request-item-698).

**CI on main (item **699**):** `push` to **`main`** (and `dev`) runs the full **CI** job matrix —
[ci-cd.md](../docs/deployment/ci-cd.md#ci-runs-on-main-branch-item-699).

**Backend build passes (item **700**):** `backend-build` green requires Maven package + JAR assert —
[ci-cd.md](../docs/deployment/ci-cd.md#backend-build-passes-item-700).

**Backend tests pass (item **701**):** `backend-test` green requires `mvn -B test` without soft-fail —
[ci-cd.md](../docs/deployment/ci-cd.md#backend-tests-pass-item-701).

**Pipeline fails on intentionally broken test (item **706**):** run
[`../scripts/verify-pipeline-fails-on-broken-test.ps1`](../scripts/verify-pipeline-fails-on-broken-test.ps1)
to create a temporary failing Vitest probe, confirm `npm test` exits non-zero, and remove the probe.
See [ci-cd.md](../docs/deployment/ci-cd.md#pipeline-fails-on-intentionally-broken-test-item-706).

**Pipeline passes on clean main branch (item **707**):** run
[`../scripts/verify-pipeline-passes-on-clean-main.ps1`](../scripts/verify-pipeline-passes-on-clean-main.ps1)
from a clean `main` worktree to execute the local CI parity gates. See
[ci-cd.md](../docs/deployment/ci-cd.md#pipeline-passes-on-clean-main-branch-item-707).

**CI/CD documentation (item **708**):** [`docs/deployment/ci-cd.md`](../docs/deployment/ci-cd.md)
is the operational index for CI jobs, triggers, artifacts, runtime evidence, local parity, security
notes, and maintenance checks. Evidence: `CiCdDocumentationExpansionTests`.

Later Sprint 17 items: none currently listed after release guide expansion (**711**).
