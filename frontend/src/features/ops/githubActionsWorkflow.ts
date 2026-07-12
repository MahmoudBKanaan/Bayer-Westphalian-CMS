/**
 * Sprint 17 item **677**: Add GitHub Actions workflow.
 * Sprint 17 item **678**: Backend build job (`backend-build`).
 * Sprint 17 item **679**: Backend test job (`backend-test`).
 * Sprint 17 item **680**: Backend integration test job (`backend-integration-test`, feasible).
 * Sprint 17 item **681**: Frontend install job (`frontend-install`).
 * Sprint 17 item **682**: Frontend lint job (`frontend-lint`).
 * Sprint 17 item **683**: Frontend test job (`frontend-test`).
 * Sprint 17 item **684**: Frontend build job (`frontend-build`).
 * Sprint 17 item **685**: Docker backend image build (`docker-backend`).
 * Sprint 17 item **686**: Docker frontend image build (`docker-frontend`).
 * Sprint 17 item **687**: Docker Compose validation (`docker-compose-validate`).
 * Sprint 17 item **690**: Production config validation (`production-config-validate`).
 * Sprint 17 item **691**: Release artifact generation (`upload-artifact` for JAR + dist).
 * Sprint 17 item **692**: CI badge on root README.
 * Sprint 17 item **693**: Verify pipeline fails when tests fail (fail-on-red).
 * Sprint 17 item **694**: Verify pipeline passes on clean main branch (pass-on-green).
 * Sprint 17 item **698**: CI runs on pull request.
 * Sprint 17 item **699**: CI runs on main branch.
 * Sprint 17 item **700**: Backend build passes (`backend-build`).
 * Sprint 17 item **701**: Backend tests pass (`backend-test`).
 * Sprint 17 item **706**: Pipeline fails on intentionally broken test.
 * Sprint 17 item **707**: Pipeline passes on clean main branch.
 * Sprint 17 item **708**: CI/CD documentation.
 *
 * KB: CI/CD via GitHub Actions for build, test, and package checks. Catalog locks workflow path,
 * triggers, and job foundations (pipeline is not executed by these unit tests).
 */

export const GITHUB_ACTIONS_WORKFLOW_ITEM = 677;

export const GITHUB_ACTIONS_WORKFLOW_STATEMENT = "Add GitHub Actions workflow";

export const BACKEND_BUILD_JOB_ITEM = 678;

export const BACKEND_BUILD_JOB_STATEMENT = "Add backend build job";

export const BACKEND_TEST_JOB_ITEM = 679;

export const BACKEND_TEST_JOB_STATEMENT = "Add backend test job";

export const BACKEND_INTEGRATION_TEST_JOB_ITEM = 680;

export const BACKEND_INTEGRATION_TEST_JOB_STATEMENT =
  "Add backend integration test job if feasible";

export const FRONTEND_INSTALL_JOB_ITEM = 681;

export const FRONTEND_INSTALL_JOB_STATEMENT = "Add frontend install job";

export const FRONTEND_LINT_JOB_ITEM = 682;

export const FRONTEND_LINT_JOB_STATEMENT = "Add frontend lint job";

export const FRONTEND_TEST_JOB_ITEM = 683;

export const FRONTEND_TEST_JOB_STATEMENT = "Add frontend test job";

export const FRONTEND_BUILD_JOB_ITEM = 684;

export const FRONTEND_BUILD_JOB_STATEMENT = "Add frontend build job";

export const DOCKER_BACKEND_IMAGE_JOB_ITEM = 685;

export const DOCKER_BACKEND_IMAGE_JOB_STATEMENT = "Add Docker backend image build";

export const DOCKER_FRONTEND_IMAGE_JOB_ITEM = 686;

export const DOCKER_FRONTEND_IMAGE_JOB_STATEMENT = "Add Docker frontend image build";

export const DOCKER_COMPOSE_VALIDATION_JOB_ITEM = 687;

export const DOCKER_COMPOSE_VALIDATION_JOB_STATEMENT = "Add Docker Compose validation";

export const PRODUCTION_CONFIG_VALIDATION_JOB_ITEM = 690;

export const PRODUCTION_CONFIG_VALIDATION_JOB_STATEMENT =
  "Add production config validation step";

export const RELEASE_ARTIFACT_GENERATION_ITEM = 691;

export const RELEASE_ARTIFACT_GENERATION_STATEMENT = "Add release artifact generation";

export const CI_BADGE_ITEM = 692;

export const CI_BADGE_STATEMENT = "Add CI badge to README";

export const PIPELINE_FAILS_WHEN_TESTS_FAIL_ITEM = 693;

export const PIPELINE_FAILS_WHEN_TESTS_FAIL_STATEMENT =
  "Verify pipeline fails when tests fail";

export const PIPELINE_FAILS_ON_INTENTIONALLY_BROKEN_TEST_ITEM = 706;

export const PIPELINE_FAILS_ON_INTENTIONALLY_BROKEN_TEST_STATEMENT =
  "Pipeline fails on intentionally broken test";

export const PIPELINE_PASSES_ON_CLEAN_MAIN_RUNTIME_ITEM = 707;

export const PIPELINE_PASSES_ON_CLEAN_MAIN_RUNTIME_STATEMENT =
  "Pipeline passes on clean main branch";

export const CI_CD_DOCUMENTATION_ITEM = 708;

export const CI_CD_DOCUMENTATION_STATEMENT = "CI/CD documentation";

export const PIPELINE_PASSES_ON_CLEAN_MAIN_ITEM = 694;

export const PIPELINE_PASSES_ON_CLEAN_MAIN_STATEMENT =
  "Verify pipeline passes on clean main branch";

export const CI_RUNS_ON_PULL_REQUEST_ITEM = 698;

export const CI_RUNS_ON_PULL_REQUEST_STATEMENT = "CI runs on pull request";

export const CI_RUNS_ON_MAIN_BRANCH_ITEM = 699;

export const CI_RUNS_ON_MAIN_BRANCH_STATEMENT = "CI runs on main branch";

export const BACKEND_BUILD_PASSES_ITEM = 700;

export const BACKEND_BUILD_PASSES_STATEMENT = "Backend build passes";

export const BACKEND_TESTS_PASS_ITEM = 701;

export const BACKEND_TESTS_PASS_STATEMENT = "Backend tests pass";

export const GITHUB_ACTIONS_WORKFLOW_PATH = ".github/workflows/ci.yml";

export const GITHUB_ACTIONS_WORKFLOW_NAME = "CI";

export const CI_CD_DOC_PATH = "docs/deployment/ci-cd.md";

export const ROOT_README_PATH = "README.md";

