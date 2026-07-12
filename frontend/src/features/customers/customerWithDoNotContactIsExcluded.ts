/**
 * Sprint 16 critical test item **648**: Customer with do_not_contact is excluded.
 *
 * KB: BR-001, FR-097, FR-055. UI surfaces the exclusion code; backend EligibilityService is
 * authoritative.
 */

import {
  formatExclusionReasonTitle,
  exclusionReasonRuleHint,
  exclusionReasonSeverity,
  isKnownExclusionReasonCode,
} from "@/features/segments/exclusionReasons";

export const CUSTOMER_WITH_DO_NOT_CONTACT_IS_EXCLUDED_ITEM = 648;

export const CUSTOMER_WITH_DO_NOT_CONTACT_IS_EXCLUDED_STATEMENT =
  "Customer with do_not_contact is excluded";

export const CUSTOMER_WITH_DO_NOT_CONTACT_IS_EXCLUDED_RULES = ["BR-001"] as const;

export const CUSTOMER_WITH_DO_NOT_CONTACT_IS_EXCLUDED_FR = ["FR-097", "FR-055"] as const;

export const DO_NOT_CONTACT_EXCLUSION_CODE = "DO_NOT_CONTACT" as const;

export const DO_NOT_CONTACT_EXCLUSION_EXPLANATION = "Customer has do-not-contact enabled";

/** First rule in KB eligibility evaluation order. */
export const DO_NOT_CONTACT_EVALUATION_ORDER_POSITION = 1;

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.campaign.CustomerWithDoNotContactIsExcludedTests";

export const ELIGIBILITY_RULES_DOC_PATH = "docs/architecture/eligibility-rules.md";

/**
 * True when a preview/exclusion payload represents a do-not-contact block.
 */
export function isDoNotContactExclusion(code: string | null | undefined): boolean {
  return code === DO_NOT_CONTACT_EXCLUSION_CODE;
}

/**
 * UI presentation contract for DO_NOT_CONTACT (recipient preview / segment preview).
 */
export function doNotContactExclusionPresentation(count = 1): {
  code: string;
  title: string;
  severity: "critical" | "warning" | "info";
  ruleHint: string;
  count: number;
} {
  return {
    code: DO_NOT_CONTACT_EXCLUSION_CODE,
    title: formatExclusionReasonTitle(DO_NOT_CONTACT_EXCLUSION_CODE),
    severity: exclusionReasonSeverity(DO_NOT_CONTACT_EXCLUSION_CODE),
    ruleHint: exclusionReasonRuleHint(DO_NOT_CONTACT_EXCLUSION_CODE),
    count,
  };
}

/**
 * A customer flagged do-not-contact must never be treated as campaign-eligible in UI models.
 */
export function customerIsExcludedFromCampaignAudience(options: {
  doNotContact: boolean;
  exclusionReasonCode?: string | null;
}): boolean {
  if (options.doNotContact) {
    return true;
  }
  return isDoNotContactExclusion(options.exclusionReasonCode);
}

export function dncIsKnownCriticalExclusionCode(): boolean {
  return (
    isKnownExclusionReasonCode(DO_NOT_CONTACT_EXCLUSION_CODE) &&
    exclusionReasonSeverity(DO_NOT_CONTACT_EXCLUSION_CODE) === "critical"
  );
}
