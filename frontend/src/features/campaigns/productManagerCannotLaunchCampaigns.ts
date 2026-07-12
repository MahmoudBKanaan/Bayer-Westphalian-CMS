/**
 * Sprint 16 critical test item **653**: Product Manager cannot launch campaigns.
 *
 * KB: TC-013, FR-060. Launch is limited to ADMIN / CAMPAIGN_MANAGER via canManageCampaigns
 * (backend) and CAMPAIGN_LAUNCH_UI_ROLES (frontend). Product Manager may manage products and
 * read campaigns, but must not launch.
 */

import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import { CAMPAIGN_MANAGE_ROLES, PRODUCT_MANAGE_ROLES } from "@/features/auth/permissions";
import {
  CAMPAIGN_LAUNCH_UI_ROLES,
  canEnableLaunchButton,
  canLaunchCampaignsThroughUi,
  evaluateLaunchReadiness,
} from "@/features/campaigns/campaignLaunchFlow";

export const PRODUCT_MANAGER_CANNOT_LAUNCH_CAMPAIGNS_ITEM = 653;

export const PRODUCT_MANAGER_CANNOT_LAUNCH_CAMPAIGNS_STATEMENT =
  "Product Manager cannot launch campaigns";

export const PRODUCT_MANAGER_CANNOT_LAUNCH_CAMPAIGNS_TEST_CASES = ["TC-013"] as const;

export const PRODUCT_MANAGER_CANNOT_LAUNCH_CAMPAIGNS_FR = ["FR-060"] as const;

export const BLOCKED_LAUNCH_ROLE: SystemRoleName = "PRODUCT_MANAGER";

export const ALLOWED_LAUNCH_ROLES: SystemRoleName[] = ["ADMIN", "CAMPAIGN_MANAGER"];

export const LAUNCH_API_PATH = "POST /api/campaigns/{id}/launch";

export const BACKEND_CRITICAL_TEST_CLASS =
  "com.bayerwestphalian.campaign.campaign.ProductManagerCannotLaunchCampaignsTests";

export const CAMPAIGN_LAUNCH_DOC_PATH = "docs/modules/campaign-launch.md";

export const PRODUCT_MANAGER_GUIDE_PATH = "docs/user-guides/product-manager-guide.md";

/** Product Manager may manage products but is not a campaign launch role. */
export function productManagerCanManageProductsButNotLaunch(): boolean {
  return (
    PRODUCT_MANAGE_ROLES.includes("PRODUCT_MANAGER") &&
    !CAMPAIGN_MANAGE_ROLES.includes("PRODUCT_MANAGER") &&
    !CAMPAIGN_LAUNCH_UI_ROLES.includes("PRODUCT_MANAGER")
  );
}

export function productManagerCannotLaunchThroughUi(): boolean {
  return !canLaunchCampaignsThroughUi(["PRODUCT_MANAGER"]);
}

export function productManagerLaunchButtonDisabledEvenWhenApproved(): boolean {
  return (
    !canEnableLaunchButton({
      canManageCampaigns: false,
      campaignStatus: "APPROVED",
    }) &&
    evaluateLaunchReadiness({
      canManageCampaigns: false,
      campaignStatus: "APPROVED",
      eligibleCount: 10,
    }).state === "blocked-role"
  );
}

export function onlyAdminAndCampaignManagerInLaunchRoles(): boolean {
  const allowed = new Set(ALLOWED_LAUNCH_ROLES);
  return (
    CAMPAIGN_LAUNCH_UI_ROLES.every((r) => allowed.has(r)) &&
    allowed.size === CAMPAIGN_LAUNCH_UI_ROLES.length &&
    !CAMPAIGN_LAUNCH_UI_ROLES.includes(BLOCKED_LAUNCH_ROLE)
  );
}