export const BACKEND_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.GitHubActionsWorkflowDocumentationTests";

export const BACKEND_BUILD_JOB_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.BackendBuildJobDocumentationTests";

export const BACKEND_TEST_JOB_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.BackendTestJobDocumentationTests";

export const BACKEND_INTEGRATION_TEST_JOB_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.BackendIntegrationTestJobDocumentationTests";

export const FRONTEND_INSTALL_JOB_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.FrontendInstallJobDocumentationTests";

export const FRONTEND_LINT_JOB_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.FrontendLintJobDocumentationTests";

export const FRONTEND_TEST_JOB_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.FrontendTestJobDocumentationTests";

export const FRONTEND_BUILD_JOB_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.FrontendBuildJobDocumentationTests";

export const DOCKER_BACKEND_IMAGE_JOB_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.DockerBackendImageBuildDocumentationTests";

export const DOCKER_FRONTEND_IMAGE_JOB_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.DockerFrontendImageBuildDocumentationTests";

export const DOCKER_COMPOSE_VALIDATION_JOB_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.DockerComposeValidationDocumentationTests";

export const PRODUCTION_CONFIG_VALIDATION_JOB_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.ProductionConfigValidationStepDocumentationTests";

export const RELEASE_ARTIFACT_GENERATION_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.ReleaseArtifactGenerationDocumentationTests";

export const CI_BADGE_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.CiBadgeDocumentationTests";

export const PIPELINE_FAILS_WHEN_TESTS_FAIL_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.PipelineFailsWhenTestsFailDocumentationTests";

export const PIPELINE_FAILS_ON_INTENTIONALLY_BROKEN_TEST_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.PipelineFailsOnIntentionallyBrokenTestDocumentationTests";

export const PIPELINE_PASSES_ON_CLEAN_MAIN_RUNTIME_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.PipelinePassesOnCleanMainRuntimeDocumentationTests";

export const CI_CD_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.CiCdDocumentationExpansionTests";

export const PIPELINE_PASSES_ON_CLEAN_MAIN_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.PipelinePassesOnCleanMainBranchDocumentationTests";

export const CI_RUNS_ON_PULL_REQUEST_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.CiRunsOnPullRequestDocumentationTests";

export const CI_RUNS_ON_MAIN_BRANCH_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.CiRunsOnMainBranchDocumentationTests";

export const BACKEND_BUILD_PASSES_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.BackendBuildPassesDocumentationTests";

export const BACKEND_TESTS_PASS_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.BackendTestsPassDocumentationTests";

/** Branches that receive push and pull_request CI. */
export const CI_TRIGGER_BRANCHES = ["main", "dev"] as const;

/** Releasable branch expected to go green on a clean tree (item 694). */
export const CI_CLEAN_PASS_BRANCH = "main";

/** GitHub owner/repo used in the public Actions status badge URLs. */
export const CI_BADGE_REPOSITORY = "MahmoudBKanaan/Bayer-Westphalian-CMS";

/** Branch reflected on the default README CI badge (releasable line). */
export const CI_BADGE_BRANCH = "main";

/** Job ids defined in the CI workflow after items 677–690 (691 attaches artifacts to build jobs). */
export const CI_JOB_IDS = [
  "backend-build",
  "backend-test",
  "backend-integration-test",
  "frontend-install",
  "frontend-lint",
  "frontend-test",
  "frontend-build",
  "docker-backend",
  "docker-frontend",
  "docker-compose-validate",
  "production-config-validate",
] as const;

export type CiJobId = (typeof CI_JOB_IDS)[number];

/** Backend build job contract (item 678). */
export const BACKEND_BUILD_JOB = {
  id: "backend-build",
  name: "Backend build",
  workingDirectory: "backend",
  javaVersion: "21",
  packageCommand: "mvn -B -DskipTests package",
  stepId: "backend-build",
} as const;

/** Backend test job contract (item 679). */
export const BACKEND_TEST_JOB = {
  id: "backend-test",
  name: "Backend test",
  workingDirectory: "backend",
  javaVersion: "21",
  testCommand: "mvn -B test",
  stepId: "backend-test",
} as const;

/** Backend integration test job contract (item 680 — feasible). */
export const BACKEND_INTEGRATION_TEST_JOB = {
  id: "backend-integration-test",
  name: "Backend integration test",
  workingDirectory: "backend",
  javaVersion: "21",
  testCommand: "mvn -B test -Dtest='*IntegrationTests'",
  surefireInclude: "*IntegrationTests",
  stepId: "backend-integration-test",
  feasible: true,
  feasibilityRationale:
    "Integration classes use *IntegrationTests naming and Testcontainers PostgreSQL on Docker-capable runners",
} as const;

/** Frontend install job contract (item 681). */
export const FRONTEND_INSTALL_JOB = {
  id: "frontend-install",
  name: "Frontend install",
  workingDirectory: "frontend",
  nodeVersion: "22",
  installCommand: "npm ci",
  stepId: "frontend-install",
  packageLockPath: "frontend/package-lock.json",
} as const;

/** Frontend lint job contract (item 682). */
export const FRONTEND_LINT_JOB = {
  id: "frontend-lint",
  name: "Frontend lint",
  workingDirectory: "frontend",
  nodeVersion: "22",
  installCommand: "npm ci",
  lintCommand: "npm run lint",
  stepId: "frontend-lint",
} as const;

/** Frontend test job contract (item 683). */
export const FRONTEND_TEST_JOB = {
  id: "frontend-test",
  name: "Frontend test",
  workingDirectory: "frontend",
  nodeVersion: "22",
  installCommand: "npm ci",
  testCommand: "npm test",
  stepId: "frontend-test",
} as const;

/** Frontend build job contract (item 684). */
export const FRONTEND_BUILD_JOB = {
  id: "frontend-build",
  name: "Frontend build",
  workingDirectory: "frontend",
  nodeVersion: "22",
  installCommand: "npm ci",
  buildCommand: "npm run build",
  stepId: "frontend-build",
} as const;

/** Docker backend image job contract (item 685). */
export const DOCKER_BACKEND_IMAGE_JOB = {
  id: "docker-backend",
  name: "Docker backend image",
  dockerfilePath: "backend/Dockerfile",
  contextPath: "backend",
  imageTag: "bwc-backend:ci",
  buildCommand: "docker build -t bwc-backend:ci -f backend/Dockerfile backend",
  stepId: "docker-backend",
} as const;

/** Docker frontend image job contract (item 686). */
export const DOCKER_FRONTEND_IMAGE_JOB = {
  id: "docker-frontend",
  name: "Docker frontend image",
  dockerfilePath: "frontend/Dockerfile",
  contextPath: "frontend",
  imageTag: "bwc-frontend:ci",
  buildCommand: "docker build -t bwc-frontend:ci -f frontend/Dockerfile frontend",
  stepId: "docker-frontend",
} as const;

