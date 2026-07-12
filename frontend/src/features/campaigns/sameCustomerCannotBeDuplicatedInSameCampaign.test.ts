import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
  BACKEND_CRITICAL_TEST_CLASS,
  CAMPAIGN_RECIPIENT_UNIQUE_CONSTRAINT,
  DUPLICATE_CAMPAIGN_RECIPIENT_EXCLUSION_CODE,
  DUPLICATE_CAMPAIGN_RECIPIENT_EXPLANATION,
  ELIGIBILITY_RULES_DOC_PATH,
  RECIPIENT_PREVIEW_DOC_PATH,
  SAME_CUSTOMER_CANNOT_BE_DUPLICATED_IN_SAME_CAMPAIGN_FR,
  SAME_CUSTOMER_CANNOT_BE_DUPLICATED_IN_SAME_CAMPAIGN_ITEM,
  SAME_CUSTOMER_CANNOT_BE_DUPLICATED_IN_SAME_CAMPAIGN_RULES,
  SAME_CUSTOMER_CANNOT_BE_DUPLICATED_IN_SAME_CAMPAIGN_STATEMENT,
  customerIsDuplicateOnCampaign,
  duplicateCampaignRecipientPresentation,
  duplicateIsKnownExclusionCode,
  isDuplicateCampaignRecipientExclusion,
} from "@/features/campaigns/sameCustomerCannotBeDuplicatedInSameCampaign";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../../../..");

function readRepoFile(relativeFromRepo: string): string {
  return readFileSync(path.join(repoRoot, relativeFromRepo), "utf8");
}

describe("sameCustomerCannotBeDuplicatedInSameCampaign (item 651)", () => {
  it("locks the critical KB rule identity", () => {
    expect(SAME_CUSTOMER_CANNOT_BE_DUPLICATED_IN_SAME_CAMPAIGN_ITEM).toBe(651);
    expect(SAME_CUSTOMER_CANNOT_BE_DUPLICATED_IN_SAME_CAMPAIGN_STATEMENT).toBe(
      "Same customer cannot be duplicated in same campaign",
    );
    expect(SAME_CUSTOMER_CANNOT_BE_DUPLICATED_IN_SAME_CAMPAIGN_RULES).toEqual(["BR-010"]);
    expect(SAME_CUSTOMER_CANNOT_BE_DUPLICATED_IN_SAME_CAMPAIGN_FR).toEqual(["FR-056"]);
    expect(DUPLICATE_CAMPAIGN_RECIPIENT_EXCLUSION_CODE).toBe("DUPLICATE_CAMPAIGN_RECIPIENT");
    expect(DUPLICATE_CAMPAIGN_RECIPIENT_EXPLANATION).toContain("already assigned");
    expect(CAMPAIGN_RECIPIENT_UNIQUE_CONSTRAINT).toBe(
      "campaign_recipients_campaign_customer_unique",
    );
    expect(BACKEND_CRITICAL_TEST_CLASS).toContain(
      "SameCustomerCannotBeDuplicatedInSameCampaignTests",
    );
  });

  it("treats already-assigned and DUPLICATE_CAMPAIGN_RECIPIENT as exclusions", () => {
    expect(
      customerIsDuplicateOnCampaign({ alreadyAssignedToCampaign: true }),
    ).toBe(true);
    expect(
      customerIsDuplicateOnCampaign({
        alreadyAssignedToCampaign: false,
        exclusionReasonCode: "DUPLICATE_CAMPAIGN_RECIPIENT",
      }),
    ).toBe(true);
    expect(
      customerIsDuplicateOnCampaign({
        alreadyAssignedToCampaign: false,
        exclusionReasonCode: null,
      }),
    ).toBe(false);
    expect(isDuplicateCampaignRecipientExclusion("DUPLICATE_CAMPAIGN_RECIPIENT")).toBe(true);
    expect(isDuplicateCampaignRecipientExclusion("DO_NOT_CONTACT")).toBe(false);
    expect(duplicateIsKnownExclusionCode()).toBe(true);
  });

  it("presents duplicate exclusion with BR-010 hint", () => {
    const presentation = duplicateCampaignRecipientPresentation(2);
    expect(presentation.code).toBe("DUPLICATE_CAMPAIGN_RECIPIENT");
    expect(presentation.title).toMatch(/already|campaign/i);
    expect(presentation.severity).toBe("info");
    expect(presentation.ruleHint).toMatch(/BR-010|duplicate/i);
    expect(presentation.count).toBe(2);
  });

  it("documents BR-010 / DUPLICATE_CAMPAIGN_RECIPIENT in eligibility and recipient preview docs", () => {
    const eligibility = readRepoFile(ELIGIBILITY_RULES_DOC_PATH);
    expect(existsSync(path.join(repoRoot, ELIGIBILITY_RULES_DOC_PATH))).toBe(true);
    expect(eligibility).toContain("651");
    expect(eligibility).toContain("SameCustomerCannotBeDuplicatedInSameCampaignTests");
    expect(eligibility).toContain("DUPLICATE_CAMPAIGN_RECIPIENT");
    expect(eligibility).toMatch(/BR-010|same campaign|duplicate/i);

    const preview = readRepoFile(RECIPIENT_PREVIEW_DOC_PATH);
    expect(existsSync(path.join(repoRoot, RECIPIENT_PREVIEW_DOC_PATH))).toBe(true);
    expect(preview).toMatch(/duplicate|BR-010|DUPLICATE/i);
  });
});
