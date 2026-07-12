import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  ALLOWED_LAUNCH_ROLES,
  BACKEND_CRITICAL_TEST_CLASS,
  BLOCKED_LAUNCH_ROLE,
  CAMPAIGN_LAUNCH_DOC_PATH,
  LAUNCH_API_PATH,
  PRODUCT_MANAGER_CANNOT_LAUNCH_CAMPAIGNS_FR,
  PRODUCT_MANAGER_CANNOT_LAUNCH_CAMPAIGNS_ITEM,
  PRODUCT_MANAGER_CANNOT_LAUNCH_CAMPAIGNS_STATEMENT,
  PRODUCT_MANAGER_CANNOT_LAUNCH_CAMPAIGNS_TEST_CASES,
  PRODUCT_MANAGER_GUIDE_PATH,
  onlyAdminAndCampaignManagerInLaunchRoles,
  productManagerCanManageProductsButNotLaunch,
  productManagerCannotLaunchThroughUi,
  productManagerLaunchButtonDisabledEvenWhenApproved,
} from "@/features/campaigns/productManagerCannotLaunchCampaigns";
import { CAMPAIGN_LAUNCH_UI_ROLES } from "@/features/campaigns/campaignLaunchFlow";
import { CAMPAIGN_MANAGE_ROLES, PRODUCT_MANAGE_ROLES } from "@/features/auth/permissions";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("productManagerCannotLaunchCampaigns (item 653)", () => {
  it("locks the critical KB rule identity", () => {
    expect(PRODUCT_MANAGER_CANNOT_LAUNCH_CAMPAIGNS_ITEM).toBe(653);
    expect(PRODUCT_MANAGER_CANNOT_LAUNCH_CAMPAIGNS_STATEMENT).toBe(
      "Product Manager cannot launch campaigns",
    );
    expect(PRODUCT_MANAGER_CANNOT_LAUNCH_CAMPAIGNS_TEST_CASES).toEqual(["TC-013"]);
    expect(PRODUCT_MANAGER_CANNOT_LAUNCH_CAMPAIGNS_FR).toEqual(["FR-060"]);
    expect(BLOCKED_LAUNCH_ROLE).toBe("PRODUCT_MANAGER");
    expect(ALLOWED_LAUNCH_ROLES).toEqual(["ADMIN", "CAMPAIGN_MANAGER"]);
    expect(LAUNCH_API_PATH).toBe("POST /api/campaigns/{id}/launch");
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain("ProductManagerCannotLaunchCampaignsTests");
  });

  it("keeps Product Manager out of campaign manage and launch UI roles", () => {
    expect(productManagerCanManageProductsButNotLaunch()).toBe(true);
    expect(productManagerCannotLaunchThroughUi()).toBe(true);
    expect(productManagerLaunchButtonDisabledEvenWhenApproved()).toBe(true);
    expect(onlyAdminAndCampaignManagerInLaunchRoles()).toBe(true);
    expect(CAMPAIGN_LAUNCH_UI_ROLES).not.toContain("PRODUCT_MANAGER");
    expect(CAMPAIGN_MANAGE_ROLES).not.toContain("PRODUCT_MANAGER");
    expect(PRODUCT_MANAGE_ROLES).toContain("PRODUCT_MANAGER");
  });

  it("documents TC-013 / Product Manager cannot launch in launch and PM guides", () => {
    const launchDoc = readRepoFile(CAMPAIGN_LAUNCH_DOC_PATH);
    expect(existsSync(path.join(repoRoot, CAMPAIGN_LAUNCH_DOC_PATH))).toBe(true);
    expect(launchDoc).toContain("653");
    expect(launchDoc).toContain("ProductManagerCannotLaunchCampaignsTests");
    expect(launchDoc).toContain("TC-013");
    expect(launchDoc).toMatch(/PRODUCT_MANAGER.*cannot|cannot launch campaigns/i);

    const pmGuide = readRepoFile(PRODUCT_MANAGER_GUIDE_PATH);
    expect(existsSync(path.join(repoRoot, PRODUCT_MANAGER_GUIDE_PATH))).toBe(true);
    expect(pmGuide).toMatch(/cannot launch campaigns/i);
  });
});