/** Docker Compose validation job contract (item 687). */
export const DOCKER_COMPOSE_VALIDATION_JOB = {
  id: "docker-compose-validate",
  name: "Docker Compose validation",
  composeFile: "docker-compose.yml",
  validateCommand: 'docker compose -f "docker-compose.yml" config',
  stepId: "docker-compose-validate",
  startsContainers: false,
} as const;

/** Production config validation job contract (item 690). */
export const PRODUCTION_CONFIG_VALIDATION_JOB = {
  id: "production-config-validate",
  name: "Production config validation",
  prodYamlPath: "backend/src/main/resources/application-prod.yml",
  stepId: "production-config-validate",
  startsApplication: false,
  usesRealSecrets: false,
} as const;

/** Release artifact generation contract (item 691). */
export const RELEASE_ARTIFACT_GENERATION = {
  action: "actions/upload-artifact@v4",
  backendArtifactName: "bwc-backend-jar",
  frontendArtifactName: "bwc-frontend-dist",
  backendPath: "backend/target/*.jar",
  frontendPath: "frontend/dist",
  retentionDays: 14,
  ifNoFilesFound: "error",
  attachesToJobs: ["backend-build", "frontend-build"] as const,
} as const;

/** CI status badge contract for the root README (item 692). */
export const CI_BADGE = {
  altText: "CI",
  workflowFile: "ci.yml",
  workflowName: GITHUB_ACTIONS_WORKFLOW_NAME,
  branch: CI_BADGE_BRANCH,
  repository: CI_BADGE_REPOSITORY,
  imageUrl: `https://github.com/${CI_BADGE_REPOSITORY}/actions/workflows/ci.yml/badge.svg?branch=${CI_BADGE_BRANCH}`,
  linkUrl: `https://github.com/${CI_BADGE_REPOSITORY}/actions/workflows/ci.yml`,
  readmePath: ROOT_README_PATH,
} as const;

/** Required README markers for the CI badge (item 692). */
export const CI_BADGE_REQUIRED_MARKERS = [
  "[![CI](",
  "actions/workflows/ci.yml/badge.svg",
  "branch=main",
  "MahmoudBKanaan/Bayer-Westphalian-CMS",
  "692",
] as const;

/**
 * Fail-on-red quality-gate jobs (item 693): test/lint commands must fail the job when the suite
 * fails. Configuration is verified statically (no intentional red run here; see item 706).
 */
export const PIPELINE_FAILS_WHEN_TESTS_FAIL = {
  item: PIPELINE_FAILS_WHEN_TESTS_FAIL_ITEM,
  statement: PIPELINE_FAILS_WHEN_TESTS_FAIL_STATEMENT,
  qualityGateJobs: [
    "backend-test",
    "backend-integration-test",
    "frontend-lint",
    "frontend-test",
  ] as const,
  backendTestCommand: "mvn -B test",
  backendIntegrationTestCommand: "mvn -B test -Dtest='*IntegrationTests'",
  frontendTestCommand: "npm test",
  frontendLintCommand: "npm run lint",
  forbiddenSoftFail: "continue-on-error: true",
  forbiddenShellSwallow: "|| true",
} as const;

/** Required workflow markers documenting fail-on-red (item 693). */
export const PIPELINE_FAILS_WHEN_TESTS_FAIL_REQUIRED_MARKERS = [
  "693",
  "fail-on-red",
  "backend-test:",
  "mvn -B test",
  "backend-integration-test:",
  "frontend-test:",
  "npm test",
  "frontend-lint:",
  "npm run lint",
] as const;

export const PIPELINE_FAILS_ON_INTENTIONALLY_BROKEN_TEST_SCRIPT_PATH =
  "scripts/verify-pipeline-fails-on-broken-test.ps1";

/**
 * Runtime evidence for item 706: create a temporary failing Vitest spec, prove npm test exits
 * non-zero, and clean up the probe so no intentionally broken test is committed.
 */
export const PIPELINE_FAILS_ON_INTENTIONALLY_BROKEN_TEST = {
  item: PIPELINE_FAILS_ON_INTENTIONALLY_BROKEN_TEST_ITEM,
  statement: PIPELINE_FAILS_ON_INTENTIONALLY_BROKEN_TEST_STATEMENT,
  scriptPath: PIPELINE_FAILS_ON_INTENTIONALLY_BROKEN_TEST_SCRIPT_PATH,
  temporaryTestPath: "frontend/src/__pipeline_broken__.test.ts",
  command: "npm test -- --reporter=dot --silent=true src/__pipeline_broken__.test.ts",
  expectedExitCode: "non-zero",
  cleanupRequired: true,
} as const;

export const PIPELINE_FAILS_ON_INTENTIONALLY_BROKEN_TEST_REQUIRED_MARKERS = [
  "706",
  "Pipeline fails on intentionally broken test",
  "verify-pipeline-fails-on-broken-test.ps1",
  "__pipeline_broken__.test.ts",
  "npm test",
  "$LASTEXITCODE",
  "Remove-Item",
  "finally",
] as const;

export const PIPELINE_PASSES_ON_CLEAN_MAIN_RUNTIME_SCRIPT_PATH =
  "scripts/verify-pipeline-passes-on-clean-main.ps1";

/**
 * Runtime evidence for item 707: run the local CI parity commands from a clean main worktree.
 */
export const PIPELINE_PASSES_ON_CLEAN_MAIN_RUNTIME = {
  item: PIPELINE_PASSES_ON_CLEAN_MAIN_RUNTIME_ITEM,
  statement: PIPELINE_PASSES_ON_CLEAN_MAIN_RUNTIME_STATEMENT,
  scriptPath: PIPELINE_PASSES_ON_CLEAN_MAIN_RUNTIME_SCRIPT_PATH,
  requiredBranch: "main",
  requiresCleanWorktree: true,
  commands: [
    "mvn -B -DskipTests package",
    "mvn -B test",
    "mvn -B test -Dtest=*IntegrationTests",
    "npm ci",
    "npm run lint",
    "npm test",
    "npm run build",
    "docker build -t bwc-backend:ci -f backend/Dockerfile backend",
    "docker build -t bwc-frontend:ci -f frontend/Dockerfile frontend",
    "scripts/test-docker-compose-config.ps1",
  ] as const,
} as const;

