import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_DOCUMENTATION_TEST_CLASS,
  DEPLOYMENT_PLACEHOLDER_ENVIRONMENTS,
  DEPLOYMENT_PLACEHOLDER_FORBIDDEN_MARKERS,
  DEPLOYMENT_WORKFLOW_NAME,
  DEPLOYMENT_WORKFLOW_PATH,
  DEPLOYMENT_WORKFLOW_PLACEHOLDER,
  DEPLOYMENT_WORKFLOW_PLACEHOLDER_ITEM,
  DEPLOYMENT_WORKFLOW_PLACEHOLDER_STATEMENT,
  DEPLOYMENT_WORKFLOW_REQUIRED_MARKERS,
  deploymentPlaceholderIsNonDeploying,
  workflowYamlDefinesDeploymentPlaceholder,
} from "@/features/ops/deploymentWorkflowPlaceholder";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("deploymentWorkflowPlaceholder (item 697)", () => {
  it("locks the deployment workflow placeholder identity", () => {
    expect(DEPLOYMENT_WORKFLOW_PLACEHOLDER_ITEM).toBe(697);
    expect(DEPLOYMENT_WORKFLOW_PLACEHOLDER_STATEMENT).toBe(
      "Add deployment workflow placeholder",
    );
    expect(DEPLOYMENT_WORKFLOW_PATH).toBe(".github/workflows/deploy-placeholder.yml");
    expect(DEPLOYMENT_WORKFLOW_NAME).toBe("Deploy (placeholder)");
    expect(DEPLOYMENT_WORKFLOW_PLACEHOLDER.trigger).toBe("workflow_dispatch");
    expect(DEPLOYMENT_WORKFLOW_PLACEHOLDER.performsLiveDeploy).toBe(false);
    expect(DEPLOYMENT_WORKFLOW_PLACEHOLDER.jobId).toBe("deploy-placeholder");
    expect(DEPLOYMENT_PLACEHOLDER_ENVIRONMENTS).toContain("staging-placeholder");
    expect(DEPLOYMENT_PLACEHOLDER_ENVIRONMENTS).toContain("production-placeholder");
    expect(BACKEND_DOCUMENTATION_TEST_CLASS).toContain(
      "DeploymentWorkflowPlaceholderDocumentationTests",
    );
    expect(DEPLOYMENT_WORKFLOW_REQUIRED_MARKERS).toContain("workflow_dispatch:");
    expect(DEPLOYMENT_PLACEHOLDER_FORBIDDEN_MARKERS).toContain("docker push");
  });

  it("requires deploy-placeholder.yml, docs, and non-deploying contract", () => {
    expect(existsSync(path.join(repoRoot, DEPLOYMENT_WORKFLOW_PATH))).toBe(true);
    expect(existsSync(path.join(repoRoot, "docs/deployment/ci-cd.md"))).toBe(true);

    const yaml = readRepoFile(DEPLOYMENT_WORKFLOW_PATH);
    expect(yaml).toContain("697");
    expect(workflowYamlDefinesDeploymentPlaceholder(yaml)).toBe(true);
    expect(deploymentPlaceholderIsNonDeploying(yaml)).toBe(true);
    expect(yaml).toContain("does NOT deploy");

    const ciCd = readRepoFile("docs/deployment/ci-cd.md");
    expect(ciCd).toContain("697");
    expect(ciCd).toContain("deploy-placeholder.yml");
    expect(ciCd).toContain("DeploymentWorkflowPlaceholderDocumentationTests");

    const githubReadme = readRepoFile(".github/README.md");
    expect(githubReadme).toContain("697");
    expect(githubReadme).toContain("deploy-placeholder.yml");
    expect(githubReadme).toContain("DeploymentWorkflowPlaceholderDocumentationTests");

    const index = readRepoFile("docs/README.md");
    expect(index).toContain("697");

    const ci = readRepoFile(".github/workflows/ci.yml");
    expect(ci).toContain("name: CI");
    expect(ci).toContain("697");
    expect(ci).not.toContain("name: Deploy (placeholder)");
  });
});
