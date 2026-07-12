/**
 * Sprint 15 production gate (KB item **616**):
 * “A business user should be able to complete core workflows without developer help.”
 *
 * Catalogs core UI workflows, evidence links, and gate evaluation helpers.
 */

/** Repo-relative path for the production gate document. */
export const BUSINESS_USER_WORKFLOW_GATE_DOC_PATH =
  "docs/agile/sprint-15-production-gate.md";

export const BUSINESS_USER_WORKFLOW_GATE_TITLE = "Sprint 15 Production Gate";

export const BUSINESS_USER_WORKFLOW_GATE_STATEMENT =
  "A business user should be able to complete core workflows without developer help.";

export type BusinessUserWorkflowId =
  | "login"
  | "customer-create"
  | "consent-update"
  | "product-create"
  | "segment-create"
  | "campaign-create"
  | "compliance-approval"
  | "campaign-launch"
  | "dashboard-analytics";

export type BusinessUserWorkflowDefinition = {
  id: BusinessUserWorkflowId;
  index: number;
  title: string;
  /** Primary SPA route. */
  route: string;
  /** Business roles that typically perform the step (not exclusive of ADMIN). */
  primaryRoles: string[];
  /** Repo-relative user guide path. */
  userGuidePath: string;
  /** Repo-relative UI acceptance doc. */
  uiAcceptanceDocPath: string;
  /** Sprint 15 UI acceptance backlog item. */
  uiBacklogItem: number;
  /** True when the workflow is part of the KB E2E happy path. */
  onHappyPath: boolean;
  /**
   * Business users complete this step via browser UI only (no SQL/CLI required).
   */
  uiOnly: boolean;
};

/**
 * Core workflows a business user must complete without developer assistance.
 */
export const BUSINESS_USER_CORE_WORKFLOWS: BusinessUserWorkflowDefinition[] = [
  {
    id: "login",
    index: 1,
    title: "Sign in",
    route: "/login",
    primaryRoles: ["ALL_EMPLOYEES"],
    userGuidePath: "docs/user-guides/campaign-manager-guide.md",
    uiAcceptanceDocPath: "docs/testing/ui-login-flow.md",
    uiBacklogItem: 598,
    onHappyPath: true,
    uiOnly: true,
  },
  {
    id: "customer-create",
    index: 2,
    title: "Create customer",
    route: "/customers",
    primaryRoles: ["ADMIN", "CUSTOMER_SERVICE_AGENT"],
    userGuidePath: "docs/user-guides/customer-service-agent-guide.md",
    uiAcceptanceDocPath: "docs/testing/ui-customer-creation.md",
    uiBacklogItem: 599,
    onHappyPath: true,
    uiOnly: true,
  },
  {
    id: "consent-update",
    index: 3,
    title: "Record consent",
    route: "/customers/:id",
    primaryRoles: ["ADMIN", "COMPLIANCE_OFFICER", "CUSTOMER_SERVICE_AGENT"],
    userGuidePath: "docs/user-guides/compliance-officer-guide.md",
    uiAcceptanceDocPath: "docs/testing/ui-consent-update.md",
    uiBacklogItem: 600,
    onHappyPath: true,
    uiOnly: true,
  },
  {
    id: "product-create",
    index: 4,
    title: "Create product",
    route: "/products",
    primaryRoles: ["ADMIN", "PRODUCT_MANAGER"],
    userGuidePath: "docs/user-guides/product-manager-guide.md",
    uiAcceptanceDocPath: "docs/testing/ui-product-creation.md",
    uiBacklogItem: 601,
    onHappyPath: true,
    uiOnly: true,
  },
  {
    id: "segment-create",
    index: 5,
    title: "Create segment",
    route: "/segments",
    primaryRoles: ["ADMIN", "CAMPAIGN_MANAGER"],
    userGuidePath: "docs/user-guides/segmentation-user-guide.md",
    uiAcceptanceDocPath: "docs/testing/ui-segment-creation.md",
    uiBacklogItem: 602,
    onHappyPath: true,
    uiOnly: true,
  },
  {
    id: "campaign-create",
    index: 6,
    title: "Create and submit campaign",
    route: "/campaign-builder",
    primaryRoles: ["ADMIN", "CAMPAIGN_MANAGER"],
    userGuidePath: "docs/user-guides/campaign-manager-guide.md",
    uiAcceptanceDocPath: "docs/testing/ui-campaign-creation.md",
    uiBacklogItem: 603,
    onHappyPath: true,
    uiOnly: true,
  },
  {
    id: "compliance-approval",
    index: 7,
    title: "Approve campaign",
    route: "/compliance",
    primaryRoles: ["ADMIN", "COMPLIANCE_OFFICER"],
    userGuidePath: "docs/user-guides/compliance-officer-guide.md",
    uiAcceptanceDocPath: "docs/testing/ui-compliance-approval.md",
    uiBacklogItem: 604,
    onHappyPath: true,
    uiOnly: true,
  },
  {
    id: "campaign-launch",
    index: 8,
    title: "Launch campaign",
    route: "/campaigns/:campaignId/recipients/preview",
    primaryRoles: ["ADMIN", "CAMPAIGN_MANAGER"],
    userGuidePath: "docs/user-guides/campaign-manager-guide.md",
    uiAcceptanceDocPath: "docs/testing/ui-campaign-launch.md",
    uiBacklogItem: 605,
    onHappyPath: true,
    uiOnly: true,
  },
  {
    id: "dashboard-analytics",
    index: 9,
    title: "View dashboard analytics",
    route: "/dashboard",
    primaryRoles: ["ADMIN", "BI_ANALYST", "CAMPAIGN_MANAGER", "EXECUTIVE_VIEWER"],
    userGuidePath: "docs/user-guides/bi-analyst-guide.md",
    uiAcceptanceDocPath: "docs/testing/ui-dashboard-analytics.md",
    uiBacklogItem: 606,
    onHappyPath: true,
    uiOnly: true,
  },
];