export const PIPELINE_PASSES_ON_CLEAN_MAIN_RUNTIME_REQUIRED_MARKERS = [
  "707",
  "Pipeline passes on clean main branch",
  "verify-pipeline-passes-on-clean-main.ps1",
  "git branch --show-current",
  "git status --porcelain",
  "mvn.cmd",
  "-DskipTests",
  "npm.cmd",
  "npm test",
  "npm run build",
  "docker",
  "test-docker-compose-config.ps1",
] as const;

export const CI_CD_DOCUMENTATION = {
  item: CI_CD_DOCUMENTATION_ITEM,
  statement: CI_CD_DOCUMENTATION_STATEMENT,
  path: CI_CD_DOC_PATH,
  evidenceTestClass: CI_CD_DOCUMENTATION_TEST_CLASS,
  requiredSections: [
    "Workflow file",
    "Triggers",
    "Jobs",
    "Release artifact generation",
    "Local parity",
    "Runtime evidence",
    "Security notes",
    "Maintenance checklist",
  ] as const,
} as const;

export const CI_CD_DOCUMENTATION_REQUIRED_MARKERS = [
  "708",
  "CI/CD documentation",
  "Workflow file",
  "Automated documentation evidence",
  "Triggers",
  "Jobs",
  "Release artifact generation",
  "Pipeline fails on intentionally broken test",
  "Pipeline passes on clean main branch",
  "Local parity",
  "Security notes",
  "Maintenance checklist",
  "CiCdDocumentationExpansionTests",
] as const;

/**
 * Pass-on-green contract (item 694): clean main must be able to produce a green workflow when the
 * suite is green. Configuration is verified statically (does not claim a remote green run).
 */
export const PIPELINE_PASSES_ON_CLEAN_MAIN = {
  item: PIPELINE_PASSES_ON_CLEAN_MAIN_ITEM,
  statement: PIPELINE_PASSES_ON_CLEAN_MAIN_STATEMENT,
  branch: CI_CLEAN_PASS_BRANCH,
  triggerEvents: ["push", "pull_request"] as const,
  requiredJobs: [
    "backend-build",
    "backend-test",
    "backend-integration-test",
    "frontend-install",
    "frontend-lint",
    "frontend-test",
    "frontend-build",
    "docker-backend",
    "docker-frontend",
    "docker-compose-validate",
    "production-config-validate",
  ] as const,
  forbidsPathFilters: true,
  forbidsJobIfSkips: true,
  badgeTracksMain: true,
} as const;

/** Required workflow markers documenting pass-on-green for main (item 694). */
export const PIPELINE_PASSES_ON_CLEAN_MAIN_REQUIRED_MARKERS = [
  "694",
  "pass-on-green",
  "push:",
  "- main",
  "backend-build:",
  "backend-test:",
  "frontend-test:",
  "frontend-build:",
  "production-config-validate:",
] as const;

/**
 * CI on pull request acceptance (item 698): PRs targeting protected/integration branches run CI.
 */
export const CI_RUNS_ON_PULL_REQUEST = {
  item: CI_RUNS_ON_PULL_REQUEST_ITEM,
  statement: CI_RUNS_ON_PULL_REQUEST_STATEMENT,
  event: "pull_request" as const,
  targetBranches: CI_TRIGGER_BRANCHES,
  requiresPathFilters: false,
  runsFullJobMatrix: true,
} as const;

/** Required workflow markers for pull_request CI (item 698). */
export const CI_RUNS_ON_PULL_REQUEST_REQUIRED_MARKERS = [
  "698",
  "pull_request:",
  "branches:",
  "- main",
  "- dev",
  "name: CI",
  "backend-test:",
  "frontend-test:",
] as const;

/**
 * CI on main branch acceptance (item 699): push to releasable main runs CI.
 */
export const CI_RUNS_ON_MAIN_BRANCH = {
  item: CI_RUNS_ON_MAIN_BRANCH_ITEM,
  statement: CI_RUNS_ON_MAIN_BRANCH_STATEMENT,
  event: "push" as const,
  branch: "main" as const,
  alsoRunsOnDev: true,
  requiresPathFilters: false,
  runsFullJobMatrix: true,
  badgeTracksMain: true,
} as const;

/** Required workflow markers for push-to-main CI (item 699). */
export const CI_RUNS_ON_MAIN_BRANCH_REQUIRED_MARKERS = [
  "699",
  "push:",
  "- main",
  "name: CI",
  "backend-test:",
  "frontend-test:",
] as const;

/**
 * Backend build passes acceptance (item 700): Maven package + JAR assert without soft-fail.
 */
export const BACKEND_BUILD_PASSES = {
  item: BACKEND_BUILD_PASSES_ITEM,
  statement: BACKEND_BUILD_PASSES_STATEMENT,
  jobId: "backend-build",
  jobName: "Backend build",
  packageCommand: "mvn -B -DskipTests package",
  assertsJar: true,
  softFailForbidden: true,
  relatedJobItem: 678,
} as const;

/** Required markers for backend build pass acceptance (item 700). */
export const BACKEND_BUILD_PASSES_REQUIRED_MARKERS = [
  "700",
  "backend-build:",
  "name: Backend build",
  "mvn -B -DskipTests package",
  "id: backend-build",
  "Assert backend JAR artifact was produced",
] as const;

/**
 * Backend tests pass acceptance (item 701): full Maven Surefire without soft-fail or skipTests.
 */
export const BACKEND_TESTS_PASS = {
  item: BACKEND_TESTS_PASS_ITEM,
  statement: BACKEND_TESTS_PASS_STATEMENT,
  jobId: "backend-test",
  jobName: "Backend test",
  testCommand: "mvn -B test",
  softFailForbidden: true,
  skipTestsForbidden: true,
  relatedJobItem: 679,
} as const;

/** Required markers for backend tests pass acceptance (item 701). */
export const BACKEND_TESTS_PASS_REQUIRED_MARKERS = [
  "701",
  "backend-test:",
  "name: Backend test",
  "mvn -B test",
  "id: backend-test",
  "Run backend tests (Maven test)",
] as const;

/** Required YAML markers for the primary CI workflow (677–687, 690–691). */
export const CI_WORKFLOW_REQUIRED_MARKERS = [
  "name: CI",
  "pull_request:",
  "push:",
  "backend-build:",
  "backend-test:",
  "backend-integration-test:",
  "frontend-install:",
  "frontend-lint:",
  "frontend-test:",
  "frontend-build:",
  "docker-backend:",
  "docker-frontend:",
  "docker-compose-validate:",
  "production-config-validate:",
  "actions/checkout@v4",
  "actions/setup-java@v4",
  "actions/setup-node@v4",
  "actions/upload-artifact@v4",
  "mvn -B -DskipTests package",
  "mvn -B test",
  "*IntegrationTests",
  "npm ci",
  "npm run lint",
  "npm test",
  "npm run build",
  "docker build -t bwc-backend:ci -f backend/Dockerfile backend",
  "docker build -t bwc-frontend:ci -f frontend/Dockerfile frontend",
  'docker compose -f "$compose_file" config',
  "config --format json",
  "application-prod.yml",
  "EnvironmentVariableValidator",
  "SecretPresenceValidator",
  "name: bwc-backend-jar",
  "name: bwc-frontend-dist",
  "contents: read",
] as const;

