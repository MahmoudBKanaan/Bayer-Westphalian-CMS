import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  catalogIdsMatchExpectedOrder,
  countByDomain,
  docsIndexMustLinkFunctionalRequirementsMap,
  documentationContainsRequiredSnippets,
  everyMappingHasEvidence,
  EXPECTED_FUNCTIONAL_REQUIREMENT_IDS,
  formatFunctionalRequirementsDocOutline,
  FUNCTIONAL_REQUIREMENT_MAPPINGS,
  FUNCTIONAL_REQUIREMENTS_DOC_REQUIRED_SNIPPETS,
  FUNCTIONAL_REQUIREMENTS_DOC_SECTIONS,
  FUNCTIONAL_REQUIREMENTS_RELATED_BACKLOG_ITEMS,
  FUNCTIONAL_REQUIREMENTS_TEST_MAP_BACKLOG_ITEM,
  FUNCTIONAL_REQUIREMENTS_TEST_MAP_DOC_PATH,
  FUNCTIONAL_REQUIREMENTS_TEST_MAP_TITLE,
  functionalRequirementIds,
  functionalRequirementsDocSectionIdsInOrder,
  getFunctionalRequirementMapping,
  HAPPY_PATH_FUNCTIONAL_REQUIREMENT_IDS,
  isValidFunctionalRequirementsDocSectionOrder,
} from "@/features/testing/functionalRequirementsTestMap";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("functionalRequirementsTestMap (item 620)", () => {
  it("documents the ordered functional requirements test map outline", () => {
    expect(functionalRequirementsDocSectionIdsInOrder()).toEqual([
      "purpose",
      "scope",
      "how-to-read",
      "auth-rbac",
      "customer",
      "beneficiary-consent",
      "product",
      "campaign",
      "segmentation",
      "reminders",
      "communication-followup",
      "analytics-reports",
      "ai-assist",
      "coverage-summary",
      "related-items",
      "acceptance",
    ]);
    expect(formatFunctionalRequirementsDocOutline()).toContain("Campaign lifecycle");
    expect(formatFunctionalRequirementsDocOutline()).toContain("Acceptance");
    expect(
      isValidFunctionalRequirementsDocSectionOrder(functionalRequirementsDocSectionIdsInOrder()),
    ).toBe(true);
    expect(isValidFunctionalRequirementsDocSectionOrder(["purpose"])).toBe(false);
    expect(FUNCTIONAL_REQUIREMENTS_DOC_SECTIONS).toHaveLength(16);
    expect(FUNCTIONAL_REQUIREMENTS_TEST_MAP_TITLE).toBe("Functional Requirements Test Map");
    expect(FUNCTIONAL_REQUIREMENTS_TEST_MAP_BACKLOG_ITEM).toBe(620);
    expect(FUNCTIONAL_REQUIREMENTS_DOC_REQUIRED_SNIPPETS).toContain("item **620**");
  });

  it("catalogues every KB FR and AI-assisted functional requirement in order", () => {
    expect(FUNCTIONAL_REQUIREMENT_MAPPINGS).toHaveLength(86);
    expect(EXPECTED_FUNCTIONAL_REQUIREMENT_IDS).toHaveLength(86);
    expect(catalogIdsMatchExpectedOrder()).toBe(true);
    expect(functionalRequirementIds()[0]).toBe("FR-001");
    expect(functionalRequirementIds().at(-1)).toBe("AI-006");
    expect(getFunctionalRequirementMapping("FR-059")?.statement).toContain("approve/reject");
    expect(getFunctionalRequirementMapping("FR-034")?.domain).toBe("beneficiary-consent");
    expect(getFunctionalRequirementMapping("AI-005")?.backendTests).toContain(
      "AiSupportsHumanDecisionMakingOnlyTests",
    );
  });

  it("requires backend, frontend, and documentation evidence for each mapping", () => {
    expect(everyMappingHasEvidence()).toBe(true);
    const byDomain = countByDomain();
    expect(byDomain["auth-rbac"]).toBe(5);
    expect(byDomain.customer).toBe(11);
    expect(byDomain["beneficiary-consent"]).toBe(5);
    expect(byDomain.product).toBe(7);
    expect(byDomain.campaign).toBe(13);
    expect(byDomain.segmentation).toBe(10);
    expect(byDomain.reminders).toBe(10);
    expect(byDomain["communication-followup"]).toBe(8);
    expect(byDomain["analytics-reports"]).toBe(11);
    expect(byDomain["ai-assist"]).toBe(6);
  });

  it("aligns happy-path IDs with the critical business journey", () => {
    expect(HAPPY_PATH_FUNCTIONAL_REQUIREMENT_IDS).toEqual([
      "FR-001",
      "FR-011",
      "FR-033",
      "FR-041",
      "FR-077",
      "FR-050",
      "FR-059",
      "FR-060",
      "FR-100",
    ]);
    for (const id of HAPPY_PATH_FUNCTIONAL_REQUIREMENT_IDS) {
      const mapping = getFunctionalRequirementMapping(id);
      expect(mapping, `missing mapping for ${id}`).toBeDefined();
      expect(
        mapping!.frontendTests.some(
          (t) => t.includes("e2e/") || t.includes("happyPath") || t.includes("Flow"),
        ),
      ).toBe(true);
    }
    expect(FUNCTIONAL_REQUIREMENTS_RELATED_BACKLOG_ITEMS).toContain(620);
    expect(FUNCTIONAL_REQUIREMENTS_RELATED_BACKLOG_ITEMS).toContain(621);
    expect(FUNCTIONAL_REQUIREMENTS_RELATED_BACKLOG_ITEMS).toContain(670);
  });

  it("keeps the functional requirements test map markdown as delivery evidence", () => {
    const docPath = path.join(repoRoot, FUNCTIONAL_REQUIREMENTS_TEST_MAP_DOC_PATH);
    expect(existsSync(docPath), `Missing ${FUNCTIONAL_REQUIREMENTS_TEST_MAP_DOC_PATH}`).toBe(
      true,
    );

    const documentation = readRepoFile(FUNCTIONAL_REQUIREMENTS_TEST_MAP_DOC_PATH);
    expect(documentationContainsRequiredSnippets(documentation)).toBe(true);
    expect(documentation).toContain("## Auth and RBAC (FR-001–FR-005)");
    expect(documentation).toContain("## Campaign lifecycle (FR-050–FR-062)");
    expect(documentation).toContain("## AI-assisted features (AI-001–AI-006)");
    expect(documentation).toContain("## Acceptance (item 620)");
    expect(documentation).toContain("functionalRequirementsTestMap.ts");
    expect(documentation).toContain("do not run any tests");

    for (const id of EXPECTED_FUNCTIONAL_REQUIREMENT_IDS) {
      expect(documentation, `doc missing ${id}`).toContain(id);
    }
  });

  it("links the map from the documentation index", () => {
    const index = readRepoFile("docs/README.md");
    expect(docsIndexMustLinkFunctionalRequirementsMap(index)).toBe(true);
  });
});
