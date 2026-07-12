/**
 * Core workflow screenshots catalog (KB Testing Plan / item **613**).
 *
 * Inventory of report/demo evidence captures for the happy-path journey and
 * Sprint 15 professionalization surfaces. Binary files live under
 * docs/evidence/core-workflows/.
 */

import { HAPPY_PATH_STEPS } from "@/features/e2e/happyPathFlow";

/** Relative path from repo root for binary evidence. */
export const CORE_WORKFLOW_EVIDENCE_DIR = "docs/evidence/core-workflows";

/** Documentation path (repo-relative). */
export const CORE_WORKFLOW_SCREENSHOTS_DOC_PATH = "docs/testing/core-workflow-screenshots.md";

export const CORE_WORKFLOW_SCREENSHOTS_TITLE = "Core Workflow Screenshots";

export type CoreWorkflowScreenshotCategory = "happy-path" | "professionalization";

export type CoreWorkflowScreenshotId =
  | "login"
  | "dashboard"
  | "customer-create"
  | "consent-update"
  | "product-create"
  | "segment-create"
  | "campaign-builder"
  | "compliance-approval"
  | "campaign-launch"
  | "app-shell-layout"
  | "role-based-menu"
  | "form-validation"
  | "confirmation-dialog"
  | "status-badges"
  | "responsive-tablet"
  | "keyboard-focus"
  | "skip-link-or-landmarks"
  | "playwright-evidence";

export type CoreWorkflowScreenshotDefinition = {
  id: CoreWorkflowScreenshotId;
  index: number;
  /** Zero-padded file name including extension. */
  fileName: string;
  title: string;
  description: string;
  /** Primary SPA route or surface description. */
  route: string;
  category: CoreWorkflowScreenshotCategory;
  relatedBacklogItems: number[];
};

/**
 * Ordered screenshot inventory for item 613.
 * File names are stable report appendix identifiers.
 */
export const CORE_WORKFLOW_SCREENSHOTS: CoreWorkflowScreenshotDefinition[] = [
  {
    id: "login",
    index: 1,
    fileName: "01-login-sign-in.png",
    title: "Employee sign-in",
    description: "Login form for internal employee access.",
    route: "/login",
    category: "happy-path",
    relatedBacklogItems: [598, 597, 613],
  },
  {
    id: "dashboard",
    index: 2,
    fileName: "02-dashboard-analytics.png",
    title: "Dashboard analytics",
    description: "KPI cards and analytics shell after login.",
    route: "/dashboard",
    category: "happy-path",
    relatedBacklogItems: [606, 591, 613],
  },
  {
    id: "customer-create",
    index: 3,
    fileName: "03-customer-create.png",
    title: "Create customer",
    description: "Customer create form and list on Customers page.",
    route: "/customers",
    category: "happy-path",
    relatedBacklogItems: [599, 597, 613],
  },
  {
    id: "consent-update",
    index: 4,
    fileName: "04-consent-update.png",
    title: "Record consent",
    description: "Consent panel on customer details.",
    route: "/customers/:id",
    category: "happy-path",
    relatedBacklogItems: [600, 597, 613],
  },
  {
    id: "product-create",
    index: 5,
    fileName: "05-product-create.png",
    title: "Create product",
    description: "Product create form on Products page.",
    route: "/products",
    category: "happy-path",
    relatedBacklogItems: [601, 613],
  },
  {
    id: "segment-create",
    index: 6,
    fileName: "06-segment-create.png",
    title: "Create segment",
    description: "Segment create form with criteria builder.",
    route: "/segments",
    category: "happy-path",
    relatedBacklogItems: [602, 613],
  },
  {
    id: "campaign-builder",
    index: 7,
    fileName: "07-campaign-builder.png",
    title: "Campaign builder",
    description: "Multi-step campaign builder for draft creation.",
    route: "/campaign-builder",
    category: "happy-path",
    relatedBacklogItems: [603, 592, 597, 613],
  },
  {
    id: "compliance-approval",
    index: 8,
    fileName: "08-compliance-approval.png",
    title: "Compliance approval",
    description: "Submitted campaign queue and approve decision.",
    route: "/compliance",
    category: "happy-path",
    relatedBacklogItems: [604, 593, 597, 613],
  },
  {
    id: "campaign-launch",
    index: 9,
    fileName: "09-campaign-launch.png",
    title: "Campaign launch",
    description: "Recipient preview with launch confirmation path.",
    route: "/campaigns/:campaignId/recipients/preview",
    category: "happy-path",
    relatedBacklogItems: [605, 594, 597, 613],
  },
  {
    id: "app-shell-layout",
    index: 10,
    fileName: "10-app-shell-layout.png",
    title: "Application shell layout",
    description: "Sidebar navigation, top bar, and main content.",
    route: "/dashboard",
    category: "professionalization",
    relatedBacklogItems: [569, 570, 571, 573, 613],
  },
  {
    id: "role-based-menu",
    index: 11,
    fileName: "11-role-based-menu.png",
    title: "Role-based menu",
    description: "Main navigation filtered for a non-admin role.",
    route: "/dashboard",
    category: "professionalization",
    relatedBacklogItems: [607, 572, 613],
  },
  {
    id: "form-validation",
    index: 12,
    fileName: "12-form-validation.png",
    title: "Form validation",
    description: "Visible field validation messages on a core form.",
    route: "/login",
    category: "professionalization",
    relatedBacklogItems: [578, 598, 613],
  },
  {
    id: "confirmation-dialog",
    index: 13,
    fileName: "13-confirmation-dialog.png",
    title: "Confirmation dialog",
    description: "Sensitive-action confirmation dialog open.",
    route: "/compliance",
    category: "professionalization",
    relatedBacklogItems: [579, 604, 605, 613],
  },
  {
    id: "status-badges",
    index: 14,
    fileName: "14-status-badges.png",
    title: "Status badges",
    description: "Campaign or domain status badges in a worklist.",
    route: "/campaigns",
    category: "professionalization",
    relatedBacklogItems: [580, 581, 582, 613],
  },
  {
    id: "responsive-tablet",
    index: 15,
    fileName: "15-responsive-tablet.png",
    title: "Responsive tablet layout",
    description: "Shell usable at tablet width band.",
    route: "/dashboard",
    category: "professionalization",
    relatedBacklogItems: [585, 586, 613],
  },
  {
    id: "keyboard-focus",
    index: 16,
    fileName: "16-keyboard-focus.png",
    title: "Keyboard focus indicator",
    description: "Visible focus-visible ring on an interactive control.",
    route: "/login",
    category: "professionalization",
    relatedBacklogItems: [608, 587, 613],
  },
  {
    id: "skip-link-or-landmarks",
    index: 17,
    fileName: "17-skip-link-or-landmarks.png",
    title: "Skip link or landmarks",
    description: "Skip-to-content focused or clear landmark structure.",
    route: "/dashboard",
    category: "professionalization",
    relatedBacklogItems: [609, 611, 613],
  },
  {
    id: "playwright-evidence",
    index: 18,
    fileName: "18-playwright-evidence.png",
    title: "Playwright E2E evidence",
    description: "Playwright report or terminal green run for happy path.",
    route: "tests/e2e/happy-path.spec.ts",
    category: "professionalization",
    relatedBacklogItems: [597, 637, 645, 613],
  },
];

