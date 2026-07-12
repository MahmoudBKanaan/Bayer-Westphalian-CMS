/**
 * Campaign creation UI flow (KB FR-050 / FR-057 / item 603).
 *
 * Acceptance contract for creating a DRAFT campaign through the multi-step
 * Campaign Builder UI. Step field rules live in {@link campaignBuilderFlow};
 * full form rules live in {@link campaignFormValidation}.
 */

import type { CampaignFormPayload } from "@/api/campaigns";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import {
  CAMPAIGN_BUILDER_PAGE_LEAD,
  CAMPAIGN_BUILDER_STEPS,
  PRODUCT_REQUIRED_MESSAGE,
  validateBuilderStep,
} from "@/features/campaigns/campaignBuilderFlow";
import {
  campaignFormValidationMessages,
  hasCampaignFormErrors,
  validateCampaignForm,
} from "@/features/campaigns/campaignFormValidation";

/** Roles that may create campaigns through the UI (matches CAMPAIGN_MANAGE_ROLES). */
export const CAMPAIGN_CREATE_UI_ROLES: SystemRoleName[] = ["ADMIN", "CAMPAIGN_MANAGER"];

export const CAMPAIGN_BUILDER_PAGE_TITLE = "Campaign Builder";
/** Accessible name for multi-step builder forms (item 603 / keyboard nav item 608). */
export const CAMPAIGN_BUILDER_FORM_ARIA_LABEL = "Campaign builder form";
export const CAMPAIGN_CREATE_DRAFT_LABEL = "Create draft";
export const CAMPAIGN_SUBMIT_FOR_REVIEW_LABEL = "Submit for review";
export const CAMPAIGN_DRAFT_CREATED_NOTICE = "Campaign draft created.";
export const CAMPAIGN_SUBMITTED_NOTICE = "Campaign submitted for compliance review.";

export { CAMPAIGN_BUILDER_PAGE_LEAD, PRODUCT_REQUIRED_MESSAGE, campaignFormValidationMessages };

export type CampaignCreationStepId =
  | "open-builder"
  | "complete-builder-steps"
  | "create-draft"
  | "see-created-draft";

export type CampaignCreationStepDefinition = {
  id: CampaignCreationStepId;
  index: number;
  title: string;
  description: string;
};

/** UI acceptance steps for “Campaign creation works through UI” (item 603). */
export const CAMPAIGN_CREATION_FLOW_STEPS: CampaignCreationStepDefinition[] = [
  {
    id: "open-builder",
    index: 0,
    title: "Open Campaign Builder",
    description: "Campaign Manager or Admin opens /campaign-builder.",
  },
  {
    id: "complete-builder-steps",
    index: 1,
    title: "Complete builder steps",
    description:
      "Walk basics → audience/product → message → schedule → review with per-step validation.",
  },
  {
    id: "create-draft",
    index: 2,
    title: "Create draft",
    description: "POST /api/campaigns then select products; campaign status becomes DRAFT.",
  },
  {
    id: "see-created-draft",
    index: 3,
    title: "See created draft",
    description: "Success notice and draft status confirm the campaign was created.",
  },
];

/** Deterministic fixtures for Playwright / integration campaign creation. */
export const CAMPAIGN_CREATION_FIXTURES = {
  name: "UI Created Outreach Campaign",
  objective: "Validate campaign creation through the builder UI",
  channel: "EMAIL" as const,
  messageSubject: "UI campaign subject",
  messageBody: "Controlled UI campaign creation message body.",
  startDate: "2026-09-01",
  endDate: "2026-09-30",
  campaignId: "50000000-0000-0000-0000-00000000c603",
  segmentId: "40000000-0000-0000-0000-00000000e201",
  segmentName: "E2E Eligible Audience",
  productId: "30000000-0000-0000-0000-00000000e301",
  productName: "E2E Investment Fund",
} as const;

export function canCreateCampaignsThroughUi(roles: readonly SystemRoleName[]): boolean {
  return roles.some((role) => CAMPAIGN_CREATE_UI_ROLES.includes(role));
}

/**
 * True when the builder has enough data to create a draft (full form + product).
 */
export function canCreateCampaignDraft(
  form: CampaignFormPayload,
  selectedProductIds: readonly string[],
): boolean {
  if (hasCampaignFormErrors(validateCampaignForm(form))) {
    return false;
  }
  return selectedProductIds.length > 0 && selectedProductIds[0]!.trim().length > 0;
}

/**
 * Validates review-step create readiness (all builder steps + product).
 */
export function validateCampaignCreateDraft(
  form: CampaignFormPayload,
  selectedProductIds: string[],
): { formErrors: ReturnType<typeof validateCampaignForm>; productError: string } {
  return validateBuilderStep("review", form, selectedProductIds);
}

export function campaignCreationStepIdsInOrder(): CampaignCreationStepId[] {
  return [...CAMPAIGN_CREATION_FLOW_STEPS]
    .sort((left, right) => left.index - right.index)
    .map((step) => step.id);
}

export function formatCampaignCreationJourney(
  steps: readonly CampaignCreationStepDefinition[] = CAMPAIGN_CREATION_FLOW_STEPS,
): string {
  return steps
    .slice()
    .sort((left, right) => left.index - right.index)
    .map((step) => step.title)
    .join(" → ");
}

export function isValidCampaignCreationOrder(
  observed: readonly CampaignCreationStepId[],
): boolean {
  const expected = campaignCreationStepIdsInOrder();
  if (observed.length !== expected.length) {
    return false;
  }
  return observed.every((stepId, index) => stepId === expected[index]);
}

/** Builder step short titles used by the UI stepper (item 592 / 603). */
export function campaignBuilderStepShortTitles(): string[] {
  return CAMPAIGN_BUILDER_STEPS.map((step) => step.shortTitle);
}
