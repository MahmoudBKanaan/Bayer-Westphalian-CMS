import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { HAPPY_PATH_STEPS } from "@/features/e2e/happyPathFlow";
import {
  catalogHasUniqueFileNames,
  catalogIndexesAreSequential,
  CORE_WORKFLOW_EVIDENCE_DIR,
  CORE_WORKFLOW_SCREENSHOTS,
  CORE_WORKFLOW_SCREENSHOTS_DOC_PATH,
  CORE_WORKFLOW_SCREENSHOTS_DOC_REQUIRED_SNIPPETS,
  CORE_WORKFLOW_SCREENSHOTS_DOC_SECTIONS,
  CORE_WORKFLOW_SCREENSHOTS_TITLE,
  coreWorkflowScreenshotFileNames,
  coreWorkflowScreenshotIds,
  coreWorkflowScreenshotRepoPath,
  coreWorkflowScreenshotsDocSectionIdsInOrder,
  documentationContainsRequiredSnippets,
  docsIndexMustLinkCoreWorkflowScreenshots,
  formatCoreWorkflowScreenshotJourney,
  formatCoreWorkflowScreenshotsDocOutline,
  formatScreenshotFigureCaption,
  getCoreWorkflowScreenshotById,
  happyPathScreenshotIds,
  happyPathTitlesMatchE2eContract,
  isValidCoreWorkflowScreenshotsDocOrder,
  professionalizationScreenshotIds,
} from "@/features/testing/coreWorkflowScreenshots";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("coreWorkflowScreenshots (item 613)", () => {
  it("documents the ordered documentation outline", () => {
    expect(coreWorkflowScreenshotsDocSectionIdsInOrder()).toEqual([
      "goals",
      "kb-happy-path",
      "evidence-folder",
      "screenshot-catalog",
      "capture-procedure",
      "caption-templates",
      "manifest-helpers",
      "relationship-to-tests",
      "acceptance",
    ]);
    expect(formatCoreWorkflowScreenshotsDocOutline()).toContain("Screenshot catalog");
    expect(formatCoreWorkflowScreenshotsDocOutline()).toContain("Acceptance");
    expect(isValidCoreWorkflowScreenshotsDocOrder(coreWorkflowScreenshotsDocSectionIdsInOrder())).toBe(
      true,
    );
    expect(isValidCoreWorkflowScreenshotsDocOrder(["goals"])).toBe(false);
    expect(CORE_WORKFLOW_SCREENSHOTS_DOC_SECTIONS).toHaveLength(9);
    expect(CORE_WORKFLOW_SCREENSHOTS_TITLE).toBe("Core Workflow Screenshots");
  });

  it("catalogues 18 ordered unique screenshot evidence slots", () => {
    expect(CORE_WORKFLOW_SCREENSHOTS).toHaveLength(18);
    expect(catalogIndexesAreSequential()).toBe(true);
    expect(catalogHasUniqueFileNames()).toBe(true);
    expect(coreWorkflowScreenshotFileNames()[0]).toBe("01-login-sign-in.png");
    expect(coreWorkflowScreenshotFileNames()[17]).toBe("18-playwright-evidence.png");
    expect(happyPathScreenshotIds()).toEqual([
      "login",
      "dashboard",
      "customer-create",
      "consent-update",
      "product-create",
      "segment-create",
      "campaign-builder",
      "compliance-approval",
      "campaign-launch",
    ]);
    expect(professionalizationScreenshotIds()).toContain("role-based-menu");
    expect(professionalizationScreenshotIds()).toContain("keyboard-focus");
    expect(coreWorkflowScreenshotIds()).toHaveLength(18);
  });

  it("aligns the happy-path narrative with the KB E2E journey", () => {
    expect(formatCoreWorkflowScreenshotJourney()).toBe(
      "Employee sign-in → Dashboard analytics → Create customer → Record consent → Create product → Create segment → Campaign builder → Compliance approval → Campaign launch",
    );
    expect(happyPathTitlesMatchE2eContract()).toBe(true);
    expect(HAPPY_PATH_STEPS.map((step) => step.id)).toEqual([
      "login",
      "create-customer",
      "consent",
      "campaign",
      "approval",
      "launch",
    ]);
    expect(getCoreWorkflowScreenshotById("login").route).toBe("/login");
    expect(getCoreWorkflowScreenshotById("campaign-launch").relatedBacklogItems).toContain(605);
  });

  it("builds stable report figure captions and repo paths", () => {
    const login = getCoreWorkflowScreenshotById("login");
    expect(formatScreenshotFigureCaption(login)).toBe(
      "Figure 613-01. Login form for internal employee access.",
    );
    expect(coreWorkflowScreenshotRepoPath(login.fileName)).toBe(
      `${CORE_WORKFLOW_EVIDENCE_DIR}/01-login-sign-in.png`,
    );
  });

  it("keeps the core workflow screenshots markdown as delivery evidence", () => {
    const docPath = path.join(repoRoot, CORE_WORKFLOW_SCREENSHOTS_DOC_PATH);
    expect(existsSync(docPath), `Missing ${CORE_WORKFLOW_SCREENSHOTS_DOC_PATH}`).toBe(true);

    const documentation = readRepoFile(CORE_WORKFLOW_SCREENSHOTS_DOC_PATH);
    expect(documentationContainsRequiredSnippets(documentation)).toBe(true);
    expect(documentation).toContain("## Screenshot catalog");
    expect(documentation).toContain("## Acceptance (item 613)");
    expect(documentation).toContain("coreWorkflowScreenshots.ts");
    expect(CORE_WORKFLOW_SCREENSHOTS_DOC_REQUIRED_SNIPPETS).toContain("item **613**");
  });

  it("links core workflow screenshots from the documentation index", () => {
    const index = readRepoFile("docs/README.md");
    expect(docsIndexMustLinkCoreWorkflowScreenshots(index)).toBe(true);
    expect(index).toContain("testing/core-workflow-screenshots.md");
  });

  it("requires the evidence directory and README naming guide", () => {
    const evidenceDir = path.join(repoRoot, CORE_WORKFLOW_EVIDENCE_DIR);
    const evidenceReadme = path.join(evidenceDir, "README.md");
    expect(existsSync(evidenceDir), CORE_WORKFLOW_EVIDENCE_DIR).toBe(true);
    expect(existsSync(evidenceReadme), "docs/evidence/core-workflows/README.md").toBe(true);
    const readme = readFileSync(evidenceReadme, "utf8");
    expect(readme).toContain("item 613");
    expect(readme).toContain("01-login-sign-in.png");
  });

  it("rejects incomplete documentation snapshots", () => {
    expect(documentationContainsRequiredSnippets("# incomplete")).toBe(false);
    expect(docsIndexMustLinkCoreWorkflowScreenshots("# Documentation\n")).toBe(false);
  });
});