/** Required markers for the backend-build job block alone. */
export const BACKEND_BUILD_JOB_REQUIRED_MARKERS = [
  "backend-build:",
  "name: Backend build",
  "working-directory: backend",
  'java-version: "21"',
  "mvn -B -DskipTests package",
  "id: backend-build",
  "Assert backend JAR artifact was produced",
] as const;

/** Required markers for the backend-test job block alone. */
export const BACKEND_TEST_JOB_REQUIRED_MARKERS = [
  "backend-test:",
  "name: Backend test",
  "working-directory: backend",
  'java-version: "21"',
  "mvn -B test",
  "id: backend-test",
  "Run backend tests (Maven test)",
] as const;

/** Required markers for the backend-integration-test job block alone. */
export const BACKEND_INTEGRATION_TEST_JOB_REQUIRED_MARKERS = [
  "backend-integration-test:",
  "name: Backend integration test",
  "working-directory: backend",
  'java-version: "21"',
  "mvn -B test -Dtest='*IntegrationTests'",
  "id: backend-integration-test",
  "Run backend integration tests (Maven, *IntegrationTests)",
] as const;

/** Required markers for the frontend-install job block alone. */
export const FRONTEND_INSTALL_JOB_REQUIRED_MARKERS = [
  "frontend-install:",
  "name: Frontend install",
  "working-directory: frontend",
  'node-version: "22"',
  "npm ci",
  "id: frontend-install",
  "Install frontend dependencies (npm ci)",
  "Assert node_modules was installed",
] as const;

/** Required markers for the frontend-lint job block alone. */
export const FRONTEND_LINT_JOB_REQUIRED_MARKERS = [
  "frontend-lint:",
  "name: Frontend lint",
  "working-directory: frontend",
  'node-version: "22"',
  "npm ci",
  "npm run lint",
  "id: frontend-lint",
  "Lint frontend (npm run lint)",
] as const;

/** Required markers for the frontend-test job block alone. */
export const FRONTEND_TEST_JOB_REQUIRED_MARKERS = [
  "frontend-test:",
  "name: Frontend test",
  "working-directory: frontend",
  'node-version: "22"',
  "npm ci",
  "npm test",
  "id: frontend-test",
  "Run frontend unit tests (npm test)",
] as const;

/** Required markers for the frontend-build job block alone. */
export const FRONTEND_BUILD_JOB_REQUIRED_MARKERS = [
  "frontend-build:",
  "name: Frontend build",
  "working-directory: frontend",
  'node-version: "22"',
  "npm ci",
  "npm run build",
  "id: frontend-build",
  "Build frontend (npm run build)",
  "Assert frontend dist was produced",
] as const;

/** Required markers for the docker-backend job block alone. */
export const DOCKER_BACKEND_IMAGE_JOB_REQUIRED_MARKERS = [
  "docker-backend:",
  "name: Docker backend image",
  "docker build -t bwc-backend:ci -f backend/Dockerfile backend",
  "id: docker-backend",
  "Build backend Docker image",
  "Assert backend Docker image exists",
  "docker image inspect bwc-backend:ci",
] as const;

/** Required markers for the docker-frontend job block alone. */
export const DOCKER_FRONTEND_IMAGE_JOB_REQUIRED_MARKERS = [
  "docker-frontend:",
  "name: Docker frontend image",
  "docker build -t bwc-frontend:ci -f frontend/Dockerfile frontend",
  "id: docker-frontend",
  "Build frontend Docker image",
  "Assert frontend Docker image exists",
  "docker image inspect bwc-frontend:ci",
] as const;

/** Required markers for the docker-compose-validate job block alone. */
export const DOCKER_COMPOSE_VALIDATION_JOB_REQUIRED_MARKERS = [
  "docker-compose-validate:",
  "name: Docker Compose validation",
  'docker compose -f "$compose_file" config',
  "id: docker-compose-validate",
  "Validate Docker Compose configuration",
  "config --format json",
  "postgres:16-alpine",
  "bwc_local",
  "bwc_postgres_data",
] as const;

/** Required markers for the production-config-validate job block alone. */
export const PRODUCTION_CONFIG_VALIDATION_JOB_REQUIRED_MARKERS = [
  "production-config-validate:",
  "name: Production config validation",
  "id: production-config-validate",
  "Validate production configuration artifacts",
  "application-prod.yml",
  "EnvironmentVariableValidator",
  "SecretPresenceValidator",
  "MIN_JWT_SECRET_LENGTH = 32",
  "include-stacktrace: never",
] as const;

/** Required markers for release artifact generation (item 691) across the workflow. */
export const RELEASE_ARTIFACT_GENERATION_REQUIRED_MARKERS = [
  "actions/upload-artifact@v4",
  "Upload backend release JAR",
  "name: bwc-backend-jar",
  "backend/target/*.jar",
  "!backend/target/*.jar.original",
  "Upload frontend release dist",
  "name: bwc-frontend-dist",
  "path: frontend/dist",
  "if-no-files-found: error",
  "retention-days: 14",
] as const;

/**
 * True when workflow YAML text satisfies the item 677–687 / 690–691 contract.
 */
export function workflowYamlDefinesCiPipeline(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  return CI_WORKFLOW_REQUIRED_MARKERS.every((marker) => yaml.includes(marker));
}

/**
 * Index of the job that follows `backend-build` (prefer `backend-test`, else frontend jobs).
 */
function indexAfterBackendBuildJob(yaml: string): number {
  const testJob = yaml.indexOf("\n  backend-test:");
  if (testJob >= 0) {
    return testJob;
  }
  return indexOfFirstFrontendJob(yaml);
}

/**
 * Index of the job that follows full-suite `backend-test`.
 */
function indexAfterBackendTestJob(yaml: string): number {
  const integrationJob = yaml.indexOf("\n  backend-integration-test:");
  if (integrationJob >= 0) {
    return integrationJob;
  }
  return indexOfFirstFrontendJob(yaml);
}

/**
 * Index of the first frontend-related job after backend jobs.
 */
function indexOfFirstFrontendJob(yaml: string): number {
  const install = yaml.indexOf("\n  frontend-install:");
  if (install >= 0) {
    return install;
  }
  const build = yaml.indexOf("\n  frontend-build:");
  if (build >= 0) {
    return build;
  }
  return -1;
}

