/**
 * Sprint 16 critical test item **650**: Minor beneficiary without guardian consent is excluded.
 *
 * KB: BR-003, FR-032, FR-034. When a beneficiary link requires guardian consent and no valid
 * GUARDIAN consent exists, EligibilityService excludes with INVALID_CONSENT.
 */

import {
  exclusionReasonRuleHint,
  exclusionReasonSeverity,
  formatExclusionReasonTitle,
  isKnownExclusionReasonCode,
} from "@/features/segments/exclusionReasons";

export const MINOR_BENEFICIARY_WITHOUT_GUARDIAN_CONSENT_IS_EXCLUDED_ITEM = 650;

export const MINOR_BENEFICIARY_WITHOUT_GUARDIAN_CONSENT_IS_EXCLUDED_STATEMENT =
  "Minor beneficiary without guardian consent is excluded";

export const MINOR_BENEFICIARY_WITHOUT_GUARDIAN_CONSENT_IS_EXCLUDED_RULES = ["BR-003"] as const;

export const MINOR_BENEFICIARY_WITHOUT_GUARDIAN_CONSENT_IS_EXCLUDED_FR = [
  "FR-032",
  "FR-034",
] as const;

/** Exclusion reason code used when guardian consent is required but not valid. */
export const GUARDIAN_CONSENT_EXCLUSION_CODE = "INVALID_CONSENT" as const;

export const GUARDIAN_CONSENT_EXCLUSION_EXPLANATION =
  "Customer does not have valid required consent";

export const GUARDIAN_CONSENT_TYPE = "GUARDIAN" as const;

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.campaign.MinorBeneficiaryWithoutGuardianConsentIsExcludedTests";

export const ELIGIBILITY_RULES_DOC_PATH = "docs/architecture/eligibility-rules.md";

export const BENEFICIARY_MODULE_DOC_PATH = "docs/modules/beneficiary-module.md";

/**
 * True when a beneficiary row requires guardian consent and that consent is not present/valid.
 */
export function minorBeneficiaryIsExcludedWithoutGuardianConsent(options: {
  guardianConsentRequired: boolean;
  hasValidGuardianConsent: boolean;
  exclusionReasonCode?: string | null;
}): boolean {
  if (options.guardianConsentRequired && !options.hasValidGuardianConsent) {
    return true;
  }
  return (
    options.guardianConsentRequired &&
    options.exclusionReasonCode === GUARDIAN_CONSENT_EXCLUSION_CODE
  );
}

export function guardianFailureExclusionPresentation(count = 1): {
  code: string;
  title: string;
  severity: "critical" | "warning" | "info";
  ruleHint: string;
  count: number;
} {
  return {
    code: GUARDIAN_CONSENT_EXCLUSION_CODE,
    title: formatExclusionReasonTitle(GUARDIAN_CONSENT_EXCLUSION_CODE),
    severity: exclusionReasonSeverity(GUARDIAN_CONSENT_EXCLUSION_CODE),
    ruleHint: exclusionReasonRuleHint(GUARDIAN_CONSENT_EXCLUSION_CODE),
    count,
  };
}

export function guardianRuleHintMentionsConsent(): boolean {
  const hint = exclusionReasonRuleHint(GUARDIAN_CONSENT_EXCLUSION_CODE);
  return /guardian|consent|FR-034|BR-003/i.test(hint);
}

export function invalidConsentIsKnownForGuardianPath(): boolean {
  return isKnownExclusionReasonCode(GUARDIAN_CONSENT_EXCLUSION_CODE);
}
