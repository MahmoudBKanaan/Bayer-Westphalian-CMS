import type { CampaignFormPayload } from "@/api/campaigns";
import {
  campaignFormValidationMessages,
  type CampaignFormErrors,
} from "@/features/campaigns/campaignFormValidation";

/**
 * Campaign builder multi-step flow (KB item 592 / FR-050 / FR-057).
 *
 * Guides Campaign Managers through definition → audience/product → message →
 * schedule → review before creating a draft and submitting for compliance.
 */

export type CampaignBuilderStepId =
  | "basics"
  | "audience"
  | "message"
  | "schedule"
  | "review";

export type CampaignBuilderStepDefinition = {
  id: CampaignBuilderStepId;
  index: number;
  title: string;
  shortTitle: string;
  description: string;
  /** Primary action label shown on this step (before draft exists). */
  primaryActionLabel: string;
};

export const CAMPAIGN_BUILDER_PAGE_LEAD =
  "Follow the steps to define the campaign, choose audience and product, write the message, set the schedule, then review and create a draft for compliance.";

export const CAMPAIGN_BUILDER_STEPS: CampaignBuilderStepDefinition[] = [
  {
    id: "basics",
    index: 0,
    title: "Campaign basics",
    shortTitle: "Basics",
    description: "Name the campaign, state the objective, and pick a channel.",
    primaryActionLabel: "Continue to audience",
  },
  {
    id: "audience",
    index: 1,
    title: "Audience & product",
    shortTitle: "Audience",
    description: "Select the target segment and the product this campaign promotes.",
    primaryActionLabel: "Continue to message",
  },
  {
    id: "message",
    index: 2,
    title: "Message",
    shortTitle: "Message",
    description: "Write subject and body. Optional AI copy still requires human approval.",
    primaryActionLabel: "Continue to schedule",
  },
  {
    id: "schedule",
    index: 3,
    title: "Schedule",
    shortTitle: "Schedule",
    description: "Set the planned start and end dates for the campaign window.",
    primaryActionLabel: "Continue to review",
  },
  {
    id: "review",
    index: 4,
    title: "Review & create",
    shortTitle: "Review",
    description: "Confirm the draft details, create the draft, then submit for compliance review.",
    primaryActionLabel: "Create draft",
  },
];

export const PRODUCT_REQUIRED_MESSAGE = "Product is required.";

export function getCampaignBuilderStep(
  stepId: CampaignBuilderStepId,
): CampaignBuilderStepDefinition {
  const step = CAMPAIGN_BUILDER_STEPS.find((entry) => entry.id === stepId);
  if (step == null) {
    return CAMPAIGN_BUILDER_STEPS[0]!;
  }
  return step;
}

export function getNextBuilderStepId(
  stepId: CampaignBuilderStepId,
): CampaignBuilderStepId | null {
  const current = getCampaignBuilderStep(stepId);
  const next = CAMPAIGN_BUILDER_STEPS[current.index + 1];
  return next?.id ?? null;
}

export function getPreviousBuilderStepId(
  stepId: CampaignBuilderStepId,
): CampaignBuilderStepId | null {
  const current = getCampaignBuilderStep(stepId);
  if (current.index <= 0) {
    return null;
  }
  return CAMPAIGN_BUILDER_STEPS[current.index - 1]?.id ?? null;
}

/**
 * Field-level errors for a single builder step (subset of full form validation).
 */
