import { describe, expect, it } from "vitest";
import {
  canApproveCampaignsThroughUi,
  canRequestApproval,
  canRequestRejection,
  COMPLIANCE_APPROVAL_FIXTURES,
  COMPLIANCE_APPROVE_UI_ROLES,
  COMPLIANCE_APPROVED_NOTICE,
  complianceApprovalConfirmCopy,
  complianceApprovalStepIdsInOrder,
  formatComplianceApprovalJourney,
  isValidComplianceApprovalOrder,
} from "@/features/campaigns/complianceApprovalFlow";

describe("complianceApprovalFlow (item 604)", () => {
  it("documents the UI compliance approval journey", () => {
    expect(complianceApprovalStepIdsInOrder()).toEqual([
      "open-compliance",
      "select-submitted-campaign",
      "confirm-approve",
      "see-approved",
    ]);
    expect(formatComplianceApprovalJourney()).toBe(
      "Open Compliance review → Select submitted campaign → Confirm approval → See approved result",
    );
    expect(isValidComplianceApprovalOrder(complianceApprovalStepIdsInOrder())).toBe(true);
    expect(isValidComplianceApprovalOrder(["confirm-approve"] as never)).toBe(false);
  });

  it("allows only admin and compliance officer to approve through the UI", () => {
    expect(COMPLIANCE_APPROVE_UI_ROLES).toEqual(["ADMIN", "COMPLIANCE_OFFICER"]);
    expect(canApproveCampaignsThroughUi(["ADMIN"])).toBe(true);
    expect(canApproveCampaignsThroughUi(["COMPLIANCE_OFFICER"])).toBe(true);
    expect(canApproveCampaignsThroughUi(["CAMPAIGN_MANAGER"])).toBe(false);
    expect(canApproveCampaignsThroughUi(["BI_ANALYST"])).toBe(false);
  });

  it("only allows approval for selected SUBMITTED campaigns", () => {
    expect(
      canRequestApproval({
        campaignId: COMPLIANCE_APPROVAL_FIXTURES.campaignId,
        campaignStatus: "SUBMITTED",
      }),
    ).toBe(true);
    expect(
      canRequestApproval({
        campaignId: COMPLIANCE_APPROVAL_FIXTURES.campaignId,
        campaignStatus: "DRAFT",
      }),
    ).toBe(false);
    expect(canRequestApproval({ campaignId: "", campaignStatus: "SUBMITTED" })).toBe(false);
  });

  it("requires a rejection reason before reject confirmation", () => {
    expect(canRequestRejection("")).toBe(false);
    expect(canRequestRejection("   ")).toBe(false);
    expect(canRequestRejection(COMPLIANCE_APPROVAL_FIXTURES.rejectionReason)).toBe(true);
  });

  it("exposes approve confirmation copy for the dialog", () => {
    const copy = complianceApprovalConfirmCopy("approve");
    expect(copy.title).toBe("Confirm campaign approval");
    expect(copy.label).toBe("Confirm approval");
    expect(copy.outcome).toContain("APPROVED");
  });

  it("pins success notice and fixture identity for UI tests", () => {
    expect(COMPLIANCE_APPROVED_NOTICE).toBe("Campaign approved.");
    expect(COMPLIANCE_APPROVAL_FIXTURES.campaignName).toContain("Compliance Approval");
  });
});
