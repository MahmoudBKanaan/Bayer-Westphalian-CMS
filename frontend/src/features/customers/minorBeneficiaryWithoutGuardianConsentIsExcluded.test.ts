import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_CRITICAL_TEST_CLASS,
  BENEFICIARY_MODULE_DOC_PATH,
  ELIGIBILITY_RULES_DOC_PATH,
  GUARDIAN_CONSENT_EXCLUSION_CODE,
  GUARDIAN_CONSENT_EXCLUSION_EXPLANATION,
  GUARDIAN_CONSENT_TYPE,
  MINOR_BENEFICIARY_WITHOUT_GUARDIAN_CONSENT_IS_EXCLUDED_FR,
  MINOR_BENEFICIARY_WITHOUT_GUARDIAN_CONSENT_IS_EXCLUDED_ITEM,
  MINOR_BENEFICIARY_WITHOUT_GUARDIAN_CONSENT_IS_EXCLUDED_RULES,
  MINOR_BENEFICIARY_WITHOUT_GUARDIAN_CONSENT_IS_EXCLUDED_STATEMENT,
  guardianFailureExclusionPresentation,
  guardianRuleHintMentionsConsent,
  invalidConsentIsKnownForGuardianPath,
  minorBeneficiaryIsExcludedWithoutGuardianConsent,
} from "@/features/customers/minorBeneficiaryWithoutGuardianConsentIsExcluded";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("minorBeneficiaryWithoutGuardianConsentIsExcluded (item 650)", () => {
  it("locks the critical KB rule identity", () => {
    expect(MINOR_BENEFICIARY_WITHOUT_GUARDIAN_CONSENT_IS_EXCLUDED_ITEM).toBe(650);
    expect(MINOR_BENEFICIARY_WITHOUT_GUARDIAN_CONSENT_IS_EXCLUDED_STATEMENT).toBe(
      "Minor beneficiary without guardian consent is excluded",
    );
    expect(MINOR_BENEFICIARY_WITHOUT_GUARDIAN_CONSENT_IS_EXCLUDED_RULES).toEqual(["BR-003"]);
    expect(MINOR_BENEFICIARY_WITHOUT_GUARDIAN_CONSENT_IS_EXCLUDED_FR).toEqual([
      "FR-032",
      "FR-034",
    ]);
    expect(GUARDIAN_CONSENT_EXCLUSION_CODE).toBe("INVALID_CONSENT");
    expect(GUARDIAN_CONSENT_EXCLUSION_EXPLANATION).toContain("valid required consent");
    expect(GUARDIAN_CONSENT_TYPE).toBe("GUARDIAN");
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain(
      "MinorBeneficiaryWithoutGuardianConsentIsExcludedTests",
    );
  });

  it("excludes when guardian consent is required but not valid", () => {
    expect(
      minorBeneficiaryIsExcludedWithoutGuardianConsent({
        guardianConsentRequired: true,
        hasValidGuardianConsent: false,
      }),
    ).toBe(true);
    expect(
      minorBeneficiaryIsExcludedWithoutGuardianConsent({
        guardianConsentRequired: true,
        hasValidGuardianConsent: true,
      }),
    ).toBe(false);
    expect(
      minorBeneficiaryIsExcludedWithoutGuardianConsent({
        guardianConsentRequired: false,
        hasValidGuardianConsent: false,
      }),
    ).toBe(false);
    expect(
      minorBeneficiaryIsExcludedWithoutGuardianConsent({
        guardianConsentRequired: true,
        hasValidGuardianConsent: true,
        exclusionReasonCode: "INVALID_CONSENT",
      }),
    ).toBe(true);
    expect(invalidConsentIsKnownForGuardianPath()).toBe(true);
    expect(guardianRuleHintMentionsConsent()).toBe(true);
  });

  it("presents guardian failure using INVALID_CONSENT UI metadata", () => {
    const presentation = guardianFailureExclusionPresentation(1);
    expect(presentation.code).toBe("INVALID_CONSENT");
    expect(presentation.title).toMatch(/consent/i);
    expect(presentation.ruleHint).toMatch(/consent|guardian|FR-034|BR-003/i);
  });

  it("documents guardian consent requirement in eligibility and beneficiary docs", () => {
    const eligibility = readRepoFile(ELIGIBILITY_RULES_DOC_PATH);
    expect(existsSync(path.join(repoRoot, ELIGIBILITY_RULES_DOC_PATH))).toBe(true);
    expect(eligibility).toContain("650");
    expect(eligibility).toContain("MinorBeneficiaryWithoutGuardianConsentIsExcludedTests");
    expect(eligibility).toContain("BR-003");
    expect(eligibility).toMatch(/guardian/i);
    expect(eligibility).toContain("INVALID_CONSENT");

    const beneficiary = readRepoFile(BENEFICIARY_MODULE_DOC_PATH);
    expect(existsSync(path.join(repoRoot, BENEFICIARY_MODULE_DOC_PATH))).toBe(true);
    expect(beneficiary).toMatch(/guardianConsentRequired|guardian consent/i);
  });
});
