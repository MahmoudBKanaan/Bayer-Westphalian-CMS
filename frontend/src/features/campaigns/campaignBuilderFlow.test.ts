import { describe, expect, it } from "vitest";
import { emptyCampaignForm, type CampaignFormPayload } from "@/api/campaigns";
import {
  CAMPAIGN_BUILDER_PAGE_LEAD,
  CAMPAIGN_BUILDER_STEPS,
  PRODUCT_REQUIRED_MESSAGE,
  builderStepStatus,
  firstIncompleteBuilderStep,
  getNextBuilderStepId,
  getPreviousBuilderStepId,
  isBuilderStepComplete,
  validateBuilderStep,
} from "@/features/campaigns/campaignBuilderFlow";

function completeForm(overrides: Partial<CampaignFormPayload> = {}): CampaignFormPayload {
  return {
    ...emptyCampaignForm,
    name: "Life renewal outreach",
    objective: "Promote renewals",
    channel: "EMAIL",
    segmentId: "42000000-0000-4000-8000-000000000201",
    messageSubject: "Renew your cover",
    messageBody: "Dear customer, ...",
    startDate: "2026-09-01",
    endDate: "2026-09-30",
    ...overrides,
  };
}

describe("campaignBuilderFlow (item 592)", () => {
  it("defines a five-step builder path ending in review", () => {
    expect(CAMPAIGN_BUILDER_STEPS).toHaveLength(5);
    expect(CAMPAIGN_BUILDER_STEPS.map((step) => step.id)).toEqual([
      "basics",
      "audience",
      "message",
      "schedule",
      "review",
    ]);
    expect(CAMPAIGN_BUILDER_PAGE_LEAD.toLowerCase()).toContain("review");
    expect(getNextBuilderStepId("basics")).toBe("audience");
    expect(getNextBuilderStepId("review")).toBeNull();
    expect(getPreviousBuilderStepId("basics")).toBeNull();
    expect(getPreviousBuilderStepId("message")).toBe("audience");
  });

  it("validates each step independently so users can progress safely", () => {
    const empty = { ...emptyCampaignForm };
    const productIds: string[] = [];

    expect(validateBuilderStep("basics", empty, productIds).formErrors).toMatchObject({
      name: expect.any(String),
      objective: expect.any(String),
    });
    expect(isBuilderStepComplete("basics", empty, productIds)).toBe(false);

    const basicsOk = completeForm({ segmentId: "", messageSubject: "", messageBody: "" });
    expect(isBuilderStepComplete("basics", basicsOk, productIds)).toBe(true);
    expect(validateBuilderStep("audience", basicsOk, productIds).productError).toBe(
      PRODUCT_REQUIRED_MESSAGE,
    );
    expect(validateBuilderStep("audience", basicsOk, productIds).formErrors.segmentId).toBeTruthy();

    const full = completeForm();
    const products = ["41000000-0000-0000-0000-000000000201"];
    expect(isBuilderStepComplete("audience", full, products)).toBe(true);
    expect(isBuilderStepComplete("message", full, products)).toBe(true);
    expect(isBuilderStepComplete("schedule", full, products)).toBe(true);
    expect(firstIncompleteBuilderStep(full, products)).toBeNull();
  });

  it("flags the first incomplete step for review-time navigation", () => {
    const form = completeForm({ messageBody: "" });
    const products = ["41000000-0000-0000-0000-000000000201"];
    expect(firstIncompleteBuilderStep(form, products)).toBe("message");

    const reviewErrors = validateBuilderStep("review", form, products);
    expect(reviewErrors.formErrors.messageBody).toBeTruthy();
  });

  it("reports step status relative to the current step", () => {
    const form = completeForm();
    const products = ["41000000-0000-0000-0000-000000000201"];
    expect(builderStepStatus("basics", "message", form, products, false)).toBe("complete");
    expect(builderStepStatus("message", "message", form, products, false)).toBe("current");
    expect(builderStepStatus("schedule", "message", form, products, false)).toBe("upcoming");
  });
});