export type CoreWorkflowScreenshotsDocSectionId =
  | "goals"
  | "kb-happy-path"
  | "evidence-folder"
  | "screenshot-catalog"
  | "capture-procedure"
  | "caption-templates"
  | "manifest-helpers"
  | "relationship-to-tests"
  | "acceptance";

export type CoreWorkflowScreenshotsDocSection = {
  id: CoreWorkflowScreenshotsDocSectionId;
  index: number;
  title: string;
  docHeading: string;
};

export const CORE_WORKFLOW_SCREENSHOTS_DOC_SECTIONS: CoreWorkflowScreenshotsDocSection[] = [
  { id: "goals", index: 0, title: "Goals", docHeading: "## Goals" },
  {
    id: "kb-happy-path",
    index: 1,
    title: "KB happy-path journey",
    docHeading: "## KB happy-path journey",
  },
  { id: "evidence-folder", index: 2, title: "Evidence folder", docHeading: "## Evidence folder" },
  {
    id: "screenshot-catalog",
    index: 3,
    title: "Screenshot catalog",
    docHeading: "## Screenshot catalog",
  },
  {
    id: "capture-procedure",
    index: 4,
    title: "Capture procedure",
    docHeading: "## Capture procedure",
  },
  {
    id: "caption-templates",
    index: 5,
    title: "Caption templates",
    docHeading: "## Caption templates (report appendix)",
  },
  {
    id: "manifest-helpers",
    index: 6,
    title: "Manifest helpers",
    docHeading: "## Manifest helpers",
  },
  {
    id: "relationship-to-tests",
    index: 7,
    title: "Relationship to automated tests",
    docHeading: "## Relationship to automated tests",
  },
  {
    id: "acceptance",
    index: 8,
    title: "Acceptance",
    docHeading: "## Acceptance (item 613)",
  },
];

export function coreWorkflowScreenshotsDocSectionIdsInOrder(): CoreWorkflowScreenshotsDocSectionId[] {
  return CORE_WORKFLOW_SCREENSHOTS_DOC_SECTIONS.map((section) => section.id);
}

export function formatCoreWorkflowScreenshotsDocOutline(): string {
  return CORE_WORKFLOW_SCREENSHOTS_DOC_SECTIONS.map((section) => section.title).join(" → ");
}

export function isValidCoreWorkflowScreenshotsDocOrder(ids: string[]): boolean {
  const expected = coreWorkflowScreenshotsDocSectionIdsInOrder();
  if (ids.length !== expected.length) {
    return false;
  }
  return ids.every((id, index) => id === expected[index]);
}