/**
 * True when workflow YAML defines the dedicated backend build job (item 678).
 */
export function workflowYamlDefinesBackendBuildJob(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (!BACKEND_BUILD_JOB_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))) {
    return false;
  }
  const buildJobIndex = yaml.indexOf("backend-build:");
  const nextJobIndex = indexAfterBackendBuildJob(yaml);
  if (buildJobIndex < 0 || nextJobIndex <= buildJobIndex) {
    return false;
  }
  const buildJobBlock = yaml.slice(buildJobIndex, nextJobIndex);
  return (
    buildJobBlock.includes("mvn -B -DskipTests package") &&
    !buildJobBlock.includes("mvn -B test")
  );
}

/**
 * True when workflow YAML defines the dedicated backend test job (item 679).
 */
export function workflowYamlDefinesBackendTestJob(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (!BACKEND_TEST_JOB_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))) {
    return false;
  }
  const testJobIndex = yaml.indexOf("backend-test:");
  const nextJobIndex = indexAfterBackendTestJob(yaml);
  if (testJobIndex < 0 || nextJobIndex <= testJobIndex) {
    return false;
  }
  const testJobBlock = yaml.slice(testJobIndex, nextJobIndex);
  return (
    testJobBlock.includes("mvn -B test") &&
    !testJobBlock.includes("mvn -B -DskipTests package") &&
    !testJobBlock.includes("*IntegrationTests")
  );
}

/**
 * True when workflow YAML defines the dedicated backend integration test job (item 680).
 */
export function workflowYamlDefinesBackendIntegrationTestJob(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (
    !BACKEND_INTEGRATION_TEST_JOB_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))
  ) {
    return false;
  }
  const integrationJobIndex = yaml.indexOf("backend-integration-test:");
  const nextJobIndex = indexOfFirstFrontendJob(yaml);
  if (integrationJobIndex < 0 || nextJobIndex <= integrationJobIndex) {
    return false;
  }
  const integrationBlock = yaml.slice(integrationJobIndex, nextJobIndex);
  return (
    integrationBlock.includes("mvn -B test -Dtest='*IntegrationTests'") &&
    !integrationBlock.includes("mvn -B -DskipTests package")
  );
}

/**
 * Index after the frontend-install job (prefer lint job).
 */
function indexAfterFrontendInstallJob(yaml: string): number {
  const lint = yaml.indexOf("\n  frontend-lint:");
  if (lint >= 0) {
    return lint;
  }
  return yaml.indexOf("\n  frontend-build:");
}

/**
 * True when workflow YAML defines the dedicated frontend install job (item 681).
 */
export function workflowYamlDefinesFrontendInstallJob(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (!FRONTEND_INSTALL_JOB_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))) {
    return false;
  }
  const installJobIndex = yaml.indexOf("frontend-install:");
  const nextJobIndex = indexAfterFrontendInstallJob(yaml);
  if (installJobIndex < 0 || nextJobIndex <= installJobIndex) {
    return false;
  }
  const installBlock = yaml.slice(installJobIndex, nextJobIndex);
  return (
    installBlock.includes("npm ci") &&
    !installBlock.includes("npm run lint") &&
    !installBlock.includes("npm test") &&
    !installBlock.includes("npm run build")
  );
}

/**
 * Index after the frontend-lint job (prefer test job).
 */
function indexAfterFrontendLintJob(yaml: string): number {
  const testJob = yaml.indexOf("\n  frontend-test:");
  if (testJob >= 0) {
    return testJob;
  }
  return yaml.indexOf("\n  frontend-build:");
}

/**
 * True when workflow YAML defines the dedicated frontend lint job (item 682).
 */
export function workflowYamlDefinesFrontendLintJob(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (!FRONTEND_LINT_JOB_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))) {
    return false;
  }
  const lintJobIndex = yaml.indexOf("frontend-lint:");
  const nextJobIndex = indexAfterFrontendLintJob(yaml);
  if (lintJobIndex < 0 || nextJobIndex <= lintJobIndex) {
    return false;
  }
  const lintBlock = yaml.slice(lintJobIndex, nextJobIndex);
  return (
    lintBlock.includes("npm run lint") &&
    !lintBlock.includes("npm test") &&
    !lintBlock.includes("npm run build")
  );
}

/**
 * Index after the frontend-test job (prefer build job).
 */
function indexAfterFrontendTestJob(yaml: string): number {
  return yaml.indexOf("\n  frontend-build:");
}

/**
 * True when workflow YAML defines the dedicated frontend test job (item 683).
 */
export function workflowYamlDefinesFrontendTestJob(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (!FRONTEND_TEST_JOB_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))) {
    return false;
  }
  const testJobIndex = yaml.indexOf("frontend-test:");
  const nextJobIndex = indexAfterFrontendTestJob(yaml);
  if (testJobIndex < 0 || nextJobIndex <= testJobIndex) {
    return false;
  }
  const testBlock = yaml.slice(testJobIndex, nextJobIndex);
  return (
    testBlock.includes("npm test") &&
    !testBlock.includes("npm run lint") &&
    !testBlock.includes("npm run build")
  );
}

/**
 * True when workflow YAML defines the dedicated frontend build job (item 684).
 */
export function workflowYamlDefinesFrontendBuildJob(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (!FRONTEND_BUILD_JOB_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))) {
    return false;
  }
  const buildJobIndex = yaml.indexOf("frontend-build:");
  if (buildJobIndex < 0) {
    return false;
  }
  const nextJobIndex = yaml.indexOf("\n  docker-backend:");
  const buildBlock =
    nextJobIndex > buildJobIndex
      ? yaml.slice(buildJobIndex, nextJobIndex)
      : yaml.slice(buildJobIndex);
  return (
    buildBlock.includes("npm run build") &&
    buildBlock.includes("Assert frontend dist was produced") &&
    !buildBlock.includes("npm run lint") &&
    !buildBlock.includes("npm test")
  );
}

/**
 * True when workflow YAML defines the Docker backend image job (item 685).
 */
export function workflowYamlDefinesDockerBackendImageJob(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (!DOCKER_BACKEND_IMAGE_JOB_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))) {
    return false;
  }
  const jobIndex = yaml.indexOf("docker-backend:");
  if (jobIndex < 0) {
    return false;
  }
  const nextJobIndex = yaml.indexOf("\n  docker-frontend:");
  const jobBlock =
    nextJobIndex > jobIndex ? yaml.slice(jobIndex, nextJobIndex) : yaml.slice(jobIndex);
  return (
    jobBlock.includes("docker build -t bwc-backend:ci -f backend/Dockerfile backend") &&
    !jobBlock.includes("docker push")
  );
}

