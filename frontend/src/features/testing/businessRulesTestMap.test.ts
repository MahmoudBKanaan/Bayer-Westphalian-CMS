import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BUSINESS_RULE_MAPPINGS,
  BUSINESS_RULES_DOC_REQUIRED_SNIPPETS,
  BUSINESS_RULES_DOC_SECTIONS,
  BUSINESS_RULES_RELATED_BACKLOG_ITEMS,
  BUSINESS_RULES_TEST_MAP_BACKLOG_ITEM,
  BUSINESS_RULES_TEST_MAP_DOC_PATH,
  BUSINESS_RULES_TEST_MAP_TITLE,
  businessRuleIds,
  businessRulesDocSectionIdsInOrder,
  catalogIdsMatchExpectedOrder,
  countByDomain,
  docsIndexMustLinkBusinessRulesMap,
  documentationContainsRequiredSnippets,
  ELIGIBILITY_GATE_BUSINESS_RULE_IDS,
  everyMappingHasEvidence,
  EXPECTED_BUSINESS_RULE_IDS,
  formatBusinessRulesDocOutline,
  getBusinessRuleMapping,
  isValidBusinessRulesDocSectionOrder,
  LAUNCH_GATE_BUSINESS_RULE_IDS,
  mappingsWithCriticalTestItems,
} from "@/features/testing/businessRulesTestMap";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("businessRulesTestMap (item 621)", () => {
  it("documents the ordered business rules test map outline", () => {
    expect(businessRulesDocSectionIdsInOrder()).toEqual([
      "purpose",
      "scope",
      "how-to-read",
      "eligibility-consent",
      "campaign-compliance",
      "contact-frequency",
      "reminders-payments",
      "campaign-lifecycle",
      "metrics",
      "critical-crosswalk",
      "coverage-summary",
      "related-items",
      "acceptance",
    ]);
    expect(formatBusinessRulesDocOutline()).toContain("Eligibility and consent");
    expect(formatBusinessRulesDocOutline()).toContain("Acceptance");
    expect(isValidBusinessRulesDocSectionOrder(businessRulesDocSectionIdsInOrder())).toBe(true);
    expect(isValidBusinessRulesDocSectionOrder(["purpose"])).toBe(false);
    expect(BUSINESS_RULES_DOC_SECTIONS).toHaveLength(13);
    expect(BUSINESS_RULES_TEST_MAP_TITLE).toBe("Business Rules Test Map");
    expect(BUSINESS_RULES_TEST_MAP_BACKLOG_ITEM).toBe(621);
    expect(BUSINESS_RULES_DOC_REQUIRED_SNIPPETS).toContain("item **621**");
  });

  it("catalogues every KB business rule in order", () => {
    expect(BUSINESS_RULE_MAPPINGS).toHaveLength(22);
    expect(EXPECTED_BUSINESS_RULE_IDS).toHaveLength(22);
    expect(catalogIdsMatchExpectedOrder()).toBe(true);
    expect(businessRuleIds()[0]).toBe("BR-001");
    expect(businessRuleIds().at(-1)).toBe("BR-034");
    expect(getBusinessRuleMapping("BR-005")?.statement).toContain("Compliance Officer approval");
    expect(getBusinessRuleMapping("BR-001")?.domain).toBe("eligibility-consent");
    expect(getBusinessRuleMapping("BR-024")?.backendTests).toContain(
      "PaymentReminderIsNotSentIfPaymentIsCompletedTests",
    );
    expect(getBusinessRuleMapping("BR-024")?.backendTests).toContain(
      "PaymentReminderNotSentIfPaymentCompletedTests",
    );
  });

  it("requires backend, frontend, and documentation evidence for each mapping", () => {
    expect(everyMappingHasEvidence()).toBe(true);
    const byDomain = countByDomain();
    expect(byDomain["eligibility-consent"]).toBe(4);
    expect(byDomain["campaign-compliance"]).toBe(3);
    expect(byDomain["contact-frequency"]).toBe(5);
    expect(byDomain["reminders-payments"]).toBe(5);
    expect(byDomain["campaign-lifecycle"]).toBe(4);
    expect(byDomain.metrics).toBe(1);
  });

  it("links critical Sprint 16 tests and gate rule sets", () => {
    const withCritical = mappingsWithCriticalTestItems();
    expect(withCritical.length).toBeGreaterThanOrEqual(8);
    expect(getBusinessRuleMapping("BR-001")?.criticalTestItem).toBe(648);
    expect(getBusinessRuleMapping("BR-003")?.criticalTestItem).toBe(650);
    expect(getBusinessRuleMapping("BR-010")?.criticalTestItem).toBe(651);
    expect(getBusinessRuleMapping("BR-011")?.criticalTestItem).toBe(652);
    expect(getBusinessRuleMapping("BR-004")?.criticalTestItem).toBe(649);
    expect(getBusinessRuleMapping("BR-005")?.criticalTestItem).toBe(647);
    expect(getBusinessRuleMapping("BR-032")?.criticalTestItem).toBe(647);
    expect(getBusinessRuleMapping("BR-024")?.criticalTestItem).toBe(660);
    expect(getBusinessRuleMapping("BR-034")?.criticalTestItem).toBe(656);
    expect(getBusinessRuleMapping("BR-034")?.backendTests).toContain(
      "ContactEventsUpdateAnalyticsTests",
    );

    expect(ELIGIBILITY_GATE_BUSINESS_RULE_IDS).toEqual([
      "BR-001",
      "BR-002",
      "BR-003",
      "BR-010",
      "BR-011",
      "BR-013",
      "BR-014",
    ]);
    expect(LAUNCH_GATE_BUSINESS_RULE_IDS).toEqual(["BR-005", "BR-032"]);
    for (const id of ELIGIBILITY_GATE_BUSINESS_RULE_IDS) {
      expect(getBusinessRuleMapping(id)?.backendTests).toContain("EligibilityServiceTests");
    }

    expect(BUSINESS_RULES_RELATED_BACKLOG_ITEMS).toContain(621);
    expect(BUSINESS_RULES_RELATED_BACKLOG_ITEMS).toContain(620);
    expect(BUSINESS_RULES_RELATED_BACKLOG_ITEMS).toContain(674);
  });

  it("keeps the business rules test map markdown as delivery evidence", () => {
    const docPath = path.join(repoRoot, BUSINESS_RULES_TEST_MAP_DOC_PATH);
    expect(existsSync(docPath), `Missing ${BUSINESS_RULES_TEST_MAP_DOC_PATH}`).toBe(true);

    const documentation = readRepoFile(BUSINESS_RULES_TEST_MAP_DOC_PATH);
    expect(documentationContainsRequiredSnippets(documentation)).toBe(true);
    expect(documentation).toContain("## Eligibility and consent (BR-001–BR-004)");
    expect(documentation).toContain("## Campaign lifecycle constraints (BR-030–BR-033)");
    expect(documentation).toContain("## Critical test crosswalk (items 647–665)");
    expect(documentation).toContain("## Acceptance (item 621)");
    expect(documentation).toContain("businessRulesTestMap.ts");
    expect(documentation).toContain("do not run any tests");

    for (const id of EXPECTED_BUSINESS_RULE_IDS) {
      expect(documentation, `doc missing ${id}`).toContain(id);
    }
  });

  it("links the map from the documentation index", () => {
    const index = readRepoFile("docs/README.md");
    expect(docsIndexMustLinkBusinessRulesMap(index)).toBe(true);
  });
});
