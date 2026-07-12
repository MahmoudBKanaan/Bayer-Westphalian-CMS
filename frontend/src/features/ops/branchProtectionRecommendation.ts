/**
 * Sprint 17 item **695**: Add branch protection recommendation.
 *
 * KB: releasable `main` should require green CI before merge. Catalog locks the ops guide path and
 * recommended rule markers (GitHub UI settings are not applied by these unit tests).
 */

export const BRANCH_PROTECTION_RECOMMENDATION_ITEM = 695;

export const BRANCH_PROTECTION_RECOMMENDATION_STATEMENT =
  "Add branch protection recommendation";

export const BRANCH_PROTECTION_DOC_PATH = "docs/deployment/branch-protection.md";

export const CI_CD_DOC_PATH = "docs/deployment/ci-cd.md";

export const BACKEND_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.BranchProtectionRecommendationDocumentationTests";

/** Branch that must be protected as the releasable line. */
export const PROTECTED_RELEASE_BRANCH = "main";

/** Optional lighter-protection integration branch. */
export const OPTIONAL_DEV_BRANCH = "dev";

/**
 * GitHub Actions job display names to require as status checks (must match `name:` in ci.yml).
 */
export const RECOMMENDED_REQUIRED_STATUS_CHECKS = [
  "Backend build",
  "Backend test",
  "Backend integration test",
  "Frontend install",
  "Frontend lint",
  "Frontend test",
  "Frontend build",
  "Docker backend image",
  "Docker frontend image",
  "Docker Compose validation",
  "Production config validation",
] as const;

/** Core recommendation markers required in branch-protection.md. */
export const BRANCH_PROTECTION_DOC_REQUIRED_MARKERS = [
  "695",
  "branch protection",
  "main",
  "Require status checks",
  "Allow force pushes",
  "Off",
  "pull request",
  "Backend test",
  "Frontend test",
  "Production config validation",
  "solo",
  "BranchProtectionRecommendationDocumentationTests",
  "ci.yml",
] as const;

export const BRANCH_PROTECTION_RECOMMENDATION = {
  item: BRANCH_PROTECTION_RECOMMENDATION_ITEM,
  statement: BRANCH_PROTECTION_RECOMMENDATION_STATEMENT,
  docPath: BRANCH_PROTECTION_DOC_PATH,
  protectedBranch: PROTECTED_RELEASE_BRANCH,
  optionalDevBranch: OPTIONAL_DEV_BRANCH,
  requirePullRequest: true,
  requireStatusChecks: true,
  allowForcePushes: false,
  allowDeletions: false,
  requiredStatusChecks: RECOMMENDED_REQUIRED_STATUS_CHECKS,
  enforcesViaGithubUiOnly: true,
} as const;

/**
 * True when branch protection markdown includes all required recommendation markers.
 */
export function branchProtectionDocDefinesRequiredMarkers(markdown: string): boolean {
  if (markdown == null || markdown.trim() === "") {
    return false;
  }
  return BRANCH_PROTECTION_DOC_REQUIRED_MARKERS.every((marker) => {
    if (marker === "branch protection" || marker === "solo" || marker === "pull request") {
      return markdown.toLowerCase().includes(marker.toLowerCase());
    }
    return markdown.includes(marker);
  });
}

/**
 * True when the guide lists every recommended required status check name.
 */
export function branchProtectionDocListsRequiredChecks(
  markdown: string,
  checks: readonly string[] = RECOMMENDED_REQUIRED_STATUS_CHECKS,
): boolean {
  if (markdown == null || markdown.trim() === "") {
    return false;
  }
  return checks.every((name) => markdown.includes(name));
}

/**
 * True when the recommendation forbids force push on the protected branch.
 */
export function branchProtectionDocForbidsForcePushOnMain(markdown: string): boolean {
  if (markdown == null || markdown.trim() === "") {
    return false;
  }
  const lower = markdown.toLowerCase();
  return (
    lower.includes("force push") &&
    lower.includes("off") &&
    lower.includes("main")
  );
}
