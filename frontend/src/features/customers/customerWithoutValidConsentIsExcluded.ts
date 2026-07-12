/**
 * Sprint 16 critical test item **649**: Customer without valid consent is excluded.
 *
 * KB: FR-034, FR-055, BR-004. UI surfaces `INVALID_CONSENT`; backend EligibilityService is
 * authoritative. Withdrawn marketing consent uses `MARKETING_OPT_OUT` (BR-002) and also blocks.
 */

import {
  exclusionReasonRuleHint,
  exclusionReasonSeverity,
  formatExclusionReasonTitle,
  isKnownExclusionReasonCode,
} from "@/features/segments/exclusionReasons";

export const CUSTOMER_WITHOUT_VALID_CONSENT_IS_EXCLUDED_ITEM = 649;

export const CUSTOMER_WITHOUT_VALID_CONSENT_IS_EXCLUDED_STATEMENT =
  "Customer without valid consent is excluded";

export const CUSTOMER_WITHOUT_VALID_CONSENT_IS_EXCLUDED_RULES = ["BR-004"] as const;

export const CUSTOMER_WITHOUT_VALID_CONSENT_IS_EXCLUDED_FR = ["FR-034", "FR-055"] as const;

export const INVALID_CONSENT_EXCLUSION_CODE = "INVALID_CONSENT" as const;

export const INVALID_CONSENT_EXCLUSION_EXPLANATION =
  "Customer does not have valid required consent";

export const RELATED_MARKETING_OPT_OUT_CODE = "MARKETING_OPT_OUT" as const;

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.campaign.CustomerWithoutValidConsentIsExcludedTests";

export const ELIGIBILITY_RULES_DOC_PATH = "docs/architecture/eligibility-rules.md";

export function isInvalidConsentExclusion(code: string | null | undefined): boolean {
  return code === INVALID_CONSENT_EXCLUSION_CODE;
}

/** True for either missing/invalid consent or withdrawn marketing consent. */
export function isConsentRelatedExclusion(code: string | null | undefined): boolean {
  return (
    code === INVALID_CONSENT_EXCLUSION_CODE || code === RELATED_MARKETING_OPT_OUT_CODE
  );
}

export function invalidConsentExclusionPresentation(count = 1): {
  code: string;
  title: string;
  severity: "critical" | "warning" | "info";
  ruleHint: string;
  count: number;
} {
  return {
    code: INVALID_CONSENT_EXCLUSION_CODE,
    title: formatExclusionReasonTitle(INVALID_CONSENT_EXCLUSION_CODE),
    severity: exclusionReasonSeverity(INVALID_CONSENT_EXCLUSION_CODE),
    ruleHint: exclusionReasonRuleHint(INVALID_CONSENT_EXCLUSION_CODE),
    count,
  };
}

/**
 * Audience models must treat missing/invalid required consent as non-contactable.
 */
export function customerIsExcludedForMissingOrInvalidConsent(options: {
  hasValidRequiredConsent: boolean;
  exclusionReasonCode?: string | null;
}): boolean {
  if (!options.hasValidRequiredConsent) {
    return true;
  }
  return isInvalidConsentExclusion(options.exclusionReasonCode);
}

export function invalidConsentIsKnownExclusionCode(): boolean {
  return isKnownExclusionReasonCode(INVALID_CONSENT_EXCLUSION_CODE);
}
