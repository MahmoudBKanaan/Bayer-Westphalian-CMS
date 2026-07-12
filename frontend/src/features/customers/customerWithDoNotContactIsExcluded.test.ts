import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_CRITICAL_TEST_CLASS,
  CUSTOMER_WITH_DO_NOT_CONTACT_IS_EXCLUDED_FR,
  CUSTOMER_WITH_DO_NOT_CONTACT_IS_EXCLUDED_ITEM,
  CUSTOMER_WITH_DO_NOT_CONTACT_IS_EXCLUDED_RULES,
  CUSTOMER_WITH_DO_NOT_CONTACT_IS_EXCLUDED_STATEMENT,
  DO_NOT_CONTACT_EVALUATION_ORDER_POSITION,
  DO_NOT_CONTACT_EXCLUSION_CODE,
  DO_NOT_CONTACT_EXCLUSION_EXPLANATION,
  ELIGIBILITY_RULES_DOC_PATH,
  customerIsExcludedFromCampaignAudience,
  dncIsKnownCriticalExclusionCode,
  doNotContactExclusionPresentation,
  isDoNotContactExclusion,
} from "@/features/customers/customerWithDoNotContactIsExcluded";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("customerWithDoNotContactIsExcluded (item 648)", () => {
  it("locks the critical KB rule identity", () => {
    expect(CUSTOMER_WITH_DO_NOT_CONTACT_IS_EXCLUDED_ITEM).toBe(648);
    expect(CUSTOMER_WITH_DO_NOT_CONTACT_IS_EXCLUDED_STATEMENT).toBe(
      "Customer with do_not_contact is excluded",
    );
    expect(CUSTOMER_WITH_DO_NOT_CONTACT_IS_EXCLUDED_RULES).toEqual(["BR-001"]);
    expect(CUSTOMER_WITH_DO_NOT_CONTACT_IS_EXCLUDED_FR).toEqual(["FR-097", "FR-055"]);
    expect(DO_NOT_CONTACT_EXCLUSION_CODE).toBe("DO_NOT_CONTACT");
    expect(DO_NOT_CONTACT_EXCLUSION_EXPLANATION).toContain("do-not-contact");
    expect(DO_NOT_CONTACT_EVALUATION_ORDER_POSITION).toBe(1);
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain("CustomerWithDoNotContactIsExcludedTests");
  });

  it("treats doNotContact flag and DO_NOT_CONTACT code as audience exclusions", () => {
    expect(customerIsExcludedFromCampaignAudience({ doNotContact: true })).toBe(true);
    expect(
      customerIsExcludedFromCampaignAudience({
        doNotContact: false,
        exclusionReasonCode: "DO_NOT_CONTACT",
      }),
    ).toBe(true);
    expect(
      customerIsExcludedFromCampaignAudience({
        doNotContact: false,
        exclusionReasonCode: null,
      }),
    ).toBe(false);
    expect(isDoNotContactExclusion("DO_NOT_CONTACT")).toBe(true);
    expect(isDoNotContactExclusion("MARKETING_OPT_OUT")).toBe(false);
    expect(dncIsKnownCriticalExclusionCode()).toBe(true);
  });

  it("presents DO_NOT_CONTACT with critical severity and BR-001 hint", () => {
    const presentation = doNotContactExclusionPresentation(3);
    expect(presentation.code).toBe("DO_NOT_CONTACT");
    expect(presentation.title).toMatch(/do not contact/i);
    expect(presentation.severity).toBe("critical");
    expect(presentation.ruleHint).toContain("BR-001");
    expect(presentation.count).toBe(3);
  });

  it("documents DNC as first eligibility gate in architecture docs", () => {
    const docPath = path.join(repoRoot, ELIGIBILITY_RULES_DOC_PATH);
    expect(existsSync(docPath)).toBe(true);
    const documentation = readRepoFile(ELIGIBILITY_RULES_DOC_PATH);
    expect(documentation).toContain("doNotContact");
    expect(documentation).toContain("DO_NOT_CONTACT");
    expect(documentation).toContain("BR-001");
    expect(documentation).toContain("648");
    expect(documentation).toContain("CustomerWithDoNotContactIsExcludedTests");
    expect(documentation).toMatch(/doNotContact\s*=\s*true|do_not_contact/i);
  });
});
