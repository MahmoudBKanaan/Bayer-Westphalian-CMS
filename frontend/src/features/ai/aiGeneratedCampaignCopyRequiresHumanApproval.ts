/**
 * Sprint 16 critical test item **662**: AI-generated campaign copy requires human approval.
 *
 * KB: AI-005 / COMP-005 — campaign copy suggestions always require human review before use;
 * approve endpoint records a human principal and does not auto-apply copy or approve campaigns.
 */

export const AI_GENERATED_CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL_ITEM = 662;

export const AI_GENERATED_CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL_STATEMENT =
  "AI-generated campaign copy requires human approval";

export const AI_GENERATED_CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL_AI = ["AI-005"] as const;

export const AI_GENERATED_CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL_COMP = ["COMP-005"] as const;

export const CAMPAIGN_COPY_RECOMMENDATION_TYPE = "COPY" as const;

/** Forced on every CampaignCopySuggestionView (DTO compact constructor). */
export const CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL = true as const;

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.ai.AiGeneratedCampaignCopyRequiresHumanApprovalTests";

export const COMPANION_CAMPAIGN_COPY_SERVICE_TEST_CLASS =
  "com.bayerwestphalian.campaign.ai.CampaignCopyServiceTests";

export const AI_LIMITATIONS_DOC_PATH = "docs/modules/ai-limitations-and-human-approval.md";

export const AI_FEATURES_DOC_PATH = "docs/modules/ai-features.md";

export type CampaignCopySuggestionLike = {
  requiresHumanApproval?: boolean | null;
  humanApproved?: boolean | null;
  approvedByUserId?: string | null;
  recommendationType?: string | null;
  storedRecommendationId?: string | null;
};

/**
 * True when a suggestion is still pending human review (must not be treated as live copy).
 */
export function isCampaignCopyPendingHumanApproval(
  suggestion: CampaignCopySuggestionLike | null | undefined,
): boolean {
  if (suggestion == null) {
    return true;
  }
  if (suggestion.requiresHumanApproval !== true && suggestion.requiresHumanApproval !== false) {
    // Missing flag: treat as requiring approval (safe default).
    return suggestion.humanApproved !== true;
  }
  if (suggestion.requiresHumanApproval !== true) {
    // Policy: even if a client sends false, operational use still requires approval.
    return suggestion.humanApproved !== true;
  }
  return suggestion.humanApproved !== true;
}

/**
 * True when human approval has been recorded for a stored copy recommendation.
 */
export function isCampaignCopyHumanApproved(
  suggestion: CampaignCopySuggestionLike | null | undefined,
): boolean {
  if (suggestion == null) {
    return false;
  }
  if (suggestion.humanApproved === true && suggestion.approvedByUserId) {
    return true;
  }
  return false;
}

/**
 * Human approval of copy never equals campaign compliance approval / launch rights.
 */
export function humanCopyApprovalGrantsCampaignCompliance(): false {
  return false;
}

/**
 * Safe UI notice for operators reviewing AI campaign copy.
 */
export const CAMPAIGN_COPY_HUMAN_APPROVAL_UI_NOTICE =
  "AI-generated campaign copy requires human approval before use. Approving copy does not approve or launch the campaign." as const;