/**
 * True when workflow YAML defines the Docker frontend image job (item 686).
 */
export function workflowYamlDefinesDockerFrontendImageJob(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (!DOCKER_FRONTEND_IMAGE_JOB_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))) {
    return false;
  }
  const jobIndex = yaml.indexOf("docker-frontend:");
  if (jobIndex < 0) {
    return false;
  }
  const nextJobIndex = yaml.indexOf("\n  docker-compose-validate:");
  const jobBlock =
    nextJobIndex > jobIndex ? yaml.slice(jobIndex, nextJobIndex) : yaml.slice(jobIndex);
  return (
    jobBlock.includes("docker build -t bwc-frontend:ci -f frontend/Dockerfile frontend") &&
    !jobBlock.includes("docker push")
  );
}

/**
 * True when workflow YAML defines the Docker Compose validation job (item 687).
 */
export function workflowYamlDefinesDockerComposeValidationJob(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (
    !DOCKER_COMPOSE_VALIDATION_JOB_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))
  ) {
    return false;
  }
  const jobIndex = yaml.indexOf("docker-compose-validate:");
  if (jobIndex < 0) {
    return false;
  }
  const nextJobIndex = yaml.indexOf("\n  production-config-validate:");
  const jobBlock =
    nextJobIndex > jobIndex ? yaml.slice(jobIndex, nextJobIndex) : yaml.slice(jobIndex);
  return (
    jobBlock.includes('docker compose -f "$compose_file" config') &&
    jobBlock.includes("config --format json") &&
    !jobBlock.includes("docker compose up") &&
    !jobBlock.includes("docker push")
  );
}

/**
 * True when workflow YAML defines the production config validation job (item 690).
 */
export function workflowYamlDefinesProductionConfigValidationJob(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (
    !PRODUCTION_CONFIG_VALIDATION_JOB_REQUIRED_MARKERS.every((marker) =>
      yaml.includes(marker),
    )
  ) {
    return false;
  }
  const jobIndex = yaml.indexOf("production-config-validate:");
  if (jobIndex < 0) {
    return false;
  }
  const jobBlock = yaml.slice(jobIndex);
  return (
    jobBlock.includes("application-prod.yml") &&
    jobBlock.includes("SecretPresenceValidator") &&
    !jobBlock.includes("mvn spring-boot:run") &&
    !jobBlock.includes("docker push")
  );
}

/**
 * True when root README markdown includes the GitHub Actions CI status badge (item 692).
 */
export function readmeDefinesCiBadge(readme: string): boolean {
  if (readme == null || readme.trim() === "") {
    return false;
  }
  if (!CI_BADGE_REQUIRED_MARKERS.every((marker) => readme.includes(marker))) {
    return false;
  }
  return (
    readme.includes(CI_BADGE.imageUrl) &&
    readme.includes(CI_BADGE.linkUrl) &&
    readme.includes("[![CI](")
  );
}

function jobBlockBetween(yaml: string, jobKey: string, nextJobKey: string): string | null {
  const start = yaml.indexOf(jobKey);
  const end = yaml.indexOf(nextJobKey);
  if (start < 0 || end <= start) {
    return null;
  }
  return yaml.slice(start, end);
}

function jobBlockFailsOnRed(block: string, requiredCommand: string): boolean {
  if (!block.includes(requiredCommand)) {
    return false;
  }
  if (block.includes(PIPELINE_FAILS_WHEN_TESTS_FAIL.forbiddenSoftFail)) {
    return false;
  }
  if (block.includes("continue-on-error:true")) {
    return false;
  }
  // Soft-fail patterns on the test command line.
  if (block.includes(`${requiredCommand} || true`)) {
    return false;
  }
  return true;
}

/**
 * True when quality-gate jobs are configured to fail the pipeline when tests/lint fail (item 693).
 */
export function workflowYamlDefinesPipelineFailsWhenTestsFail(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (
    !PIPELINE_FAILS_WHEN_TESTS_FAIL_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))
  ) {
    return false;
  }

  const backendTest = jobBlockBetween(yaml, "backend-test:", "backend-integration-test:");
  const backendIt = jobBlockBetween(yaml, "backend-integration-test:", "frontend-install:");
  const frontendLint = jobBlockBetween(yaml, "frontend-lint:", "frontend-test:");
  const frontendTest = jobBlockBetween(yaml, "frontend-test:", "frontend-build:");

  if (
    backendTest == null ||
    backendIt == null ||
    frontendLint == null ||
    frontendTest == null
  ) {
    return false;
  }

  return (
    jobBlockFailsOnRed(backendTest, PIPELINE_FAILS_WHEN_TESTS_FAIL.backendTestCommand) &&
    jobBlockFailsOnRed(
      backendIt,
      PIPELINE_FAILS_WHEN_TESTS_FAIL.backendIntegrationTestCommand,
    ) &&
    jobBlockFailsOnRed(frontendLint, PIPELINE_FAILS_WHEN_TESTS_FAIL.frontendLintCommand) &&
    jobBlockFailsOnRed(frontendTest, PIPELINE_FAILS_WHEN_TESTS_FAIL.frontendTestCommand)
  );
}

export function scriptDefinesPipelineFailsOnIntentionallyBrokenTest(script: string): boolean {
  if (script == null || script.trim() === "") {
    return false;
  }
  return PIPELINE_FAILS_ON_INTENTIONALLY_BROKEN_TEST_REQUIRED_MARKERS.every((marker) =>
    script.includes(marker),
  );
}

export function scriptDefinesPipelinePassesOnCleanMainRuntime(script: string): boolean {
  if (script == null || script.trim() === "") {
    return false;
  }
  return PIPELINE_PASSES_ON_CLEAN_MAIN_RUNTIME_REQUIRED_MARKERS.every((marker) =>
    script.includes(marker),
  );
}

export function markdownDefinesCiCdDocumentation(markdown: string): boolean {
  if (markdown == null || markdown.trim() === "") {
    return false;
  }
  return CI_CD_DOCUMENTATION_REQUIRED_MARKERS.every((marker) => markdown.includes(marker));
}

/**
 * True when the CI workflow runs on pull requests targeting main/dev (item 698).
 */
export function workflowYamlDefinesCiRunsOnPullRequest(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (
    !CI_RUNS_ON_PULL_REQUEST_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))
  ) {
    return false;
  }
  const prIndex = yaml.indexOf("pull_request:");
  if (prIndex < 0) {
    return false;
  }
  const concurrencyIndex = yaml.indexOf("\nconcurrency:", prIndex);
  const prBlock =
    concurrencyIndex > prIndex ? yaml.slice(prIndex, concurrencyIndex) : yaml.slice(prIndex);
  if (!prBlock.includes("- main") || !prBlock.includes("- dev")) {
    return false;
  }
  if (prBlock.includes("paths:") || prBlock.includes("paths-ignore:")) {
    return false;
  }
  return true;
}

