import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  catalogIdsMatchExpectedOrder,
  countByDomain,
  docsIndexMustLinkNonFunctionalRequirementsMap,
  documentationContainsRequiredSnippets,
  everyMappingHasEvidence,
  EXPECTED_NON_FUNCTIONAL_REQUIREMENT_IDS,
  formatNonFunctionalRequirementsDocOutline,
  getNonFunctionalRequirementMapping,
  isValidNonFunctionalRequirementsDocSectionOrder,
  NON_FUNCTIONAL_REQUIREMENT_MAPPINGS,
  NON_FUNCTIONAL_REQUIREMENTS_DOC_REQUIRED_SNIPPETS,
  NON_FUNCTIONAL_REQUIREMENTS_DOC_SECTIONS,
  NON_FUNCTIONAL_REQUIREMENTS_RELATED_BACKLOG_ITEMS,
  NON_FUNCTIONAL_REQUIREMENTS_TEST_MAP_BACKLOG_ITEM,
  NON_FUNCTIONAL_REQUIREMENTS_TEST_MAP_DOC_PATH,
  NON_FUNCTIONAL_REQUIREMENTS_TEST_MAP_TITLE,
  nonFunctionalRequirementIds,
  nonFunctionalRequirementsDocSectionIdsInOrder,
  SECURITY_HARDENING_NFR_IDS,
  TESTABILITY_QUALITY_NFR_IDS,
  UX_ACCESSIBILITY_NFR_IDS,
} from "@/features/testing/nonFunctionalRequirementsTestMap";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("nonFunctionalRequirementsTestMap (item 622)", () => {
  it("documents the ordered non-functional requirements test map outline", () => {
    expect(nonFunctionalRequirementsDocSectionIdsInOrder()).toEqual([
      "purpose",
      "scope",
      "how-to-read",
      "security-privacy",
      "performance-availability",
      "usability-a11y",
      "engineering-qualities",
      "audit-reliability",
      "data-ops",
      "critical-crosswalk",
      "coverage-summary",
      "related-items",
      "acceptance",
    ]);
    expect(formatNonFunctionalRequirementsDocOutline()).toContain("Security and privacy");
    expect(formatNonFunctionalRequirementsDocOutline()).toContain("Acceptance");
    expect(
      isValidNonFunctionalRequirementsDocSectionOrder(
        nonFunctionalRequirementsDocSectionIdsInOrder(),
      ),
    ).toBe(true);
    expect(isValidNonFunctionalRequirementsDocSectionOrder(["purpose"])).toBe(false);
    expect(NON_FUNCTIONAL_REQUIREMENTS_DOC_SECTIONS).toHaveLength(13);
    expect(NON_FUNCTIONAL_REQUIREMENTS_TEST_MAP_TITLE).toBe(
      "Non-Functional Requirements Test Map",
    );
    expect(NON_FUNCTIONAL_REQUIREMENTS_TEST_MAP_BACKLOG_ITEM).toBe(622);
    expect(NON_FUNCTIONAL_REQUIREMENTS_DOC_REQUIRED_SNIPPETS).toContain("item **622**");
  });

  it("catalogues every KB non-functional requirement in order", () => {
    expect(NON_FUNCTIONAL_REQUIREMENT_MAPPINGS).toHaveLength(14);
    expect(EXPECTED_NON_FUNCTIONAL_REQUIREMENT_IDS).toHaveLength(14);
    expect(catalogIdsMatchExpectedOrder()).toBe(true);
    expect(nonFunctionalRequirementIds()[0]).toBe("NFR-001");
    expect(nonFunctionalRequirementIds().at(-1)).toBe("NFR-014");
    expect(getNonFunctionalRequirementMapping("NFR-001")?.name).toBe("Security");
    expect(getNonFunctionalRequirementMapping("NFR-011")?.domain).toBe("accessibility");
    expect(getNonFunctionalRequirementMapping("NFR-010")?.frontendTests).toContain(
      "src/features/testing/frontendTestingNotes.test.ts",
    );
  });

  it("requires documentation and automated evidence for each mapping", () => {
    expect(everyMappingHasEvidence()).toBe(true);
    const byDomain = countByDomain();
    for (const domain of Object.keys(byDomain) as (keyof typeof byDomain)[]) {
      expect(byDomain[domain], domain).toBe(1);
    }

    // Accessibility is frontend-primary.
    const a11y = getNonFunctionalRequirementMapping("NFR-011");
    expect(a11y?.backendTests).toHaveLength(0);
    expect(a11y!.frontendTests.length).toBeGreaterThan(0);

    // Backup/recovery is ops/docs + migration integrity (no backup UI product surface).
    const backup = getNonFunctionalRequirementMapping("NFR-013");
    expect(backup!.backendTests).toContain(
      "BackupAndRestoreProcessIsDocumentedAndTestableTests",
    );
    expect(backup!.frontendTests).toContain(
      "src/features/ops/backupAndRestoreProcessIsDocumentedAndTestable.test.ts",
    );
    expect(backup!.docs).toContain("docs/deployment/backup-and-restore.md");
    expect(backup!.backendTests.length).toBeGreaterThan(0);
    expect(backup!.docs.length).toBeGreaterThan(0);
  });

  it("groups security, UX, and testability NFR sets used by Sprint 16", () => {
    expect(SECURITY_HARDENING_NFR_IDS).toEqual(["NFR-001", "NFR-002", "NFR-014"]);
    expect(UX_ACCESSIBILITY_NFR_IDS).toEqual(["NFR-005", "NFR-011"]);
    expect(TESTABILITY_QUALITY_NFR_IDS).toEqual(["NFR-006", "NFR-010"]);

    expect(getNonFunctionalRequirementMapping("NFR-001")?.relatedSprint16Items).toContain(640);
    expect(getNonFunctionalRequirementMapping("NFR-011")?.relatedSprint16Items).toContain(638);
    expect(getNonFunctionalRequirementMapping("NFR-003")?.relatedSprint16Items).toContain(639);
    expect(getNonFunctionalRequirementMapping("NFR-013")?.relatedSprint16Items).toContain(666);

    expect(NON_FUNCTIONAL_REQUIREMENTS_RELATED_BACKLOG_ITEMS).toContain(622);
    expect(NON_FUNCTIONAL_REQUIREMENTS_RELATED_BACKLOG_ITEMS).toContain(620);
    expect(NON_FUNCTIONAL_REQUIREMENTS_RELATED_BACKLOG_ITEMS).toContain(621);
    expect(NON_FUNCTIONAL_REQUIREMENTS_RELATED_BACKLOG_ITEMS).toContain(674);
  });

  it("keeps the non-functional requirements test map markdown as delivery evidence", () => {
    const docPath = path.join(repoRoot, NON_FUNCTIONAL_REQUIREMENTS_TEST_MAP_DOC_PATH);
    expect(existsSync(docPath), `Missing ${NON_FUNCTIONAL_REQUIREMENTS_TEST_MAP_DOC_PATH}`).toBe(
      true,
    );

    const documentation = readRepoFile(NON_FUNCTIONAL_REQUIREMENTS_TEST_MAP_DOC_PATH);
    expect(documentationContainsRequiredSnippets(documentation)).toBe(true);
    expect(documentation).toContain("## Security and privacy (NFR-001–NFR-002)");
    expect(documentation).toContain(
      "## Data integrity, backup, and observability (NFR-012–NFR-014)",
    );
    expect(documentation).toContain("## Critical and run-item crosswalk");
    expect(documentation).toContain("## Acceptance (item 622)");
    expect(documentation).toContain("nonFunctionalRequirementsTestMap.ts");
    expect(documentation).toContain("do not run any tests");

    for (const id of EXPECTED_NON_FUNCTIONAL_REQUIREMENT_IDS) {
      expect(documentation, `doc missing ${id}`).toContain(id);
    }
  });

  it("links the map from the documentation index", () => {
    const index = readRepoFile("docs/README.md");
    expect(docsIndexMustLinkNonFunctionalRequirementsMap(index)).toBe(true);
  });
});
