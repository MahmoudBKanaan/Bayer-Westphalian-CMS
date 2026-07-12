import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  ALLOWED_LAUNCH_STATUS,
  BACKEND_CRITICAL_TEST_CLASS,
  BLOCKED_LAUNCH_STATUSES,
  CAMPAIGN_CANNOT_LAUNCH_WITHOUT_APPROVAL_FR,
  CAMPAIGN_CANNOT_LAUNCH_WITHOUT_APPROVAL_ITEM,
  CAMPAIGN_CANNOT_LAUNCH_WITHOUT_APPROVAL_RULES,
  CAMPAIGN_CANNOT_LAUNCH_WITHOUT_APPROVAL_STATEMENT,
  CAMPAIGN_LAUNCH_DOC_PATH,
  approvedStatusEnablesLaunchForManager,
  everyBlockedStatusDisablesLaunchButton,
  isLaunchBlockedPendingApproval,
  uiAllowsLaunchWithoutBypassingApproval,
} from "@/features/campaigns/campaignCannotLaunchWithoutApproval";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("campaignCannotLaunchWithoutApproval (item 647)", () => {
  it("locks the critical KB rule identity", () => {
    expect(CAMPAIGN_CANNOT_LAUNCH_WITHOUT_APPROVAL_ITEM).toBe(647);
    expect(CAMPAIGN_CANNOT_LAUNCH_WITHOUT_APPROVAL_STATEMENT).toBe(
      "Campaign cannot launch without approval",
    );
    expect(CAMPAIGN_CANNOT_LAUNCH_WITHOUT_APPROVAL_RULES).toEqual(["BR-005", "BR-032"]);
    expect(CAMPAIGN_CANNOT_LAUNCH_WITHOUT_APPROVAL_FR).toEqual(["FR-060"]);
    expect(ALLOWED_LAUNCH_STATUS).toBe("APPROVED");
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain("CampaignCannotLaunchWithoutApprovalTests");
  });

  it("disables launch for every non-APPROVED status including SUBMITTED (BR-032)", () => {
    expect(everyBlockedStatusDisablesLaunchButton(true)).toBe(true);
    expect(
      uiAllowsLaunchWithoutBypassingApproval({
        canManageCampaigns: true,
        campaignStatus: "SUBMITTED",
      }),
    ).toBe(false);
    expect(
      uiAllowsLaunchWithoutBypassingApproval({
        canManageCampaigns: true,
        campaignStatus: "DRAFT",
      }),
    ).toBe(false);
    expect(isLaunchBlockedPendingApproval("SUBMITTED")).toBe(true);
    expect(isLaunchBlockedPendingApproval("DRAFT")).toBe(true);
    expect(isLaunchBlockedPendingApproval("REJECTED")).toBe(true);
    expect(isLaunchBlockedPendingApproval("APPROVED")).toBe(false);
    expect(BLOCKED_LAUNCH_STATUSES).toContain("SUBMITTED");
  });

  it("enables launch only after approval for campaign managers", () => {
    expect(approvedStatusEnablesLaunchForManager()).toBe(true);
    expect(
      uiAllowsLaunchWithoutBypassingApproval({
        canManageCampaigns: false,
        campaignStatus: "APPROVED",
      }),
    ).toBe(false);
  });

  it("documents the launch gate in campaign-launch module docs", () => {
    const docPath = path.join(repoRoot, CAMPAIGN_LAUNCH_DOC_PATH);
    expect(existsSync(docPath)).toBe(true);
    const documentation = readRepoFile(CAMPAIGN_LAUNCH_DOC_PATH);
    expect(documentation).toContain("BR-005");
    expect(documentation).toContain("APPROVED");
    expect(documentation).toContain("647");
    expect(documentation).toContain("CampaignCannotLaunchWithoutApprovalTests");
    expect(documentation).toContain("cannot launch");
  });
});
