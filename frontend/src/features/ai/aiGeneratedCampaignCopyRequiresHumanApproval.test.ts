import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  AI_FEATURES_DOC_PATH,
  AI_GENERATED_CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL_AI,
  AI_GENERATED_CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL_COMP,
  AI_GENERATED_CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL_ITEM,
  AI_GENERATED_CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL_STATEMENT,
  AI_LIMITATIONS_DOC_PATH,
  BACKEND_CRITICAL_TEST_CLASS,
  CAMPAIGN_COPY_HUMAN_APPROVAL_UI_NOTICE,
  CAMPAIGN_COPY_RECOMMENDATION_TYPE,
  CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL,
  COMPANION_CAMPAIGN_COPY_SERVICE_TEST_CLASS,
  humanCopyApprovalGrantsCampaignCompliance,
  isCampaignCopyHumanApproved,
  isCampaignCopyPendingHumanApproval,
} from "@/features/ai/aiGeneratedCampaignCopyRequiresHumanApproval";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("aiGeneratedCampaignCopyRequiresHumanApproval (item 662)", () => {
  it("locks the critical KB rule identity", () => {
    expect(AI_GENERATED_CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL_ITEM).toBe(662);
    expect(AI_GENERATED_CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL_STATEMENT).toBe(
      "AI-generated campaign copy requires human approval",
    );
    expect(AI_GENERATED_CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL_AI).toEqual(["AI-005"]);
    expect(AI_GENERATED_CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL_COMP).toEqual(["COMP-005"]);
    expect(CAMPAIGN_COPY_RECOMMENDATION_TYPE).toBe("COPY");
    expect(CAMPAIGN_COPY_REQUIRES_HUMAN_APPROVAL).toBe(true);
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain(
      "AiGeneratedCampaignCopyRequiresHumanApprovalTests",
    );
    expect(COMPANION_CAMPAIGN_COPY_SERVICE_TEST_CLASS).toContain("CampaignCopyServiceTests");
    expect(CAMPAIGN_COPY_HUMAN_APPROVAL_UI_NOTICE).toMatch(/human approval/i);
  });

  it("treats generated copy as pending until a human approver is recorded", () => {
    const pending = {
      requiresHumanApproval: true,
      humanApproved: false,
      approvedByUserId: null,
      recommendationType: "COPY",
    };
    const approved = {
      requiresHumanApproval: true,
      humanApproved: true,
      approvedByUserId: "user-1",
      recommendationType: "COPY",
    };

    expect(isCampaignCopyPendingHumanApproval(pending)).toBe(true);
    expect(isCampaignCopyPendingHumanApproval(approved)).toBe(false);
    expect(isCampaignCopyHumanApproved(pending)).toBe(false);
    expect(isCampaignCopyHumanApproved(approved)).toBe(true);
    expect(isCampaignCopyPendingHumanApproval(null)).toBe(true);
    expect(humanCopyApprovalGrantsCampaignCompliance()).toBe(false);
  });

  it("documents AI-005 human approval in limitations and feature docs", () => {
    const limitationsPath = path.join(repoRoot, AI_LIMITATIONS_DOC_PATH);
    const featuresPath = path.join(repoRoot, AI_FEATURES_DOC_PATH);
    expect(existsSync(limitationsPath)).toBe(true);
    expect(existsSync(featuresPath)).toBe(true);

    const limitations = readRepoFile(AI_LIMITATIONS_DOC_PATH);
    expect(limitations).toContain("662");
    expect(limitations).toContain("AiGeneratedCampaignCopyRequiresHumanApprovalTests");
    expect(limitations).toMatch(/AI-005|requiresHumanApproval|human approval/i);

    const features = readRepoFile(AI_FEATURES_DOC_PATH);
    expect(features).toMatch(/campaign copy|AI-005|human approval/i);
  });
});