export type BusinessUserGateDocSectionId =
  | "gate-statement"
  | "without-developer-help"
  | "core-workflows"
  | "enabling-capabilities"
  | "verification-map"
  | "non-goals"
  | "gate-decision"
  | "acceptance";

export type BusinessUserGateDocSection = {
  id: BusinessUserGateDocSectionId;
  index: number;
  title: string;
  docHeading: string;
};

export const BUSINESS_USER_GATE_DOC_SECTIONS: BusinessUserGateDocSection[] = [
  {
    id: "gate-statement",
    index: 0,
    title: "Gate statement",
    docHeading: "# Sprint 15 Production Gate",
  },
  {
    id: "without-developer-help",
    index: 1,
    title: "Without developer help",
    docHeading: "## What “without developer help” means",
  },
  {
    id: "core-workflows",
    index: 2,
    title: "Core workflows",
    docHeading: "## Core workflows in scope",
  },
  {
    id: "enabling-capabilities",
    index: 3,
    title: "Enabling capabilities",
    docHeading: "## Enabling product capabilities (Sprint 15)",
  },
  {
    id: "verification-map",
    index: 4,
    title: "Verification map",
    docHeading: "## Verification map (no developer tools)",
  },
  {
    id: "non-goals",
    index: 5,
    title: "Non-goals",
    docHeading: "## Explicit non-goals",
  },
  {
    id: "gate-decision",
    index: 6,
    title: "Gate decision",
    docHeading: "## Gate decision",
  },
  {
    id: "acceptance",
    index: 7,
    title: "Acceptance",
    docHeading: "## Acceptance (item 616)",
  },
];

export function businessUserGateDocSectionIdsInOrder(): BusinessUserGateDocSectionId[] {
  return BUSINESS_USER_GATE_DOC_SECTIONS.map((section) => section.id);
}

export function formatBusinessUserGateDocOutline(): string {
  return BUSINESS_USER_GATE_DOC_SECTIONS.map((section) => section.title).join(" → ");
}

export function isValidBusinessUserGateDocOrder(ids: string[]): boolean {
  const expected = businessUserGateDocSectionIdsInOrder();
  if (ids.length !== expected.length) {
    return false;
  }
  return ids.every((id, index) => id === expected[index]);
}

export function getBusinessUserWorkflowById(
  id: BusinessUserWorkflowId,
): BusinessUserWorkflowDefinition {
  const workflow = BUSINESS_USER_CORE_WORKFLOWS.find((candidate) => candidate.id === id);
  if (workflow == null) {
    throw new Error(`Unknown business user workflow id: ${id}`);
  }
  return workflow;
}

