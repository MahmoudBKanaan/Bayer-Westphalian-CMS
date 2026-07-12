/**
 * Sprint 17 item **696**: Add release tagging process.
 *
 * KB: release versions v0.1–v1.0 are Git tags on green `main`. Catalog locks the process guide
 * (does not create or push tags).
 */

export const RELEASE_TAGGING_PROCESS_ITEM = 696;

export const RELEASE_TAGGING_PROCESS_STATEMENT = "Add release tagging process";

export const RELEASE_TAGGING_GUIDE_ITEM = 711;

export const RELEASE_TAGGING_GUIDE_STATEMENT = "Release tagging guide";

export const MAIN_CI_RELEASE_GATE_ITEM = 714;

export const MAIN_CI_RELEASE_GATE_STATEMENT =
  "The main branch must not be considered releasable unless CI passes";

export type CiRunEvidence = {
  branch: string;
  headSha: string;
  expectedSha: string;
  status: string;
  conclusion: string | null;
};

export function isMainCommitReleasable(evidence: CiRunEvidence): boolean {
  return (
    evidence.branch === "main" &&
    evidence.headSha === evidence.expectedSha &&
    evidence.status === "completed" &&
    evidence.conclusion === "success"
  );
}

export const RELEASE_TAGGING_DOC_PATH = "docs/deployment/release-tagging.md";

export const CI_CD_DOC_PATH = "docs/deployment/ci-cd.md";

export const BACKEND_DOCUMENTATION_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.ReleaseTaggingProcessDocumentationTests";

export const RELEASE_TAGGING_GUIDE_TEST_CLASS =
  "com.bayerwestphalian.campaign.common.config.ReleaseTaggingGuideDocumentationTests";

/** Branch from which official release tags are created. */
export const RELEASE_TAG_BRANCH = "main";

/** KB milestone tags (course release plan). */
export const KB_RELEASE_TAGS = [
  "v0.1",
  "v0.2",
  "v0.3",
  "v0.4",
  "v0.5",
  "v0.6",
  "v0.7",
  "v0.8",
  "v0.9",
  "v1.0",
] as const;

export type KbReleaseTag = (typeof KB_RELEASE_TAGS)[number];

/** Required markers in docs/deployment/release-tagging.md. */
export const RELEASE_TAGGING_DOC_REQUIRED_MARKERS = [
  "696",
  "release tagging",
  "main",
  "git tag",
  "v0.1",
  "v0.9",
  "v1.0",
  "annotated",
  "git push origin",
  "immutable",
  "CI",
  "ReleaseTaggingProcessDocumentationTests",
  "branch-protection.md",
  "714",
] as const;

export const RELEASE_TAGGING_GUIDE_REQUIRED_MARKERS = [
  "711",
  "Release tagging guide",
  "Release roles and ownership",
  "Release operator",
  "Reviewer / admin",
  "System Auditor",
  "Verification commands",
  "Release notes template",
  "Evidence capture",
  "Troubleshooting",
  "git ls-remote --tags origin",
  "ReleaseTaggingGuideDocumentationTests",
] as const;

export const RELEASE_TAGGING_PROCESS = {
  item: RELEASE_TAGGING_PROCESS_ITEM,
  statement: RELEASE_TAGGING_PROCESS_STATEMENT,
  docPath: RELEASE_TAGGING_DOC_PATH,
  tagBranch: RELEASE_TAG_BRANCH,
  kbTags: KB_RELEASE_TAGS,
  preferAnnotatedTags: true,
  requireGreenCi: true,
  forbidForceMovingPublishedTags: true,
  tagOnlyFromMain: true,
} as const;

/**
 * True when release-tagging markdown includes all required process markers.
 */
export function releaseTaggingDocDefinesRequiredMarkers(markdown: string): boolean {
  if (markdown == null || markdown.trim() === "") {
    return false;
  }
  return RELEASE_TAGGING_DOC_REQUIRED_MARKERS.every((marker) => {
    if (
      marker === "release tagging" ||
      marker === "annotated" ||
      marker === "immutable"
    ) {
      return markdown.toLowerCase().includes(marker.toLowerCase());
    }
    return markdown.includes(marker);
  });
}

/**
 * True when release-tagging markdown includes the expanded guide markers for item 711.
 */
export function releaseTaggingDocDefinesGuideMarkers(markdown: string): boolean {
  if (markdown == null || markdown.trim() === "") {
    return false;
  }
  return RELEASE_TAGGING_GUIDE_REQUIRED_MARKERS.every((marker) =>
    markdown.includes(marker),
  );
}

/**
 * True when the guide lists every KB milestone tag.
 */
export function releaseTaggingDocListsKbTags(
  markdown: string,
  tags: readonly string[] = KB_RELEASE_TAGS,
): boolean {
  if (markdown == null || markdown.trim() === "") {
    return false;
  }
  return tags.every((tag) => markdown.includes(tag));
}

/**
 * True when a tag string matches the KB milestone pattern v0.x or v1.0 (simple check).
 */
export function isKbStyleReleaseTag(tag: string | null | undefined): boolean {
  if (tag == null || tag === "") {
    return false;
  }
  return /^v\d+\.\d+(\.\d+)?$/.test(tag);
}

/**
 * True when the guide requires green CI / main before tagging.
 */
export function releaseTaggingDocRequiresGreenMain(markdown: string): boolean {
  if (markdown == null || markdown.trim() === "") {
    return false;
  }
  const lower = markdown.toLowerCase();
  return lower.includes("main") && lower.includes("green") && lower.includes("ci");
}
