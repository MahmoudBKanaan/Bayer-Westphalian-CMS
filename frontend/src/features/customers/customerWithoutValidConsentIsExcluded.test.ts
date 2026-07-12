import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_CRITICAL_TEST_CLASS,
  CUSTOMER_WITHOUT_VALID_CONSENT_IS_EXCLUDED_FR,
  CUSTOMER_WITHOUT_VALID_CONSENT_IS_EXCLUDED_ITEM,
  CUSTOMER_WITHOUT_VALID_CONSENT_IS_EXCLUDED_RULES,
  CUSTOMER_WITHOUT_VALID_CONSENT_IS_EXCLUDED_STATEMENT,
  ELIGIBILITY_RULES_DOC_PATH,
  INVALID_CONSENT_EXCLUSION_CODE,
  INVALID_CONSENT_EXCLUSION_EXPLANATION,
  RELATED_MARKETING_OPT_OUT_CODE,
  customerIsExcludedForMissingOrInvalidConsent,
  invalidConsentExclusionPresentation,
  invalidConsentIsKnownExclusionCode,
  isConsentRelatedExclusion,
  isInvalidConsentExclusion,
} from "@/features/customers/customerWithoutValidConsentIsExcluded";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("customerWithoutValidConsentIsExcluded (item 649)", () => {
  it("locks the critical KB rule identity", () => {
    expect(CUSTOMER_WITHOUT_VALID_CONSENT_IS_EXCLUDED_ITEM).toBe(649);
    expect(CUSTOMER_WITHOUT_VALID_CONSENT_IS_EXCLUDED_STATEMENT).toBe(
      "Customer without valid consent is excluded",
    );
    expect(CUSTOMER_WITHOUT_VALID_CONSENT_IS_EXCLUDED_RULES).toEqual(["BR-004"]);
    expect(CUSTOMER_WITHOUT_VALID_CONSENT_IS_EXCLUDED_FR).toEqual(["FR-034", "FR-055"]);
    expect(INVALID_CONSENT_EXCLUSION_CODE).toBe("INVALID_CONSENT");
    expect(INVALID_CONSENT_EXCLUSION_EXPLANATION).toContain("valid required consent");
    expect(RELATED_MARKETING_OPT_OUT_CODE).toBe("MARKETING_OPT_OUT");
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain("CustomerWithoutValidConsentIsExcludedTests");
  });

  it("treats missing valid consent and INVALID_CONSENT code as exclusions", () => {
    expect(
      customerIsExcludedForMissingOrInvalidConsent({ hasValidRequiredConsent: false }),
    ).toBe(true);
    expect(
      customerIsExcludedForMissingOrInvalidConsent({
        hasValidRequiredConsent: true,
        exclusionReasonCode: "INVALID_CONSENT",
      }),
    ).toBe(true);
    expect(
      customerIsExcludedForMissingOrInvalidConsent({
        hasValidRequiredConsent: true,
        exclusionReasonCode: null,
      }),
    ).toBe(false);
    expect(isInvalidConsentExclusion("INVALID_CONSENT")).toBe(true);
    expect(isInvalidConsentExclusion("DO_NOT_CONTACT")).toBe(false);
    expect(isConsentRelatedExclusion("INVALID_CONSENT")).toBe(true);
    expect(isConsentRelatedExclusion("MARKETING_OPT_OUT")).toBe(true);
    expect(invalidConsentIsKnownExclusionCode()).toBe(true);
  });

  it("presents INVALID_CONSENT with FR/BR consent rule hint", () => {
    const presentation = invalidConsentExclusionPresentation(2);
    expect(presentation.code).toBe("INVALID_CONSENT");
    expect(presentation.title).toMatch(/consent/i);
    expect(presentation.severity).toBe("warning");
    expect(presentation.ruleHint).toMatch(/FR-034|BR-003|consent/i);
    expect(presentation.count).toBe(2);
  });

  it("documents invalid consent exclusion in eligibility architecture docs", () => {
    const docPath = path.join(repoRoot, ELIGIBILITY_RULES_DOC_PATH);
    expect(existsSync(docPath)).toBe(true);
    const documentation = readRepoFile(ELIGIBILITY_RULES_DOC_PATH);
    expect(documentation).toContain("INVALID_CONSENT");
    expect(documentation).toContain("649");
    expect(documentation).toContain("CustomerWithoutValidConsentIsExcludedTests");
    expect(documentation).toMatch(/valid required consent|without valid consent/i);
    expect(documentation).toContain("FR-034");
  });
});
