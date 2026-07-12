/**
 * Sprint 16 critical test item **647**: Campaign cannot launch without approval.
 *
 * KB: BR-005, BR-032, FR-060 / TC-001. UI gate mirrors domain rule; backend is authoritative.
 */

import type { CampaignStatus } from "@/api/campaigns";
import {
  canEnableLaunchButton,
  evaluateLaunchReadiness,
  isLaunchReady,
} from "@/features/campaigns/campaignLaunchFlow";

export const CAMPAIGN_CANNOT_LAUNCH_WITHOUT_APPROVAL_ITEM = 647;

export const CAMPAIGN_CANNOT_LAUNCH_WITHOUT_APPROVAL_STATEMENT =
  "Campaign cannot launch without approval";

export const CAMPAIGN_CANNOT_LAUNCH_WITHOUT_APPROVAL_RULES = ["BR-005", "BR-032"] as const;

export const CAMPAIGN_CANNOT_LAUNCH_WITHOUT_APPROVAL_FR = ["FR-060"] as const;

/** Only this lifecycle status may launch (domain canLaunch / service launchCampaign). */
export const ALLOWED_LAUNCH_STATUS: CampaignStatus = "APPROVED";

/** Pre-approval and other statuses that must keep launch disabled. */
export const BLOCKED_LAUNCH_STATUSES: CampaignStatus[] = [
  "DRAFT",
  "SUBMITTED",
  "REJECTED",
  "ACTIVE",
  "PAUSED",
  "COMPLETED",
  "ARCHIVED",
];

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.campaign.CampaignCannotLaunchWithoutApprovalTests";

export const CAMPAIGN_LAUNCH_DOC_PATH = "docs/modules/campaign-launch.md";

/**
 * True when UI would allow the launch control (manager + APPROVED only).
 * Does not replace backend BR-005 enforcement.
 */
export function uiAllowsLaunchWithoutBypassingApproval(options: {
  canManageCampaigns: boolean;
  campaignStatus: CampaignStatus | null | undefined;
}): boolean {
  return canEnableLaunchButton(options);
}

/**
 * True when readiness is blocked solely because status is not APPROVED.
 */
export function isLaunchBlockedPendingApproval(
  campaignStatus: CampaignStatus | null | undefined,
  canManageCampaigns = true,
): boolean {
  if (!canManageCampaigns || campaignStatus == null) {
    return false;
  }
  if (campaignStatus === ALLOWED_LAUNCH_STATUS) {
    return false;
  }
  const readiness = evaluateLaunchReadiness({
    canManageCampaigns,
    campaignStatus,
    eligibleCount: 10,
  });
  return readiness.state === "blocked-status" && !isLaunchReady(readiness);
}

export function everyBlockedStatusDisablesLaunchButton(
  canManageCampaigns = true,
): boolean {
  return BLOCKED_LAUNCH_STATUSES.every(
    (status) =>
      !uiAllowsLaunchWithoutBypassingApproval({
        canManageCampaigns,
        campaignStatus: status,
      }),
  );
}

export function approvedStatusEnablesLaunchForManager(): boolean {
  return uiAllowsLaunchWithoutBypassingApproval({
    canManageCampaigns: true,
    campaignStatus: ALLOWED_LAUNCH_STATUS,
  });
}