export function businessUserWorkflowIds(): BusinessUserWorkflowId[] {
  return BUSINESS_USER_CORE_WORKFLOWS.map((workflow) => workflow.id);
}

export function formatBusinessUserCoreJourney(
  workflows: readonly BusinessUserWorkflowDefinition[] = BUSINESS_USER_CORE_WORKFLOWS,
): string {
  return workflows
    .slice()
    .sort((left, right) => left.index - right.index)
    .map((workflow) => workflow.title)
    .join(" → ");
}

/** Tools business users must not need for core workflows. */
export const DEVELOPER_ONLY_TOOLS = [
  "IDE",
  "terminal",
  "SQL client",
  "curl",
  "Postman",
  "Flyway edit mid-task",
  "crafted JWT",
] as const;

/** Tools / surfaces allowed for business users. */
export const BUSINESS_USER_ALLOWED_SURFACES = [
  "browser UI",
  "role-filtered navigation",
  "page forms",
  "user guides",
  "on-screen validation",
  "demo employee accounts",
] as const;

/**
 * Gate readiness checks that can be evaluated without running the full suite.
 */
export type BusinessUserGateReadinessSnapshot = {
  allWorkflowsUiOnly: boolean;
  allWorkflowsHaveUserGuide: boolean;
  allWorkflowsHaveUiAcceptanceDoc: boolean;
  happyPathCoversCriticalJourney: boolean;
  documentationStatesGatePass: boolean;
};

export function evaluateBusinessUserGateReadiness(
  snapshot: BusinessUserGateReadinessSnapshot,
): { passed: boolean; failedChecks: string[] } {
  const failedChecks: string[] = [];
  if (!snapshot.allWorkflowsUiOnly) {
    failedChecks.push("all-workflows-ui-only");
  }
  if (!snapshot.allWorkflowsHaveUserGuide) {
    failedChecks.push("user-guides");
  }
  if (!snapshot.allWorkflowsHaveUiAcceptanceDoc) {
    failedChecks.push("ui-acceptance-docs");
  }
  if (!snapshot.happyPathCoversCriticalJourney) {
    failedChecks.push("happy-path-coverage");
  }
  if (!snapshot.documentationStatesGatePass) {
    failedChecks.push("gate-decision-pass");
  }
  return { passed: failedChecks.length === 0, failedChecks };
}

export function catalogAllWorkflowsUiOnly(
  workflows: readonly BusinessUserWorkflowDefinition[] = BUSINESS_USER_CORE_WORKFLOWS,
): boolean {
  return workflows.every((workflow) => workflow.uiOnly);
}

export function catalogIndexesAreSequential(
  workflows: readonly BusinessUserWorkflowDefinition[] = BUSINESS_USER_CORE_WORKFLOWS,
): boolean {
  return workflows.every((workflow, offset) => workflow.index === offset + 1);
}

export const BUSINESS_USER_GATE_DOC_REQUIRED_SNIPPETS = [
  BUSINESS_USER_WORKFLOW_GATE_TITLE,
  BUSINESS_USER_WORKFLOW_GATE_STATEMENT,
  "item 616",
  "item **616**",
  "NFR-005",
  "NFR-010",
  "NFR-011",
  ...BUSINESS_USER_GATE_DOC_SECTIONS.map((section) => section.docHeading),
  "without developer help",
  "Browser UI",
  "SQL",
  "/login",
  "/campaign-builder",
  "/compliance",
  "ui-login-flow.md",
  "ui-campaign-launch.md",
  "Production gate 616: PASS",
  "businessUserWorkflowGate.ts",
  "item **617**",
  "item **598**",
  "item **607**",
] as const;

export function docsIndexMustLinkBusinessUserGate(indexMarkdown: string): boolean {
  return (
    indexMarkdown.includes("agile/sprint-15-production-gate.md") ||
    indexMarkdown.includes("sprint-15-production-gate.md")
  );
}

export function documentationContainsRequiredSnippets(documentationMarkdown: string): boolean {
  return BUSINESS_USER_GATE_DOC_REQUIRED_SNIPPETS.every((snippet) =>
    documentationMarkdown.includes(snippet),
  );
}

export function documentationStatesGatePass(documentationMarkdown: string): boolean {
  return (
    documentationMarkdown.includes("Production gate 616: PASS") ||
    documentationMarkdown.includes("**Production gate 616: PASS")
  );
}
