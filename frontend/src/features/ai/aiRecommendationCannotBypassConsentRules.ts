/**
 * Sprint 16 critical test item **661**: AI recommendation cannot bypass consent rules.
 *
 * KB: COMP-005 / AI-001–AI-006 preface / items 468, 502–504, 512 — AI is decision-support only;
 * consent, opt-out, do-not-contact, and EligibilityService remain authoritative for marketing.
 */

export const AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_ITEM = 661;

export const AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_STATEMENT =
  "AI recommendation cannot bypass consent rules";

export const AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_NFR = ["NFR-002"] as const;

export const AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_FR = ["FR-034"] as const;

export const AI_RECOMMENDATION_CANNOT_BYPASS_CONSENT_RULES_COMP = ["COMP-005"] as const;

/** AI feature IDs that remain advisory under consent rules. */
export const ADVISORY_AI_CAPABILITIES = [
  "AI-001",
  "AI-002",
  "AI-003",
  "AI-004",
  "AI-005",
  "AI-006",
] as const;

/** Operations AI must never expose (consent / eligibility ownership). */
export const FORBIDDEN_AI_CONSENT_OPERATIONS = [
  "recordConsent",
  "withdrawConsent",
  "markOptOut",
  "clearDoNotContact",
  "bypassEligibility",
  "overrideConsent",
  "forceEligible",
] as const;

export type ForbiddenAiConsentOperation = (typeof FORBIDDEN_AI_CONSENT_OPERATIONS)[number];

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.ai.AiRecommendationCannotBypassConsentRulesTests";

export const COMPANION_HUMAN_DECISION_TEST_CLASS =
  "com.bayerwestphalian.campaign.ai.AiSupportsHumanDecisionMakingOnlyTests";

export const AI_LIMITATIONS_DOC_PATH = "docs/modules/ai-limitations-and-human-approval.md";

export const AI_FEATURES_DOC_PATH = "docs/modules/ai-features.md";

/**
 * True when a method name would indicate AI is claiming consent/eligibility authority.
 */
export function isForbiddenAiConsentOperation(methodName: string | null | undefined): boolean {
  if (methodName == null || methodName === "") {
    return false;
  }
  return (FORBIDDEN_AI_CONSENT_OPERATIONS as readonly string[]).includes(methodName);
}

/**
 * Marketing inclusion decision authority: only eligibility/consent domain, never AI alone.
 */
export type MarketingInclusionAuthority =
  | "eligibility-service"
  | "consent-service"
  | "ai-recommendation";

/**
 * True when this authority may enforce marketing inclusion/exclusion under KB consent rules.
 * AI recommendations never may.
 */
export function isAuthoritativeForMarketingConsent(
  authority: MarketingInclusionAuthority,
): boolean {
  return authority === "eligibility-service" || authority === "consent-service";
}

/** KB item 661: AI recommendations cannot bypass consent rules. */
export function aiRecommendationCanBypassConsentRules(): false {
  return false;
}

/**
 * True when AI output is treated as advisory and still requires human + compliance path.
 * Always true for known AI capabilities: human approval never replaces eligibility/consent.
 */
export function aiOutputRequiresHumanAndConsentCompliance(options: {
  capability: string;
  humanApproved?: boolean;
  eligibilityEligible?: boolean;
}): boolean {
  const isKnownCapability = (ADVISORY_AI_CAPABILITIES as readonly string[]).includes(
    options.capability,
  );
  if (!isKnownCapability) {
    return true;
  }
  // Human approval of AI output still does not replace consent eligibility.
  void options.humanApproved;
  void options.eligibilityEligible;
  return true;
}

/**
 * Safe product copy for UI when explaining that AI cannot override consent.
 */
export const AI_CONSENT_NON_BYPASS_UI_NOTICE =
  "AI suggestions are decision support only. Consent, opt-out, and do-not-contact rules still apply." as const;
