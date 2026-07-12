/**
 * Sprint 16 critical test item **651**: Same customer cannot be duplicated in same campaign.
 *
 * KB: BR-010, FR-056. Eligibility returns DUPLICATE_CAMPAIGN_RECIPIENT; schema enforces unique
 * (campaign_id, customer_id) on campaign_recipients.
 */

import {
  exclusionReasonRuleHint,
  exclusionReasonSeverity,
  formatExclusionReasonTitle,
  isKnownExclusionReasonCode,
} from "@/features/segments/exclusionReasons";

export const SAME_CUSTOMER_CANNOT_BE_DUPLICATED_IN_SAME_CAMPAIGN_ITEM = 651;

export const SAME_CUSTOMER_CANNOT_BE_DUPLICATED_IN_SAME_CAMPAIGN_STATEMENT =
  "Same customer cannot be duplicated in same campaign";

export const SAME_CUSTOMER_CANNOT_BE_DUPLICATED_IN_SAME_CAMPAIGN_RULES = ["BR-010"] as const;

export const SAME_CUSTOMER_CANNOT_BE_DUPLICATED_IN_SAME_CAMPAIGN_FR = ["FR-056"] as const;

export const DUPLICATE_CAMPAIGN_RECIPIENT_EXCLUSION_CODE =
  "DUPLICATE_CAMPAIGN_RECIPIENT" as const;

export const DUPLICATE_CAMPAIGN_RECIPIENT_EXPLANATION =
  "Customer is already assigned to this campaign";

export const CAMPAIGN_RECIPIENT_UNIQUE_CONSTRAINT =
  "campaign_recipients_campaign_customer_unique" as const;

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.campaign.SameCustomerCannotBeDuplicatedInSameCampaignTests";

export const ELIGIBILITY_RULES_DOC_PATH = "docs/architecture/eligibility-rules.md";

export const RECIPIENT_PREVIEW_DOC_PATH = "docs/modules/recipient-preview.md";

export function isDuplicateCampaignRecipientExclusion(
  code: string | null | undefined,
): boolean {
  return code === DUPLICATE_CAMPAIGN_RECIPIENT_EXCLUSION_CODE;
}

/**
 * True when the customer is already on the campaign audience (by flag or exclusion code).
 */
export function customerIsDuplicateOnCampaign(options: {
  alreadyAssignedToCampaign: boolean;
  exclusionReasonCode?: string | null;
}): boolean {
  if (options.alreadyAssignedToCampaign) {
    return true;
  }
  return isDuplicateCampaignRecipientExclusion(options.exclusionReasonCode);
}

export function duplicateCampaignRecipientPresentation(count = 1): {
  code: string;
  title: string;
  severity: "critical" | "warning" | "info";
  ruleHint: string;
  count: number;
} {
  return {
    code: DUPLICATE_CAMPAIGN_RECIPIENT_EXCLUSION_CODE,
    title: formatExclusionReasonTitle(DUPLICATE_CAMPAIGN_RECIPIENT_EXCLUSION_CODE),
    severity: exclusionReasonSeverity(DUPLICATE_CAMPAIGN_RECIPIENT_EXCLUSION_CODE),
    ruleHint: exclusionReasonRuleHint(DUPLICATE_CAMPAIGN_RECIPIENT_EXCLUSION_CODE),
    count,
  };
}

export function duplicateIsKnownExclusionCode(): boolean {
  return isKnownExclusionReasonCode(DUPLICATE_CAMPAIGN_RECIPIENT_EXCLUSION_CODE);
}
