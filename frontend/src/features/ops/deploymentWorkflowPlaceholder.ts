/**
 * Sprint 17 item **697**: Add deployment workflow placeholder.
 *
 * KB: CI packages the app; production deploy is prepared later (Sprint 18). Catalog locks the
 * manual `workflow_dispatch` placeholder workflow (no live deploy).
 */

export const DEPLOYMENT_WORKFLOW_PLACEHOLDER_ITEM = 697;

export const DEPLOYMENT_WORKFLOW_PLACEHOLDER_STATEMENT =
  "Add deployment workflow placeholder";

export const DEPLOYMENT_WORKFLOW_PATH = ".github/workflows/deploy-placeholder.yml";

export const DEPLOYMENT_WORKFLOW_NAME = "Deploy (placeholder)";

export const CI_CD_DOC_PATH = "docs/deployment/ci-cd.md";

export const BACKEND_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.DeploymentWorkflowPlaceholderDocumentationTests";

export const DEPLOYMENT_PLACEHOLDER_JOB_ID = "deploy-placeholder";

export const DEPLOYMENT_PLACEHOLDER_JOB_NAME = "Deployment placeholder";

/** Logical environments accepted by the placeholder inputs (not real hosts). */
export const DEPLOYMENT_PLACEHOLDER_ENVIRONMENTS = [
  "staging-placeholder",
  "production-placeholder",
] as const;

/** Required markers in deploy-placeholder.yml. */
export const DEPLOYMENT_WORKFLOW_REQUIRED_MARKERS = [
  "697",
  "name: Deploy (placeholder)",
  "workflow_dispatch:",
  "deploy-placeholder:",
  "name: Deployment placeholder",
  "id: deploy-placeholder",
  "PLACEHOLDER",
  "contents: read",
  "actions/checkout@v4",
  "Sprint 18",
  "staging-placeholder",
  "production-placeholder",
] as const;

/** Commands / patterns that must not appear as live deploy actions in the placeholder. */
export const DEPLOYMENT_PLACEHOLDER_FORBIDDEN_MARKERS = [
  "docker push",
  "kubectl ",
  "helm ",
  "ssh ",
  "JWT_SECRET:",
  "DB_PASSWORD:",
  "secrets.",
] as const;

export const DEPLOYMENT_WORKFLOW_PLACEHOLDER = {
  item: DEPLOYMENT_WORKFLOW_PLACEHOLDER_ITEM,
  statement: DEPLOYMENT_WORKFLOW_PLACEHOLDER_STATEMENT,
  path: DEPLOYMENT_WORKFLOW_PATH,
  name: DEPLOYMENT_WORKFLOW_NAME,
  jobId: DEPLOYMENT_PLACEHOLDER_JOB_ID,
  jobName: DEPLOYMENT_PLACEHOLDER_JOB_NAME,
  trigger: "workflow_dispatch" as const,
  performsLiveDeploy: false,
  environments: DEPLOYMENT_PLACEHOLDER_ENVIRONMENTS,
  permissions: "contents: read",
} as const;

/**
 * True when deploy-placeholder.yml text satisfies the item 697 placeholder contract.
 */
export function workflowYamlDefinesDeploymentPlaceholder(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  if (
    !DEPLOYMENT_WORKFLOW_REQUIRED_MARKERS.every((marker) => yaml.includes(marker))
  ) {
    return false;
  }
  if (DEPLOYMENT_PLACEHOLDER_FORBIDDEN_MARKERS.some((marker) => yaml.includes(marker))) {
    return false;
  }
  // Manual only — not on every PR.
  if (!yaml.includes("workflow_dispatch:")) {
    return false;
  }
  if (yaml.includes("pull_request:")) {
    return false;
  }
  return true;
}

/**
 * True when the workflow is documented as a non-deploying placeholder.
 */
export function deploymentPlaceholderIsNonDeploying(yaml: string): boolean {
  if (yaml == null || yaml.trim() === "") {
    return false;
  }
  const lower = yaml.toLowerCase();
  return (
    lower.includes("placeholder") &&
    lower.includes("does not deploy") &&
    workflowYamlDefinesDeploymentPlaceholder(yaml)
  );
}
