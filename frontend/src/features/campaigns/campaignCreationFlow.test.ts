import { describe, expect, it } from "vitest";
import { emptyCampaignForm } from "@/api/campaigns";
import {
  CAMPAIGN_CREATE_UI_ROLES,
  CAMPAIGN_CREATION_FIXTURES,
  CAMPAIGN_DRAFT_CREATED_NOTICE,
  campaignBuilderStepShortTitles,
  campaignCreationStepIdsInOrder,
  canCreateCampaignDraft,
  canCreateCampaignsThroughUi,
  formatCampaignCreationJourney,
  isValidCampaignCreationOrder,
  validateCampaignCreateDraft,
} from "@/features/campaigns/campaignCreationFlow";
import { campaignFormValidationMessages } from "@/features/campaigns/campaignFormValidation";

const completeForm = {
  ...emptyCampaignForm,
  name: CAMPAIGN_CREATION_FIXTURES.name,
  objective: CAMPAIGN_CREATION_FIXTURES.objective,
  segmentId: CAMPAIGN_CREATION_FIXTURES.segmentId,
  channel: CAMPAIGN_CREATION_FIXTURES.channel,
  messageSubject: CAMPAIGN_CREATION_FIXTURES.messageSubject,
  messageBody: CAMPAIGN_CREATION_FIXTURES.messageBody,
  startDate: CAMPAIGN_CREATION_FIXTURES.startDate,
  endDate: CAMPAIGN_CREATION_FIXTURES.endDate,
};

describe("campaignCreationFlow (item 603)", () => {
  it("documents the UI campaign creation journey", () => {
    expect(campaignCreationStepIdsInOrder()).toEqual([
      "open-builder",
      "complete-builder-steps",
      "create-draft",
      "see-created-draft",
    ]);
    expect(formatCampaignCreationJourney()).toBe(
      "Open Campaign Builder → Complete builder steps → Create draft → See created draft",
    );
    expect(isValidCampaignCreationOrder(campaignCreationStepIdsInOrder())).toBe(true);
    expect(isValidCampaignCreationOrder(["create-draft"] as never)).toBe(false);
  });

  it("allows only admin and campaign manager to create through the UI", () => {
    expect(CAMPAIGN_CREATE_UI_ROLES).toEqual(["ADMIN", "CAMPAIGN_MANAGER"]);
    expect(canCreateCampaignsThroughUi(["ADMIN"])).toBe(true);
    expect(canCreateCampaignsThroughUi(["CAMPAIGN_MANAGER"])).toBe(true);
    expect(canCreateCampaignsThroughUi(["PRODUCT_MANAGER"])).toBe(false);
    expect(canCreateCampaignsThroughUi(["BI_ANALYST"])).toBe(false);
  });

  it("requires full form and product before draft create is allowed", () => {
    expect(canCreateCampaignDraft(emptyCampaignForm, [])).toBe(false);
    expect(canCreateCampaignDraft(completeForm, [])).toBe(false);
    expect(canCreateCampaignDraft(completeForm, [CAMPAIGN_CREATION_FIXTURES.productId])).toBe(
      true,
    );
  });

  it("surfaces name and product errors on create-draft validation", () => {
    const result = validateCampaignCreateDraft(emptyCampaignForm, []);
    expect(result.formErrors.name).toBe(campaignFormValidationMessages.nameRequired);
    expect(result.productError.length).toBeGreaterThan(0);
  });

  it("lists builder step short titles used in the UI stepper", () => {
    expect(campaignBuilderStepShortTitles()).toEqual([
      "Basics",
      "Audience",
      "Message",
      "Schedule",
      "Review",
    ]);
  });

  it("pins success notice and fixture identity for UI tests", () => {
    expect(CAMPAIGN_DRAFT_CREATED_NOTICE).toBe("Campaign draft created.");
    expect(CAMPAIGN_CREATION_FIXTURES.name).toContain("UI Created");
  });
});