export function getCoreWorkflowScreenshotById(
  id: CoreWorkflowScreenshotId,
): CoreWorkflowScreenshotDefinition {
  const shot = CORE_WORKFLOW_SCREENSHOTS.find((candidate) => candidate.id === id);
  if (shot == null) {
    throw new Error(`Unknown core workflow screenshot id: ${id}`);
  }
  return shot;
}

export function coreWorkflowScreenshotIds(): CoreWorkflowScreenshotId[] {
  return CORE_WORKFLOW_SCREENSHOTS.map((shot) => shot.id);
}

export function coreWorkflowScreenshotFileNames(): string[] {
  return CORE_WORKFLOW_SCREENSHOTS.map((shot) => shot.fileName);
}

export function happyPathScreenshotIds(): CoreWorkflowScreenshotId[] {
  return CORE_WORKFLOW_SCREENSHOTS.filter((shot) => shot.category === "happy-path").map(
    (shot) => shot.id,
  );
}

export function professionalizationScreenshotIds(): CoreWorkflowScreenshotId[] {
  return CORE_WORKFLOW_SCREENSHOTS.filter((shot) => shot.category === "professionalization").map(
    (shot) => shot.id,
  );
}

/**
 * Ordered narrative used in docs and report prose.
 */
export function formatCoreWorkflowScreenshotJourney(
  shots: readonly CoreWorkflowScreenshotDefinition[] = CORE_WORKFLOW_SCREENSHOTS.filter(
    (shot) => shot.category === "happy-path",
  ),
): string {
  return shots
    .slice()
    .sort((left, right) => left.index - right.index)
    .map((shot) => shot.title)
    .join(" → ");
}

/**
 * Repo-relative path for a screenshot file under the evidence directory.
 */
export function coreWorkflowScreenshotRepoPath(fileName: string): string {
  return `${CORE_WORKFLOW_EVIDENCE_DIR}/${fileName}`;
}

/**
 * Captions suggested for report figures (Figure 613-NN).
 */
export function formatScreenshotFigureCaption(shot: CoreWorkflowScreenshotDefinition): string {
  const padded = String(shot.index).padStart(2, "0");
  return `Figure 613-${padded}. ${shot.description}`;
}

/** Happy-path step titles must stay aligned with Playwright happyPathFlow. */
export function happyPathTitlesMatchE2eContract(): boolean {
  const e2eTitles = HAPPY_PATH_STEPS.map((step) => step.title.toLowerCase());
  // Screenshot titles are user-facing; map loosely to E2E step themes.
  const requiredThemes = ["customer", "consent", "campaign", "compliance", "launch"];
  const journey = formatCoreWorkflowScreenshotJourney().toLowerCase();
  return (
    (journey.includes("login") || journey.includes("sign-in")) &&
    requiredThemes.every((theme) => journey.includes(theme)) &&
    e2eTitles.some((title) => title.includes("login"))
  );
}

export const CORE_WORKFLOW_SCREENSHOTS_DOC_REQUIRED_SNIPPETS = [
  CORE_WORKFLOW_SCREENSHOTS_TITLE,
  "item **613**",
  "NFR-010",
  ...CORE_WORKFLOW_SCREENSHOTS_DOC_SECTIONS.map((section) => section.docHeading),
  CORE_WORKFLOW_EVIDENCE_DIR,
  "01-login-sign-in.png",
  "09-campaign-launch.png",
  "10-app-shell-layout.png",
  "18-playwright-evidence.png",
  "Login → create customer → consent → campaign → approval → launch",
  "coreWorkflowScreenshots.ts",
  "do not run any tests",
  "item **597**",
  "item **598**",
  "item **607**",
  "Binary PNG files may still be pending",
] as const;

export function docsIndexMustLinkCoreWorkflowScreenshots(indexMarkdown: string): boolean {
  return (
    indexMarkdown.includes("testing/core-workflow-screenshots.md") ||
    indexMarkdown.includes("core-workflow-screenshots.md")
  );
}

export function documentationContainsRequiredSnippets(documentationMarkdown: string): boolean {
  return CORE_WORKFLOW_SCREENSHOTS_DOC_REQUIRED_SNIPPETS.every((snippet) =>
    documentationMarkdown.includes(snippet),
  );
}

export function catalogHasUniqueFileNames(
  shots: readonly CoreWorkflowScreenshotDefinition[] = CORE_WORKFLOW_SCREENSHOTS,
): boolean {
  const names = shots.map((shot) => shot.fileName);
  return new Set(names).size === names.length;
}

export function catalogIndexesAreSequential(
  shots: readonly CoreWorkflowScreenshotDefinition[] = CORE_WORKFLOW_SCREENSHOTS,
): boolean {
  return shots.every((shot, offset) => shot.index === offset + 1);
}
