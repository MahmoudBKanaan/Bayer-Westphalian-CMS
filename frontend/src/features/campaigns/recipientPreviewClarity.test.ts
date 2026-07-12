import { describe, expect, it } from "vitest";
import {
  RECIPIENT_PREVIEW_GATE_NOTE,
  RECIPIENT_PREVIEW_GUIDE,
  RECIPIENT_PREVIEW_PAGE_LEAD,
  evaluateLaunchReadiness,
  formatEligibilityRateLabel,
  formatRecipientTabLabel,
  presentRecipientExclusionReason,
} from "@/features/campaigns/recipientPreviewClarity";

describe("recipientPreviewClarity (item 594)", () => {
  it("explains preview purpose and guide tabs", () => {
    expect(RECIPIENT_PREVIEW_PAGE_LEAD.toLowerCase()).toContain("eligible");
    expect(RECIPIENT_PREVIEW_PAGE_LEAD.toLowerCase()).toContain("excluded");
    expect(RECIPIENT_PREVIEW_GATE_NOTE).toContain("APPROVED");
    expect(RECIPIENT_PREVIEW_GUIDE.map((item) => item.id)).toEqual([
      "audience-preview",
      "eligible",
      "excluded",
    ]);
  });

  it("evaluates launch readiness by role and campaign status", () => {
    expect(
      evaluateLaunchReadiness({
        canManageCampaigns: false,
        campaignStatus: "APPROVED",
        eligibleCount: 5,
      }).state,
    ).toBe("blocked-role");

    expect(
      evaluateLaunchReadiness({
        canManageCampaigns: true,
        campaignStatus: "SUBMITTED",
        eligibleCount: 5,
      }).message,
    ).toContain("SUBMITTED");

    expect(
      evaluateLaunchReadiness({
        canManageCampaigns: true,
        campaignStatus: "APPROVED",
        eligibleCount: 5,
      }),
    ).toMatchObject({
      state: "ready",
      message: expect.stringContaining("5 eligible"),
    });

    expect(
      evaluateLaunchReadiness({
        canManageCampaigns: true,
        campaignStatus: "APPROVED",
        eligibleCount: 0,
      }).message.toLowerCase(),
    ).toContain("no eligible");
  });

  it("formats tab counts, eligibility rates, and exclusion reason titles", () => {
    expect(formatRecipientTabLabel("Eligible recipients", 2, false)).toBe(
      "Eligible recipients (2)",
    );
    expect(formatRecipientTabLabel("Eligible recipients", 2, true)).toBe("Eligible recipients");
    expect(formatEligibilityRateLabel(2, 4)).toBe(
      "50.0% of the matched audience is eligible for contact",
    );
    expect(formatEligibilityRateLabel(0, 0)).toBe("No audience matched yet");

    const reason = presentRecipientExclusionReason("INVALID_CONSENT");
    expect(reason.code).toBe("INVALID_CONSENT");
    expect(reason.title).toBe("Invalid or missing consent");
    expect(presentRecipientExclusionReason(null).code).toBe("UNKNOWN");
  });
});
