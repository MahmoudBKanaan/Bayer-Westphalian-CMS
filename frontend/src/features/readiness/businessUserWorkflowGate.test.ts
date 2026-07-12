import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BUSINESS_USER_ALLOWED_SURFACES,
  BUSINESS_USER_CORE_WORKFLOWS,
  BUSINESS_USER_GATE_DOC_REQUIRED_SNIPPETS,
  BUSINESS_USER_GATE_DOC_SECTIONS,
  BUSINESS_USER_WORKFLOW_GATE_DOC_PATH,
  BUSINESS_USER_WORKFLOW_GATE_STATEMENT,
  BUSINESS_USER_WORKFLOW_GATE_TITLE,
  businessUserGateDocSectionIdsInOrder,
  businessUserWorkflowIds,
  catalogAllWorkflowsUiOnly,
  catalogIndexesAreSequential,
  DEVELOPER_ONLY_TOOLS,
  documentationContainsRequiredSnippets,
  documentationStatesGatePass,
  docsIndexMustLinkBusinessUserGate,
  evaluateBusinessUserGateReadiness,
  formatBusinessUserCoreJourney,
  formatBusinessUserGateDocOutline,
  getBusinessUserWorkflowById,
  isValidBusinessUserGateDocOrder,
} from "@/features/readiness/businessUserWorkflowGate";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("businessUserWorkflowGate (item 616)", () => {
  it("documents the production gate outline and statement", () => {
    expect(businessUserGateDocSectionIdsInOrder()).toEqual([
      "gate-statement",
      "without-developer-help",
      "core-workflows",
      "enabling-capabilities",
      "verification-map",
      "non-goals",
      "gate-decision",
      "acceptance",
    ]);
    expect(formatBusinessUserGateDocOutline()).toContain("Core workflows");
    expect(isValidBusinessUserGateDocOrder(businessUserGateDocSectionIdsInOrder())).toBe(true);
    expect(isValidBusinessUserGateDocOrder(["gate-statement"])).toBe(false);
    expect(BUSINESS_USER_GATE_DOC_SECTIONS).toHaveLength(8);
    expect(BUSINESS_USER_WORKFLOW_GATE_TITLE).toBe("Sprint 15 Production Gate");
    expect(BUSINESS_USER_WORKFLOW_GATE_STATEMENT).toBe(
      "A business user should be able to complete core workflows without developer help.",
    );
  });

  it("catalogues nine UI-only core workflows in order", () => {
    expect(BUSINESS_USER_CORE_WORKFLOWS).toHaveLength(9);
    expect(catalogIndexesAreSequential()).toBe(true);
    expect(catalogAllWorkflowsUiOnly()).toBe(true);
    expect(businessUserWorkflowIds()).toEqual([
      "login",
      "customer-create",
      "consent-update",
      "product-create",
      "segment-create",
      "campaign-create",
      "compliance-approval",
      "campaign-launch",
      "dashboard-analytics",
    ]);
    expect(formatBusinessUserCoreJourney()).toBe(
      "Sign in → Create customer → Record consent → Create product → Create segment → Create and submit campaign → Approve campaign → Launch campaign → View dashboard analytics",
    );
    expect(getBusinessUserWorkflowById("campaign-create").route).toBe("/campaign-builder");
    expect(getBusinessUserWorkflowById("compliance-approval").uiBacklogItem).toBe(604);
  });

  it("defines allowed business surfaces versus developer-only tools", () => {
    expect(BUSINESS_USER_ALLOWED_SURFACES).toContain("browser UI");
    expect(BUSINESS_USER_ALLOWED_SURFACES).toContain("user guides");
    expect(DEVELOPER_ONLY_TOOLS).toContain("SQL client");
    expect(DEVELOPER_ONLY_TOOLS).toContain("terminal");
    expect(DEVELOPER_ONLY_TOOLS).toContain("curl");
  });

  it("evaluates gate readiness snapshots", () => {
    const pass = evaluateBusinessUserGateReadiness({
      allWorkflowsUiOnly: true,
      allWorkflowsHaveUserGuide: true,
      allWorkflowsHaveUiAcceptanceDoc: true,
      happyPathCoversCriticalJourney: true,
      documentationStatesGatePass: true,
    });
    expect(pass.passed).toBe(true);
    expect(pass.failedChecks).toEqual([]);

    const fail = evaluateBusinessUserGateReadiness({
      allWorkflowsUiOnly: true,
      allWorkflowsHaveUserGuide: false,
      allWorkflowsHaveUiAcceptanceDoc: true,
      happyPathCoversCriticalJourney: true,
      documentationStatesGatePass: false,
    });
    expect(fail.passed).toBe(false);
    expect(fail.failedChecks).toContain("user-guides");
    expect(fail.failedChecks).toContain("gate-decision-pass");
  });

  it("keeps the production gate markdown as delivery evidence", () => {
    const docPath = path.join(repoRoot, BUSINESS_USER_WORKFLOW_GATE_DOC_PATH);
    expect(existsSync(docPath), `Missing ${BUSINESS_USER_WORKFLOW_GATE_DOC_PATH}`).toBe(true);

    const documentation = readRepoFile(BUSINESS_USER_WORKFLOW_GATE_DOC_PATH);
    expect(documentationContainsRequiredSnippets(documentation)).toBe(true);
    expect(documentationStatesGatePass(documentation)).toBe(true);
    expect(documentation).toContain(BUSINESS_USER_WORKFLOW_GATE_STATEMENT);
    expect(documentation).toContain("## Acceptance (item 616)");
    expect(BUSINESS_USER_GATE_DOC_REQUIRED_SNIPPETS).toContain("item **616**");
  });

  it("links the production gate from the documentation index", () => {
    const index = readRepoFile("docs/README.md");
    expect(docsIndexMustLinkBusinessUserGate(index)).toBe(true);
    expect(index).toContain("agile/sprint-15-production-gate.md");
  });

  it("requires user guides and UI acceptance docs for every core workflow", () => {
    for (const workflow of BUSINESS_USER_CORE_WORKFLOWS) {
      expect(
        existsSync(path.join(repoRoot, workflow.userGuidePath)),
        workflow.userGuidePath,
      ).toBe(true);
      expect(
        existsSync(path.join(repoRoot, workflow.uiAcceptanceDocPath)),
        workflow.uiAcceptanceDocPath,
      ).toBe(true);
    }
  });

  it("requires happy-path and screenshot evidence docs that support the gate", () => {
    for (const relative of [
      "docs/testing/playwright-e2e.md",
      "docs/testing/core-workflow-screenshots.md",
      "docs/testing/frontend-testing-notes.md",
      "docs/testing/ui-role-based-menu.md",
    ]) {
      expect(existsSync(path.join(repoRoot, relative)), relative).toBe(true);
    }
  });

  it("rejects incomplete documentation snapshots", () => {
    expect(documentationContainsRequiredSnippets("# incomplete")).toBe(false);
    expect(documentationStatesGatePass("# no gate")).toBe(false);
    expect(docsIndexMustLinkBusinessUserGate("# Documentation\n")).toBe(false);
  });
});
