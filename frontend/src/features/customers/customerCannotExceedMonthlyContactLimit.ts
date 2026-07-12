/**
 * Sprint 16 critical test item **652**: Customer cannot exceed monthly contact limit.
 *
 * KB: BR-011, FR-092, FR-056. Eligibility excludes with MONTHLY_CONTACT_LIMIT when SENT/CALLED
 * events in the last 30 days reach SystemSettings.monthlyContactLimit (item 535).
 */

import {
  exclusionReasonRuleHint,
  exclusionReasonSeverity,
  formatExclusionReasonTitle,
  isKnownExclusionReasonCode,
} from "@/features/segments/exclusionReasons";

export const CUSTOMER_CANNOT_EXCEED_MONTHLY_CONTACT_LIMIT_ITEM = 652;

export const CUSTOMER_CANNOT_EXCEED_MONTHLY_CONTACT_LIMIT_STATEMENT =
  "Customer cannot exceed monthly contact limit";

export const CUSTOMER_CANNOT_EXCEED_MONTHLY_CONTACT_LIMIT_RULES = ["BR-011"] as const;

export const CUSTOMER_CANNOT_EXCEED_MONTHLY_CONTACT_LIMIT_FR = ["FR-092", "FR-056"] as const;

export const MONTHLY_CONTACT_LIMIT_EXCLUSION_CODE = "MONTHLY_CONTACT_LIMIT" as const;

export const MONTHLY_CONTACT_LIMIT_EXPLANATION =
  "Customer has reached the monthly marketing contact limit";

/** Rolling window used by EligibilityService contact-event count (days). */
export const MONTHLY_CONTACT_ROLLING_WINDOW_DAYS = 30;

/** Contact event types counted toward the monthly marketing limit. */
export const MONTHLY_CONTACT_COUNTED_EVENT_TYPES = ["SENT", "CALLED"] as const;

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.campaign.CustomerCannotExceedMonthlyContactLimitTests";

export const ELIGIBILITY_RULES_DOC_PATH = "docs/architecture/eligibility-rules.md";

export const SYSTEM_SETTINGS_DOC_PATH = "docs/modules/system-settings.md";

export function isMonthlyContactLimitExclusion(code: string | null | undefined): boolean {
  return code === MONTHLY_CONTACT_LIMIT_EXCLUSION_CODE;
}

/**
 * True when recent marketing contacts have reached or exceeded the configured monthly limit.
 */
export function customerExceedsMonthlyContactLimit(options: {
  contactsInRollingWindow: number;
  monthlyContactLimit: number;
  exclusionReasonCode?: string | null;
}): boolean {
  if (options.contactsInRollingWindow >= options.monthlyContactLimit) {
    return true;
  }
  return isMonthlyContactLimitExclusion(options.exclusionReasonCode);
}

export function monthlyContactLimitPresentation(count = 1): {
  code: string;
  title: string;
  severity: "critical" | "warning" | "info";
  ruleHint: string;
  count: number;
} {
  return {
    code: MONTHLY_CONTACT_LIMIT_EXCLUSION_CODE,
    title: formatExclusionReasonTitle(MONTHLY_CONTACT_LIMIT_EXCLUSION_CODE),
    severity: exclusionReasonSeverity(MONTHLY_CONTACT_LIMIT_EXCLUSION_CODE),
    ruleHint: exclusionReasonRuleHint(MONTHLY_CONTACT_LIMIT_EXCLUSION_CODE),
    count,
  };
}

export function monthlyLimitIsKnownExclusionCode(): boolean {
  return isKnownExclusionReasonCode(MONTHLY_CONTACT_LIMIT_EXCLUSION_CODE);
}
