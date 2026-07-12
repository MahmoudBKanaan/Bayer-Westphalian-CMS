import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_DOCUMENTATION_TEST_CLASS,
  BRANCH_PROTECTION_DOC_PATH,
  BRANCH_PROTECTION_DOC_REQUIRED_MARKERS,
  BRANCH_PROTECTION_RECOMMENDATION,
  BRANCH_PROTECTION_RECOMMENDATION_ITEM,
  BRANCH_PROTECTION_RECOMMENDATION_STATEMENT,
  PROTECTED_RELEASE_BRANCH,
  RECOMMENDED_REQUIRED_STATUS_CHECKS,
  branchProtectionDocDefinesRequiredMarkers,
  branchProtectionDocForbidsForcePushOnMain,
  branchProtectionDocListsRequiredChecks,
} from "@/features/ops/branchProtectionRecommendation";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("branchProtectionRecommendation (item 695)", () => {
  it("locks the branch protection recommendation identity", () => {
    expect(BRANCH_PROTECTION_RECOMMENDATION_ITEM).toBe(695);
    expect(BRANCH_PROTECTION_RECOMMENDATION_STATEMENT).toBe(
      "Add branch protection recommendation",
    );
    expect(BRANCH_PROTECTION_DOC_PATH).toBe("docs/deployment/branch-protection.md");
    expect(PROTECTED_RELEASE_BRANCH).toBe("main");
    expect(BRANCH_PROTECTION_RECOMMENDATION.requirePullRequest).toBe(true);
    expect(BRANCH_PROTECTION_RECOMMENDATION.requireStatusChecks).toBe(true);
    expect(BRANCH_PROTECTION_RECOMMENDATION.allowForcePushes).toBe(false);
    expect(BRANCH_PROTECTION_RECOMMENDATION.allowDeletions).toBe(false);
    expect(BRANCH_PROTECTION_RECOMMENDATION.enforcesViaGithubUiOnly).toBe(true);
    expect(BACKEND_DOCUMENTATION_TEST_CLASS).toContain(
      "BranchProtectionRecommendationDocumentationTests",
    );
    expect(RECOMMENDED_REQUIRED_STATUS_CHECKS).toContain("Backend test");
    expect(RECOMMENDED_REQUIRED_STATUS_CHECKS).toContain("Frontend test");
    expect(RECOMMENDED_REQUIRED_STATUS_CHECKS).toContain("Production config validation");
    expect(BRANCH_PROTECTION_DOC_REQUIRED_MARKERS).toContain("695");
  });

  it("requires branch-protection.md, cross-links, and CI check name coverage", () => {
    expect(existsSync(path.join(repoRoot, BRANCH_PROTECTION_DOC_PATH))).toBe(true);
    expect(existsSync(path.join(repoRoot, "docs/deployment/ci-cd.md"))).toBe(true);
    expect(existsSync(path.join(repoRoot, ".github/workflows/ci.yml"))).toBe(true);

    const doc = readRepoFile(BRANCH_PROTECTION_DOC_PATH);
    expect(branchProtectionDocDefinesRequiredMarkers(doc)).toBe(true);
    expect(branchProtectionDocListsRequiredChecks(doc)).toBe(true);
    expect(branchProtectionDocForbidsForcePushOnMain(doc)).toBe(true);

    const ciCd = readRepoFile("docs/deployment/ci-cd.md");
    expect(ciCd).toContain("695");
    expect(ciCd).toContain("branch-protection.md");
    expect(ciCd).toContain("BranchProtectionRecommendationDocumentationTests");

    const index = readRepoFile("docs/README.md");
    expect(index).toContain("deployment/branch-protection.md");
    expect(index).toContain("695");

    const workflow = readRepoFile(".github/workflows/ci.yml");
    expect(workflow).toContain("name: Backend test");
    expect(workflow).toContain("name: Frontend test");
    expect(workflow).toContain("name: Production config validation");
    expect(workflow).toContain("- main");
    expect(workflow).toContain("pull_request:");

    const githubReadme = readRepoFile(".github/README.md");
    expect(githubReadme).toContain("695");
    expect(githubReadme).toContain("BranchProtectionRecommendationDocumentationTests");
  });
});
