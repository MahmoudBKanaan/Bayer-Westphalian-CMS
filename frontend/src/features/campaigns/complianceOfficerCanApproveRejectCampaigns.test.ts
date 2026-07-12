import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  ALLOWED_REVIEW_ROLES,
  APPROVE_API_PATH,
  BACKEND_CRITICAL_TEST_CLASS,
  COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_FR,
  COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_ITEM,
  COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_RULES,
  COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_STATEMENT,
  COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_TEST_CASES,
  COMPLIANCE_OFFICER_GUIDE_PATH,
  COMPLIANCE_REVIEW_DOC_PATH,
  PRIMARY_REVIEW_ROLE,
  REJECT_API_PATH,
  campaignManagerCannotApproveThroughUi,
  complianceOfficerCanApproveThroughUi,
  complianceReviewsWhileManagersWrite,
  reviewRolesMatchKbEditors,
} from "@/features/campaigns/complianceOfficerCanApproveRejectCampaigns";
import {
  COMPLIANCE_APPROVE_UI_ROLES,
  canApproveCampaignsThroughUi,
} from "@/features/campaigns/complianceApprovalFlow";
import { CAMPAIGN_MANAGE_ROLES, CAMPAIGN_REVIEW_ROLES } from "@/features/auth/permissions";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("complianceOfficerCanApproveRejectCampaigns (item 655)", () => {
  it("locks the critical KB rule identity", () => {
    expect(COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_ITEM).toBe(655);
    expect(COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_STATEMENT).toBe(
      "Compliance Officer can approve/reject campaigns",
    );
    expect(COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_TEST_CASES).toEqual(["TC-011"]);
    expect(COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_FR).toEqual(["FR-059"]);
    expect(COMPLIANCE_OFFICER_CAN_APPROVE_REJECT_CAMPAIGNS_RULES).toEqual(["BR-005"]);
    expect(PRIMARY_REVIEW_ROLE).toBe("COMPLIANCE_OFFICER");
    expect(ALLOWED_REVIEW_ROLES).toEqual(["ADMIN", "COMPLIANCE_OFFICER"]);
    expect(APPROVE_API_PATH).toBe("POST /api/campaigns/{id}/approve");
    expect(REJECT_API_PATH).toBe("POST /api/campaigns/{id}/reject");
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain(
      "ComplianceOfficerCanApproveRejectCampaignsTests",
    );
  });

  it("allows Compliance Officer (and Admin) to approve; blocks Campaign Manager", () => {
    expect(complianceOfficerCanApproveThroughUi()).toBe(true);
    expect(canApproveCampaignsThroughUi(["ADMIN"])).toBe(true);
    expect(campaignManagerCannotApproveThroughUi()).toBe(true);
    expect(reviewRolesMatchKbEditors()).toBe(true);
    expect(complianceReviewsWhileManagersWrite()).toBe(true);
    expect(COMPLIANCE_APPROVE_UI_ROLES).toEqual(["ADMIN", "COMPLIANCE_OFFICER"]);
    expect(CAMPAIGN_REVIEW_ROLES).toContain("COMPLIANCE_OFFICER");
    expect(CAMPAIGN_MANAGE_ROLES).not.toContain("COMPLIANCE_OFFICER");
    expect(canApproveCampaignsThroughUi(["BI_ANALYST", "PRODUCT_MANAGER"])).toBe(false);
  });

  it("documents FR-059 / TC-011 compliance approval in module and officer guide", () => {
    const moduleDoc = readRepoFile(COMPLIANCE_REVIEW_DOC_PATH);
    expect(existsSync(path.join(repoRoot, COMPLIANCE_REVIEW_DOC_PATH))).toBe(true);
    expect(moduleDoc).toContain("655");
    expect(moduleDoc).toContain("ComplianceOfficerCanApproveRejectCampaignsTests");
    expect(moduleDoc).toContain("TC-011");
    expect(moduleDoc).toContain("FR-059");
    expect(moduleDoc).toMatch(/COMPLIANCE_OFFICER|approve or reject/i);

    const guide = readRepoFile(COMPLIANCE_OFFICER_GUIDE_PATH);
    expect(existsSync(path.join(repoRoot, COMPLIANCE_OFFICER_GUIDE_PATH))).toBe(true);
    expect(guide).toMatch(/approve|reject|compliance/i);
  });
});
