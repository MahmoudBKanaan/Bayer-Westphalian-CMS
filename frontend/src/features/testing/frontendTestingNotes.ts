/**
 * Frontend testing notes catalog (KB NFR-010 / Sprint 15 item **612**).
 *
 * Locks documentation structure for docs/testing/frontend-testing-notes.md and
 * references the layered frontend test strategy.
 */

/** Relative path from repo root (documentation evidence). */
export const FRONTEND_TESTING_NOTES_DOC_PATH = "docs/testing/frontend-testing-notes.md";

export const FRONTEND_TESTING_NOTES_TITLE = "Frontend Testing Notes";

export type FrontendTestingNotesSectionId =
  | "goals"
  | "test-pyramid"
  | "tools-and-scripts"
  | "directory-map"
  | "feature-flow-pattern"
  | "integration-harness"
  | "playwright-conventions"
  | "documentation-as-test"
  | "ui-workflow-doc-index"
  | "quality-gate-timeline"
  | "conventions-checklist"
  | "do-and-dont"
  | "acceptance";

export type FrontendTestingNotesSection = {
  id: FrontendTestingNotesSectionId;
  index: number;
  title: string;
  docHeading: string;
};

/** Ordered documentation sections for item 612. */
export const FRONTEND_TESTING_NOTES_SECTIONS: FrontendTestingNotesSection[] = [
  { id: "goals", index: 0, title: "Goals", docHeading: "## Goals" },
  { id: "test-pyramid", index: 1, title: "Test pyramid", docHeading: "## Test pyramid" },
  {
    id: "tools-and-scripts",
    index: 2,
    title: "Tools and scripts",
    docHeading: "## Tools and scripts",
  },
  { id: "directory-map", index: 3, title: "Directory map", docHeading: "## Directory map" },
  {
    id: "feature-flow-pattern",
    index: 4,
    title: "Feature flow pattern",
    docHeading: "## Feature flow pattern (items 598–609)",
  },
  {
    id: "integration-harness",
    index: 5,
    title: "Integration harness conventions",
    docHeading: "## Integration harness conventions",
  },
  {
    id: "playwright-conventions",
    index: 6,
    title: "Playwright conventions",
    docHeading: "## Playwright conventions",
  },
  {
    id: "documentation-as-test",
    index: 7,
    title: "Documentation-as-test",
    docHeading: "## Documentation-as-test (items 610–613)",
  },
  {
    id: "ui-workflow-doc-index",
    index: 8,
    title: "UI workflow documentation index",
    docHeading: "## UI workflow documentation index",
  },
  {
    id: "quality-gate-timeline",
    index: 9,
    title: "Quality gate timeline",
    docHeading: "## Quality gate timeline",
  },
  {
    id: "conventions-checklist",
    index: 10,
    title: "Conventions checklist",
    docHeading: "## Conventions checklist",
  },
  { id: "do-and-dont", index: 11, title: "Do / don’t", docHeading: "## Do / don’t" },
  {
    id: "acceptance",
    index: 12,
    title: "Acceptance",
    docHeading: "## Acceptance (item 612)",
  },
];

export function frontendTestingNotesSectionIdsInOrder(): FrontendTestingNotesSectionId[] {
  return FRONTEND_TESTING_NOTES_SECTIONS.map((section) => section.id);
}

export function formatFrontendTestingNotesOutline(): string {
  return FRONTEND_TESTING_NOTES_SECTIONS.map((section) => section.title).join(" → ");
}

export function isValidFrontendTestingNotesSectionOrder(ids: string[]): boolean {
  const expected = frontendTestingNotesSectionIdsInOrder();
  if (ids.length !== expected.length) {
    return false;
  }
  return ids.every((id, index) => id === expected[index]);
}

/** npm scripts documented for frontend quality. */
export const FRONTEND_TEST_NPM_SCRIPTS = [
  "test",
  "test:watch",
  "test:e2e",
  "verify",
  "lint",
  "format:check",
  "build",
] as const;

/** Pyramid layers described in the notes. */
export const FRONTEND_TEST_LAYERS = [
  "pure-feature-contracts",
  "component-tests",
  "page-tests",
  "integration-tests",
  "playwright-e2e",
] as const;

/** Key tools named in the notes. */
export const FRONTEND_TEST_TOOLS = [
  "Vitest",
  "React Testing Library",
  "user-event",
  "jsdom",
  "Playwright",
] as const;

