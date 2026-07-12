import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  documentationContainsRequiredSnippets,
  docsIndexMustLinkFrontendTestingNotes,
  formatFrontendTestingNotesOutline,
  FRONTEND_TEST_LAYERS,
  FRONTEND_TEST_NPM_SCRIPTS,
  FRONTEND_TEST_PATHS,
  FRONTEND_TEST_TOOLS,
  FRONTEND_TESTING_NOTES_DOC_PATH,
  FRONTEND_TESTING_NOTES_DOC_REQUIRED_SNIPPETS,
  FRONTEND_TESTING_NOTES_RELATED_ITEMS,
  FRONTEND_TESTING_NOTES_SECTIONS,
  FRONTEND_TESTING_NOTES_TITLE,
  FRONTEND_TESTING_RELATED_DOCS,
  FRONTEND_UI_WORKFLOW_DOCS,
  frontendTestingNotesSectionIdsInOrder,
  isValidFrontendTestingNotesSectionOrder,
  packageJsonSupportsFrontendTestingNotes,
} from "@/features/testing/frontendTestingNotes";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");
const frontendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("frontendTestingNotes (item 612)", () => {
  it("documents the ordered frontend testing notes outline", () => {
    expect(frontendTestingNotesSectionIdsInOrder()).toEqual([
      "goals",
      "test-pyramid",
      "tools-and-scripts",
      "directory-map",
      "feature-flow-pattern",
      "integration-harness",
      "playwright-conventions",
      "documentation-as-test",
      "ui-workflow-doc-index",
      "quality-gate-timeline",
      "conventions-checklist",
      "do-and-dont",
      "acceptance",
    ]);
    expect(formatFrontendTestingNotesOutline()).toContain("Test pyramid");
    expect(formatFrontendTestingNotesOutline()).toContain("Acceptance");
    expect(isValidFrontendTestingNotesSectionOrder(frontendTestingNotesSectionIdsInOrder())).toBe(
      true,
    );
    expect(isValidFrontendTestingNotesSectionOrder(["goals"])).toBe(false);
    expect(FRONTEND_TESTING_NOTES_SECTIONS).toHaveLength(13);
    expect(FRONTEND_TESTING_NOTES_TITLE).toBe("Frontend Testing Notes");
    expect(
      FRONTEND_TESTING_NOTES_SECTIONS.find((s) => s.id === "documentation-as-test")?.docHeading,
    ).toContain("610–613");
  });

  it("catalogues tools, scripts, layers, and key paths", () => {
    expect(FRONTEND_TEST_TOOLS).toContain("Vitest");
    expect(FRONTEND_TEST_TOOLS).toContain("Playwright");
    expect(FRONTEND_TEST_TOOLS).toContain("React Testing Library");
    expect(FRONTEND_TEST_NPM_SCRIPTS).toContain("test");
    expect(FRONTEND_TEST_NPM_SCRIPTS).toContain("test:e2e");
    expect(FRONTEND_TEST_NPM_SCRIPTS).toContain("verify");
    expect(FRONTEND_TEST_LAYERS).toEqual([
      "pure-feature-contracts",
      "component-tests",
      "page-tests",
      "integration-tests",
      "playwright-e2e",
    ]);
    expect(FRONTEND_TEST_PATHS.integration).toBe("src/test/integration");
    expect(FRONTEND_TEST_PATHS.e2e).toBe("tests/e2e");
    expect(FRONTEND_TEST_PATHS.renderApp).toContain("renderApp.tsx");
    expect(FRONTEND_TESTING_NOTES_RELATED_ITEMS).toContain(612);
    expect(FRONTEND_TESTING_NOTES_RELATED_ITEMS).toContain(595);
    expect(FRONTEND_TESTING_NOTES_RELATED_ITEMS).toContain(597);
    expect(FRONTEND_TESTING_NOTES_RELATED_ITEMS).toContain(637);
    expect(FRONTEND_TESTING_NOTES_DOC_REQUIRED_SNIPPETS).toContain("item **612**");
  });

  it("keeps the frontend testing notes markdown file as delivery evidence", () => {
    const docPath = path.join(repoRoot, FRONTEND_TESTING_NOTES_DOC_PATH);
    expect(existsSync(docPath), `Missing ${FRONTEND_TESTING_NOTES_DOC_PATH}`).toBe(true);

    const documentation = readRepoFile(FRONTEND_TESTING_NOTES_DOC_PATH);
    expect(documentationContainsRequiredSnippets(documentation)).toBe(true);
    expect(documentation).toContain("## Test pyramid");
    expect(documentation).toContain("## Feature flow pattern (items 598–609)");
    expect(documentation).toContain("## Acceptance (item 612)");
    expect(documentation).toContain("frontendTestingNotes.ts");
    expect(documentation).toContain("do not run any tests");
  });

  it("links frontend testing notes from the documentation index", () => {
    const index = readRepoFile("docs/README.md");
    expect(docsIndexMustLinkFrontendTestingNotes(index)).toBe(true);
    expect(index).toContain("testing/frontend-testing-notes.md");
  });

  it("requires related suite docs and UI workflow docs to exist", () => {
    for (const relative of FRONTEND_TESTING_RELATED_DOCS) {
      expect(existsSync(path.join(repoRoot, relative)), relative).toBe(true);
    }
    for (const relative of FRONTEND_UI_WORKFLOW_DOCS) {
      expect(existsSync(path.join(repoRoot, relative)), relative).toBe(true);
    }
  });

  it("requires core frontend test directories to exist", () => {
    expect(existsSync(path.join(frontendRoot, FRONTEND_TEST_PATHS.integration))).toBe(true);
    expect(existsSync(path.join(frontendRoot, FRONTEND_TEST_PATHS.renderApp))).toBe(true);
    expect(existsSync(path.join(frontendRoot, FRONTEND_TEST_PATHS.e2e))).toBe(true);
    expect(existsSync(path.join(frontendRoot, FRONTEND_TEST_PATHS.playwrightConfig))).toBe(true);
    expect(existsSync(path.join(frontendRoot, FRONTEND_TEST_PATHS.happyPathFlow))).toBe(true);
    expect(existsSync(path.join(frontendRoot, FRONTEND_TEST_PATHS.happyPathMock))).toBe(true);
  });

  it("keeps package.json scripts aligned with documented quality commands", () => {
    const packageJson = readFileSync(path.join(frontendRoot, "package.json"), "utf8");
    expect(packageJsonSupportsFrontendTestingNotes(packageJson)).toBe(true);
  });

  it("rejects incomplete documentation or package snapshots", () => {
    expect(documentationContainsRequiredSnippets("# incomplete")).toBe(false);
    expect(packageJsonSupportsFrontendTestingNotes("{}")).toBe(false);
    expect(docsIndexMustLinkFrontendTestingNotes("# Documentation\n")).toBe(false);
  });
});