/**
 * True when backend-build is configured so a successful package means the job passes (item 700).
 */
export function workflowYamlDefinesBackendBuildPasses(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (!BACKEND_BUILD_PASSES_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))) {
    return false;
  }
  const buildJobIndex = yaml.indexOf("backend-build:");
  const nextJobIndex = yaml.indexOf("\n  backend-test:");
  if (buildJobIndex < 0 || nextJobIndex <= buildJobIndex) {
    return false;
  }
  const block = yaml.slice(buildJobIndex, nextJobIndex);
  if (!block.includes("mvn -B -DskipTests package")) {
    return false;
  }
  if (!block.includes("Assert backend JAR artifact was produced")) {
    return false;
  }
  if (block.includes("continue-on-error: true") || block.includes("continue-on-error:true")) {
    return false;
  }
  if (block.includes("mvn -B -DskipTests package || true")) {
    return false;
  }
  // Package-only job — full suite is item 701 / 679.
  if (block.includes("mvn -B test")) {
    return false;
  }
  return true;
}

/**
 * True when backend-test is configured so a successful Surefire suite means the job passes (item 701).
 */
export function workflowYamlDefinesBackendTestsPass(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (!BACKEND_TESTS_PASS_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))) {
    return false;
  }
  const testJobIndex = yaml.indexOf("backend-test:");
  const nextJobIndex = yaml.indexOf("\n  backend-integration-test:");
  if (testJobIndex < 0 || nextJobIndex <= testJobIndex) {
    return false;
  }
  const block = yaml.slice(testJobIndex, nextJobIndex);
  if (!block.includes("mvn -B test")) {
    return false;
  }
  if (block.includes("continue-on-error: true") || block.includes("continue-on-error:true")) {
    return false;
  }
  if (block.includes("mvn -B test || true")) {
    return false;
  }
  if (block.includes("-DskipTests") || block.includes("mvn -B -DskipTests package")) {
    return false;
  }
  // Full suite is unfiltered; integration-only filter is item 680.
  if (block.includes("*IntegrationTests")) {
    return false;
  }
  return true;
}

/**
 * True when the CI workflow runs on push to main (item 699).
 */
export function workflowYamlDefinesCiRunsOnMainBranch(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (
    !CI_RUNS_ON_MAIN_BRANCH_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))
  ) {
    return false;
  }
  const pushIndex = yaml.indexOf("push:");
  if (pushIndex < 0) {
    return false;
  }
  let prIndex = yaml.indexOf("\npull_request:", pushIndex);
  if (prIndex < 0) {
    prIndex = yaml.indexOf("\n  pull_request:", pushIndex);
  }
  if (prIndex <= pushIndex) {
    return false;
  }
  const pushBlock = yaml.slice(pushIndex, prIndex);
  if (!pushBlock.includes("- main")) {
    return false;
  }
  if (pushBlock.includes("paths:") || pushBlock.includes("paths-ignore:")) {
    return false;
  }
  return true;
}

/**
 * True when CI is configured so a clean main branch can pass the full pipeline (item 694).
 */
export function workflowYamlDefinesPipelinePassesOnCleanMain(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (
    !PIPELINE_PASSES_ON_CLEAN_MAIN_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))
  ) {
    return false;
  }
  if (yaml.includes("paths:") || yaml.includes("paths-ignore:")) {
    return false;
  }
  for (const jobId of PIPELINE_PASSES_ON_CLEAN_MAIN.requiredJobs) {
    if (!yaml.includes(`${jobId}:`)) {
      return false;
    }
  }
  // Forbid job/step if: that would skip work on main / based on event.
  const hasSkipIf = yaml.split("\n").some((line) => {
    const trimmed = line.trim();
    return (
      trimmed.startsWith("if:") &&
      (trimmed.includes("github.ref") ||
        trimmed.includes("false") ||
        trimmed.includes("github.event"))
    );
  });
  return !hasSkipIf;
}

/**
 * True when workflow YAML generates downloadable release artifacts (item 691).
 */
export function workflowYamlDefinesReleaseArtifactGeneration(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (
    !RELEASE_ARTIFACT_GENERATION_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))
  ) {
    return false;
  }

  const backendJobIndex = yaml.indexOf("backend-build:");
  const backendNext = yaml.indexOf("\n  backend-test:");
  if (backendJobIndex < 0 || backendNext <= backendJobIndex) {
    return false;
  }
  const backendBlock = yaml.slice(backendJobIndex, backendNext);
  if (
    !backendBlock.includes("actions/upload-artifact@v4") ||
    !backendBlock.includes("bwc-backend-jar") ||
    backendBlock.includes("docker push")
  ) {
    return false;
  }

  const frontendJobIndex = yaml.indexOf("frontend-build:");
  const frontendNext = yaml.indexOf("\n  docker-backend:");
  if (frontendJobIndex < 0 || frontendNext <= frontendJobIndex) {
    return false;
  }
  const frontendBlock = yaml.slice(frontendJobIndex, frontendNext);
  return (
    frontendBlock.includes("actions/upload-artifact@v4") &&
    frontendBlock.includes("bwc-frontend-dist") &&
    frontendBlock.includes("path: frontend/dist") &&
    !frontendBlock.includes("docker push")
  );
}

/**
 * True when a branch is in the CI trigger set.
 */
export function isCiTriggerBranch(branch: string | null | undefined): boolean {
  if (branch == null || branch === "") {
    return false;
  }
  return (CI_TRIGGER_BRANCHES as readonly string[]).includes(branch);
}

/**
 * Maps a job id to the related Sprint 17 backlog band (documentation aid).
 * Build jobs also host item **691** release artifact uploads.
 */
export function relatedBacklogBandForJob(jobId: CiJobId): string {
  if (jobId === "backend-build") {
    return "678+691+700";
  }
  if (jobId === "backend-test") {
    return "679+701";
  }
  if (jobId === "backend-integration-test") {
    return "680";
  }
  if (jobId === "frontend-install") {
    return "681";
  }
  if (jobId === "frontend-lint") {
    return "682";
  }
  if (jobId === "frontend-test") {
    return "683";
  }
  if (jobId === "frontend-build") {
    return "684+691";
  }
  if (jobId === "docker-backend") {
    return "685";
  }
  if (jobId === "docker-frontend") {
    return "686";
  }
  if (jobId === "docker-compose-validate") {
    return "687";
  }
  return "690";
}
