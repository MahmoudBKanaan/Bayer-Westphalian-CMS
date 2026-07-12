import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_DOCUMENTATION_TEST_CLASS,
  KB_RELEASE_TAGS,
  MAIN_CI_RELEASE_GATE_ITEM,
  MAIN_CI_RELEASE_GATE_STATEMENT,
  RELEASE_TAGGING_DOC_PATH,
  RELEASE_TAGGING_DOC_REQUIRED_MARKERS,
  RELEASE_TAGGING_GUIDE_ITEM,
  RELEASE_TAGGING_GUIDE_REQUIRED_MARKERS,
  RELEASE_TAGGING_GUIDE_STATEMENT,
  RELEASE_TAGGING_GUIDE_TEST_CLASS,
  RELEASE_TAGGING_PROCESS,
  RELEASE_TAGGING_PROCESS_ITEM,
  RELEASE_TAGGING_PROCESS_STATEMENT,
  RELEASE_TAG_BRANCH,
  isKbStyleReleaseTag,
  isMainCommitReleasable,
  releaseTaggingDocDefinesGuideMarkers,
  releaseTaggingDocDefinesRequiredMarkers,
  releaseTaggingDocListsKbTags,
  releaseTaggingDocRequiresGreenMain,
} from "@/features/ops/releaseTaggingProcess";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("releaseTaggingProcess (items 696, 711, 714)", () => {
  it("locks the release tagging process identity", () => {
    expect(RELEASE_TAGGING_PROCESS_ITEM).toBe(696);
    expect(RELEASE_TAGGING_PROCESS_STATEMENT).toBe("Add release tagging process");
    expect(RELEASE_TAGGING_GUIDE_ITEM).toBe(711);
    expect(RELEASE_TAGGING_GUIDE_STATEMENT).toBe("Release tagging guide");
    expect(MAIN_CI_RELEASE_GATE_ITEM).toBe(714);
    expect(MAIN_CI_RELEASE_GATE_STATEMENT).toContain("unless CI passes");
    expect(RELEASE_TAGGING_DOC_PATH).toBe("docs/deployment/release-tagging.md");
    expect(RELEASE_TAG_BRANCH).toBe("main");
    expect(RELEASE_TAGGING_PROCESS.preferAnnotatedTags).toBe(true);
    expect(RELEASE_TAGGING_PROCESS.requireGreenCi).toBe(true);
    expect(RELEASE_TAGGING_PROCESS.forbidForceMovingPublishedTags).toBe(true);
    expect(RELEASE_TAGGING_PROCESS.tagOnlyFromMain).toBe(true);
    expect(KB_RELEASE_TAGS).toContain("v0.1");
    expect(KB_RELEASE_TAGS).toContain("v0.9");
    expect(KB_RELEASE_TAGS).toContain("v1.0");
    expect(KB_RELEASE_TAGS).toHaveLength(10);
    expect(BACKEND_DOCUMENTATION_TEST_CLASS).toContain(
      "ReleaseTaggingProcessDocumentationTests",
    );
    expect(RELEASE_TAGGING_GUIDE_TEST_CLASS).toContain(
      "ReleaseTaggingGuideDocumentationTests",
    );
    expect(RELEASE_TAGGING_DOC_REQUIRED_MARKERS).toContain("696");
    expect(RELEASE_TAGGING_GUIDE_REQUIRED_MARKERS).toContain("Evidence capture");
    expect(isKbStyleReleaseTag("v0.9")).toBe(true);
    expect(isKbStyleReleaseTag("v1.0")).toBe(true);
    expect(isKbStyleReleaseTag("release-1")).toBe(false);
  });

  it("considers only the exact successful main CI commit releasable", () => {
    const greenMain = {
      branch: "main",
      headSha: "abc123",
      expectedSha: "abc123",
      status: "completed",
      conclusion: "success",
    };

    expect(isMainCommitReleasable(greenMain)).toBe(true);
    expect(isMainCommitReleasable({ ...greenMain, branch: "dev" })).toBe(false);
    expect(isMainCommitReleasable({ ...greenMain, headSha: "older" })).toBe(false);
    expect(isMainCommitReleasable({ ...greenMain, status: "in_progress" })).toBe(false);
    expect(isMainCommitReleasable({ ...greenMain, conclusion: "failure" })).toBe(false);
    expect(isMainCommitReleasable({ ...greenMain, conclusion: null })).toBe(false);
  });

  it("requires release-tagging.md, KB tags, and cross-links", () => {
    expect(existsSync(path.join(repoRoot, RELEASE_TAGGING_DOC_PATH))).toBe(true);
    expect(existsSync(path.join(repoRoot, "docs/deployment/ci-cd.md"))).toBe(true);
    expect(existsSync(path.join(repoRoot, "docs/deployment/branch-protection.md"))).toBe(
      true,
    );

    const doc = readRepoFile(RELEASE_TAGGING_DOC_PATH);
    expect(releaseTaggingDocDefinesRequiredMarkers(doc)).toBe(true);
    expect(releaseTaggingDocDefinesGuideMarkers(doc)).toBe(true);
    expect(releaseTaggingDocListsKbTags(doc)).toBe(true);
    expect(releaseTaggingDocRequiresGreenMain(doc)).toBe(true);

    const ciCd = readRepoFile("docs/deployment/ci-cd.md");
    expect(ciCd).toContain("696");
    expect(ciCd).toContain("release-tagging.md");
    expect(ciCd).toContain("ReleaseTaggingProcessDocumentationTests");

    const index = readRepoFile("docs/README.md");
    expect(index).toContain("deployment/release-tagging.md");
    expect(index).toContain("696");

    const githubReadme = readRepoFile(".github/README.md");
    expect(githubReadme).toContain("696");
    expect(githubReadme).toContain("ReleaseTaggingProcessDocumentationTests");

    const rootReadme = readRepoFile("README.md");
    expect(rootReadme).toContain("v0.9");
    expect(rootReadme).toContain("v1.0");
  });
});