/** Important paths (frontend-package relative or repo-relative as documented). */
export const FRONTEND_TEST_PATHS = {
  features: "src/features",
  components: "src/components",
  pages: "src/pages",
  integration: "src/test/integration",
  renderApp: "src/test/integration/renderApp.tsx",
  e2e: "tests/e2e",
  e2eHelpers: "tests/e2e/helpers",
  playwrightConfig: "playwright.config.ts",
  happyPathFlow: "src/features/e2e/happyPathFlow.ts",
  happyPathMock: "src/features/e2e/happyPathApiMock.ts",
} as const;

/** Related documentation paths (repo-relative). */
export const FRONTEND_TESTING_RELATED_DOCS = [
  "docs/testing/frontend-component-tests.md",
  "docs/testing/frontend-integration-tests.md",
  "docs/testing/playwright-e2e.md",
  "docs/testing/frontend-testing-notes.md",
  "docs/testing/core-workflow-screenshots.md",
  "docs/testing/ui-login-flow.md",
  "docs/testing/ui-main-screens-accessibility.md",
  "docs/testing/ui-keyboard-navigation.md",
  "docs/development/accessibility-notes.md",
  "docs/development/ui-style-notes.md",
] as const;

/** UI workflow docs that form the 598–609 acceptance series. */
export const FRONTEND_UI_WORKFLOW_DOCS = [
  "docs/testing/ui-login-flow.md",
  "docs/testing/ui-customer-creation.md",
  "docs/testing/ui-consent-update.md",
  "docs/testing/ui-product-creation.md",
  "docs/testing/ui-segment-creation.md",
  "docs/testing/ui-campaign-creation.md",
  "docs/testing/ui-compliance-approval.md",
  "docs/testing/ui-campaign-launch.md",
  "docs/testing/ui-dashboard-analytics.md",
  "docs/testing/ui-role-based-menu.md",
  "docs/testing/ui-keyboard-navigation.md",
  "docs/testing/ui-main-screens-accessibility.md",
] as const;

/** Backlog items the frontend testing notes explicitly support. */
export const FRONTEND_TESTING_NOTES_RELATED_ITEMS = [
  595, // component tests
  596, // integration tests
  597, // playwright happy path
  598, // login UI
  599, // customer create UI
  600, // consent UI
  601, // product create UI
  602, // segment create UI
  603, // campaign create UI
  604, // compliance UI
  605, // launch UI
  606, // dashboard analytics UI
  607, // role-based menu
  608, // keyboard navigation
  609, // main screens a11y
  610, // UI style notes
  611, // accessibility notes
  612, // frontend testing notes (this item)
  613, // screenshots
  617, // full suite run gate
  635, // run component tests
  636, // run integration tests
  637, // run E2E
  638, // run a11y checks
] as const;

/**
 * Substrings required in docs/testing/frontend-testing-notes.md.
 */
export const FRONTEND_TESTING_NOTES_DOC_REQUIRED_SNIPPETS = [
  FRONTEND_TESTING_NOTES_TITLE,
  "item **612**",
  "NFR-010",
  ...FRONTEND_TESTING_NOTES_SECTIONS.map((section) => section.docHeading),
  "Vitest",
  "Playwright",
  "React Testing Library",
  "renderApp",
  "happyPathApiMock",
  "npm test",
  "npm run test:e2e",
  "npm run verify",
  "do not run any tests",
  "item **595**",
  "item **596**",
  "item **597**",
  "item **608**",
  "item **609**",
  "item **617**",
  "item **635**",
  "item **637**",
  "src/test/integration",
  "tests/e2e",
  "frontendTestingNotes.ts",
] as const;

export function docsIndexMustLinkFrontendTestingNotes(indexMarkdown: string): boolean {
  return (
    indexMarkdown.includes("testing/frontend-testing-notes.md") ||
    indexMarkdown.includes("frontend-testing-notes.md")
  );
}

export function documentationContainsRequiredSnippets(documentationMarkdown: string): boolean {
  return FRONTEND_TESTING_NOTES_DOC_REQUIRED_SNIPPETS.every((snippet) =>
    documentationMarkdown.includes(snippet),
  );
}

/**
 * package.json scripts must expose the documented quality commands.
 */
export function packageJsonSupportsFrontendTestingNotes(packageJsonText: string): boolean {
  return (
    packageJsonText.includes('"test"') &&
    packageJsonText.includes('"test:e2e"') &&
    packageJsonText.includes('"verify"') &&
    packageJsonText.includes("vitest") &&
    packageJsonText.includes("playwright")
  );
}
