import { describe, expect, it } from "vitest";
import { emptyCampaignForm } from "@/api/campaigns";
import {
  campaignFormValidationMessages,
  hasCampaignFormErrors,
  validateCampaignForm,
  validateRejectCampaignForm,
} from "@/features/campaigns/campaignFormValidation";

describe("campaign form validation messages (item 242)", () => {
  it("requires name, objective, segment, message subject, message body, schedule date, and end date", () => {
    const errors = validateCampaignForm({
      ...emptyCampaignForm,
      name: "   ",
      objective: "",
    });

    expect(errors.name).toBe(campaignFormValidationMessages.nameRequired);
    expect(errors.objective).toBe(campaignFormValidationMessages.objectiveRequired);
    expect(errors.segmentId).toBe(campaignFormValidationMessages.segmentRequired);
    expect(errors.messageSubject).toBe(campaignFormValidationMessages.messageSubjectRequired);
    expect(errors.messageBody).toBe(campaignFormValidationMessages.messageBodyRequired);
    expect(errors.startDate).toBe(campaignFormValidationMessages.startDateRequired);
    expect(errors.endDate).toBe(campaignFormValidationMessages.endDateRequired);
    expect(hasCampaignFormErrors(errors)).toBe(true);
  });

  it("requires valid schedule order", () => {
    const errors = validateCampaignForm({
      ...emptyCampaignForm,
      name: "Life renewal",
      objective: "Promote renewals",
      segmentId: "42000000-0000-4000-8000-000000000201",
      messageBody: "Dear customer, ...",
      startDate: "2026-10-15",
      endDate: "2026-10-01",
    });

    expect(errors.endDate).toBe(campaignFormValidationMessages.endDateBeforeStart);
    expect(hasCampaignFormErrors(errors)).toBe(true);
  });

  it("enforces name and message subject max length", () => {
    const errors = validateCampaignForm({
      ...emptyCampaignForm,
      name: "N".repeat(256),
      objective: "Promote renewals",
      messageSubject: "S".repeat(256),
    });

    expect(errors.name).toBe(campaignFormValidationMessages.nameMaxLength);
    expect(errors.messageSubject).toBe(campaignFormValidationMessages.messageSubjectMaxLength);
  });

  it("rejects invalid segment id uuid when provided", () => {
    const errors = validateCampaignForm({
      ...emptyCampaignForm,
      name: "Life renewal",
      objective: "Promote renewals",
      segmentId: "not-a-uuid",
    });

    expect(errors.segmentId).toBe(campaignFormValidationMessages.segmentIdInvalid);
  });

  it("accepts existing deterministic demo segment UUIDs", () => {
    const errors = validateCampaignForm({
      ...emptyCampaignForm,
      name: "Demo beneficiary outreach",
      objective: "Use the seeded reusable segment",
      segmentId: "40000000-0000-0000-0000-000000000101",
      channel: "EMAIL",
      messageSubject: "Plan your next financial step",
      messageBody: "Dear customer, ...",
      startDate: "2026-09-01",
      endDate: "2026-09-30",
    });

    expect(errors.segmentId).toBeUndefined();
  });

  it("accepts a complete valid draft form", () => {
    const errors = validateCampaignForm({
      name: "Life renewal outreach",
      objective: "Promote life insurance renewals",
      segmentId: "42000000-0000-4000-8000-000000000201",
      channel: "EMAIL",
      messageSubject: "Renew your cover",
      messageBody: "Dear customer, ...",
      startDate: "2026-09-01",
      endDate: "2026-09-30",
    });

    expect(errors).toEqual({});
    expect(hasCampaignFormErrors(errors)).toBe(false);
  });

  it("requires rejection reason on reject form", () => {
    const errors = validateRejectCampaignForm({
      rejectionReason: "  ",
      complianceReviewNotes: "optional notes",
    });

    expect(errors.rejectionReason).toBe(campaignFormValidationMessages.rejectionReasonRequired);
    expect(hasCampaignFormErrors(errors)).toBe(true);
  });

  it("accepts reject form with formal reason", () => {
    const errors = validateRejectCampaignForm({
      rejectionReason: "Missing consent language",
      complianceReviewNotes: "",
    });

    expect(errors).toEqual({});
  });
});