export function validateBuilderStep(
  stepId: CampaignBuilderStepId,
  form: CampaignFormPayload,
  selectedProductIds: string[],
): { formErrors: CampaignFormErrors; productError: string } {
  const formErrors: CampaignFormErrors = {};
  let productError = "";

  switch (stepId) {
    case "basics": {
      if (form.name.trim().length === 0) {
        formErrors.name = campaignFormValidationMessages.nameRequired;
      } else if (form.name.length > 255) {
        formErrors.name = campaignFormValidationMessages.nameMaxLength;
      }
      if (form.objective.trim().length === 0) {
        formErrors.objective = campaignFormValidationMessages.objectiveRequired;
      }
      if (form.channel == null || String(form.channel).trim().length === 0) {
        formErrors.channel = campaignFormValidationMessages.channelRequired;
      }
      break;
    }
    case "audience": {
      const segmentId = form.segmentId.trim();
      if (segmentId.length === 0) {
        formErrors.segmentId = campaignFormValidationMessages.segmentRequired;
      } else if (!isUuid(segmentId)) {
        formErrors.segmentId = campaignFormValidationMessages.segmentIdInvalid;
      }
      if (selectedProductIds.length === 0) {
        productError = PRODUCT_REQUIRED_MESSAGE;
      }
      break;
    }
    case "message": {
      if (form.messageSubject.trim().length === 0) {
        formErrors.messageSubject = campaignFormValidationMessages.messageSubjectRequired;
      } else if (form.messageSubject.length > 255) {
        formErrors.messageSubject = campaignFormValidationMessages.messageSubjectMaxLength;
      }
      if (form.messageBody.trim().length === 0) {
        formErrors.messageBody = campaignFormValidationMessages.messageBodyRequired;
      }
      break;
    }
    case "schedule": {
      if (form.startDate.trim().length === 0) {
        formErrors.startDate = campaignFormValidationMessages.startDateRequired;
      }
      if (form.endDate.trim().length === 0) {
        formErrors.endDate = campaignFormValidationMessages.endDateRequired;
      }
      if (
        form.startDate.trim().length > 0 &&
        form.endDate.trim().length > 0 &&
        form.endDate < form.startDate
      ) {
        formErrors.endDate = campaignFormValidationMessages.endDateBeforeStart;
      }
      break;
    }
    case "review": {
      // Full gate before create: reuse every prior step.
      for (const step of CAMPAIGN_BUILDER_STEPS) {
        if (step.id === "review") {
          continue;
        }
        const partial = validateBuilderStep(step.id, form, selectedProductIds);
        Object.assign(formErrors, partial.formErrors);
        if (partial.productError) {
          productError = partial.productError;
        }
      }
      break;
    }
    default:
      break;
  }

  return { formErrors, productError };
}

export function isBuilderStepComplete(
  stepId: CampaignBuilderStepId,
  form: CampaignFormPayload,
  selectedProductIds: string[],
): boolean {
  if (stepId === "review") {
    return false;
  }
  const { formErrors, productError } = validateBuilderStep(stepId, form, selectedProductIds);
  return Object.keys(formErrors).length === 0 && productError === "";
}

/**
 * First incomplete step before review (for jumping users to the problem area).
 */
export function firstIncompleteBuilderStep(
  form: CampaignFormPayload,
  selectedProductIds: string[],
): CampaignBuilderStepId | null {
  for (const step of CAMPAIGN_BUILDER_STEPS) {
    if (step.id === "review") {
      continue;
    }
    if (!isBuilderStepComplete(step.id, form, selectedProductIds)) {
      return step.id;
    }
  }
  return null;
}

export function builderStepStatus(
  stepId: CampaignBuilderStepId,
  currentStepId: CampaignBuilderStepId,
  form: CampaignFormPayload,
  selectedProductIds: string[],
  draftCreated: boolean,
): "current" | "complete" | "upcoming" {
  const current = getCampaignBuilderStep(currentStepId);
  const step = getCampaignBuilderStep(stepId);
  if (step.id === currentStepId) {
    return "current";
  }
  if (step.index < current.index) {
    return "complete";
  }
  if (draftCreated && step.id === "review") {
    return "complete";
  }
  if (
    step.id !== "review" &&
    isBuilderStepComplete(step.id, form, selectedProductIds) &&
    step.index < current.index
  ) {
    return "complete";
  }
  return "upcoming";
}

function isUuid(value: string) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
}
