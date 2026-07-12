import { describe, expect, it } from "vitest";
import {
  CAMPAIGN_LAUNCH_FIXTURES,
  CAMPAIGN_LAUNCH_UI_ROLES,
  CAMPAIGN_LAUNCHED_NOTICE,
  campaignLaunchConfirmDescription,
  campaignLaunchStepIdsInOrder,
  canEnableLaunchButton,
  canLaunchCampaignsThroughUi,
  evaluateLaunchReadiness,
  formatCampaignLaunchJourney,
  isLaunchReady,
  isValidCampaignLaunchOrder,
  recipientPreviewPath,
} from "@/features/campaigns/campaignLaunchFlow";

describe("campaignLaunchFlow (item 605)", () => {
  it("documents the UI campaign launch journey", () => {
    expect(campaignLaunchStepIdsInOrder()).toEqual([
      "open-recipient-preview",
      "confirm-launch-readiness",
      "confirm-launch-dialog",
      "see-launch-result",
    ]);
    expect(formatCampaignLaunchJourney()).toBe(
      "Open recipient preview → Confirm launch readiness → Confirm launch dialog → See launch result",
    );
    expect(isValidCampaignLaunchOrder(campaignLaunchStepIdsInOrder())).toBe(true);
    expect(isValidCampaignLaunchOrder(["confirm-launch-dialog"] as never)).toBe(false);
  });

  it("allows only admin and campaign manager to launch through the UI", () => {
    expect(CAMPAIGN_LAUNCH_UI_ROLES).toEqual(["ADMIN", "CAMPAIGN_MANAGER"]);
    expect(canLaunchCampaignsThroughUi(["ADMIN"])).toBe(true);
    expect(canLaunchCampaignsThroughUi(["CAMPAIGN_MANAGER"])).toBe(true);
    expect(canLaunchCampaignsThroughUi(["COMPLIANCE_OFFICER"])).toBe(false);
    expect(canLaunchCampaignsThroughUi(["BI_ANALYST"])).toBe(false);
  });

  it("enables launch only for APPROVED campaigns managed by authorized roles", () => {
    expect(
      canEnableLaunchButton({ canManageCampaigns: true, campaignStatus: "APPROVED" }),
    ).toBe(true);
    expect(
      canEnableLaunchButton({ canManageCampaigns: true, campaignStatus: "SUBMITTED" }),
    ).toBe(false);
    expect(
      canEnableLaunchButton({ canManageCampaigns: false, campaignStatus: "APPROVED" }),
    ).toBe(false);
  });

  it("reports launch readiness for approved campaigns", () => {
    const ready = evaluateLaunchReadiness({
      canManageCampaigns: true,
      campaignStatus: "APPROVED",
      eligibleCount: CAMPAIGN_LAUNCH_FIXTURES.eligible,
    });
    expect(isLaunchReady(ready)).toBe(true);
    expect(ready.message).toMatch(/APPROVED and ready to launch/i);

    const blocked = evaluateLaunchReadiness({
      canManageCampaigns: true,
      campaignStatus: "DRAFT",
      eligibleCount: 5,
    });
    expect(isLaunchReady(blocked)).toBe(false);
  });

  it("describes the confirm-launch dialog impact", () => {
    expect(
      campaignLaunchConfirmDescription({
        campaignName: CAMPAIGN_LAUNCH_FIXTURES.campaignName,
        eligibleCount: 7,
        excludedCount: 3,
      }),
    ).toContain("ACTIVE");
  });

  it("builds recipient preview paths and pins launch notice", () => {
    expect(recipientPreviewPath(CAMPAIGN_LAUNCH_FIXTURES.campaignId)).toBe(
      `/campaigns/${CAMPAIGN_LAUNCH_FIXTURES.campaignId}/recipients/preview`,
    );
    expect(CAMPAIGN_LAUNCHED_NOTICE).toBe("Campaign launched.");
  });
});
