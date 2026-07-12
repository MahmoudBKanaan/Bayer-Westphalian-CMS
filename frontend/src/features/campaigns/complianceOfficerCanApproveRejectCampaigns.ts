/**
 * Sprint 16 critical test item **655**: Compliance Officer can approve/reject campaigns.
 *
 * KB: FR-059, TC-011, BR-005 / COMP-006. Review roles are ADMIN and COMPLIANCE_OFFICER only.
 */

import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import { CAMPAIGN_MANAGE_ROLES, CAMPAIGN_REVIEW_ROLES } from "@/features/auth/permissions";
import {
  COMPLIANCE_APPROVE_UI_ROLES,
  canApproveCampaignsThroughUi,
} from "@/features/campaigns/complianceApprovalFlow";

export const COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_ITEM = 655;

export const COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_STATEMENT =
  "Compliance Officer can approve/reject campaigns";

export const COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_TEST_CASES = ["TC-011"] as const;

export const COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_FR = ["FR-059"] as const;

export const COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_RULES = ["BR-005"] as const;

export const PRIMARY_REVIEW_ROLE: SystemRoleName = "COMPLIANCE_OFFICER";

export const ALLOWED_REVIEW_ROLES: SystemRoleName[] = ["ADMIN", "COMPLIANCE_OFFICER"];

export const APPROVE_API_PATH = "POST /api/campaigns/{id}/approve";

export const REJECT_API_PATH = "POST /api/campaigns/{id}/reject";

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.campaign.ComplianceOfficerCanApproveRejectCampaignsTests";

export const COMPLIANCE_REVIEW_DOC_PATH = "docs/modules/compliance-review.md";

export const COMPLIANCE_OFFICER_GUIDE_PATH = "docs/user-guides/compliance-officer-guide.md";

export function complianceOfficerCanApproveThroughUi(): boolean {
  return canApproveCampaignsThroughUi(["COMPLIANCE_OFFICER"]);
}

export function campaignManagerCannotApproveThroughUi(): boolean {
  return !canApproveCampaignsThroughUi(["CAMPAIGN_MANAGER"]);
}

export function reviewRolesMatchKbEditors(): boolean {
  const allowed = new Set(ALLOWED_REVIEW_ROLES);
  return (
    CAMPAIGN_REVIEW_ROLES.length === allowed.size &&
    CAMPAIGN_REVIEW_ROLES.every((r) => allowed.has(r)) &&
    COMPLIANCE_APPROVE_UI_ROLES.every((r) => allowed.has(r))
  );
}

/** Compliance can review; campaign managers manage drafts but do not approve. */
export function complianceReviewsWhileManagersWrite(): boolean {
  return (
    CAMPAIGN_REVIEW_ROLES.includes("COMPLIANCE_OFFICER") &&
    !CAMPAIGN_MANAGE_ROLES.includes("COMPLIANCE_OFFICER") &&
    CAMPAIGN_MANAGE_ROLES.includes("CAMPAIGN_MANAGER") &&
    !CAMPAIGN_REVIEW_ROLES.includes("CAMPAIGN_MANAGER")
  );
}
