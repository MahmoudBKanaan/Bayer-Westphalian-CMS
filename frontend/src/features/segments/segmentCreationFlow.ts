/**
 * Segment creation UI flow (KB FR-077 / item 201 / item 602).
 *
 * Pure rules for creating reusable audience segments through the React Segments
 * page: form defaults, validation, success copy, roles, and acceptance steps.
 */

import type {
  SegmentFormPayload,
  SegmentVisibility,
  SegmentView,
} from "@/api/segments";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";

/** Roles that may create segments through the UI (matches SEGMENT_CREATE_ROLES). */
export const SEGMENT_CREATE_UI_ROLES: SystemRoleName[] = ["ADMIN", "CAMPAIGN_MANAGER"];

export const SEGMENT_CREATE_SECTION_HEADING = "Create segment";
export const SEGMENT_CREATE_SECTION_HINT =
  "Save a reusable audience definition (Campaign Manager)";
export const SEGMENT_CREATE_SUBMIT_LABEL = "Create segment";
export const SEGMENT_CREATE_FORM_ARIA_LABEL = "Create segment form";
export const SEGMENT_CREATED_NOTICE = "Segment created.";
export const SEGMENT_LIST_TABLE_ARIA_LABEL = "Segments table";

export const SEGMENT_VISIBILITIES: SegmentVisibility[] = ["PRIVATE", "TEAM", "GLOBAL"];

export const segmentFormValidationMessages = {
  nameRequired: "Segment name is required.",
  nameMaxLength: "Segment name must be 255 characters or fewer.",
  visibilityRequired: "Visibility is required.",
  criterionFieldRequired: "Each criterion needs a field name.",
  criterionValueRequired: "Each criterion with a field needs a value.",
} as const;

export type SegmentFormErrors = {
  name?: string;
  visibility?: string;
  criteria?: string;
};

export type SegmentCreationStepId =
  | "open-segments"
  | "fill-create-form"
  | "submit-create"
  | "see-created-segment";

export type SegmentCreationStepDefinition = {
  id: SegmentCreationStepId;
  index: number;
  title: string;
  description: string;
};

/** UI acceptance steps for “Segment creation works through UI” (item 602). */
export const SEGMENT_CREATION_FLOW_STEPS: SegmentCreationStepDefinition[] = [
  {
    id: "open-segments",
    index: 0,
    title: "Open Segments",
    description: "Campaign Manager or Admin opens /segments and sees the create panel.",
  },
  {
    id: "fill-create-form",
    index: 1,
    title: "Fill create form",
    description: "Enter name, description, visibility, and optional criteria rules.",
  },
  {
    id: "submit-create",
    index: 2,
    title: "Submit create",
    description: "Client validation then POST /api/segments with the segment definition.",
  },
  {
    id: "see-created-segment",
    index: 3,
    title: "See created segment",
    description: "Success notice appears and the segment is listed for reuse.",
  },
];

/** Deterministic fixtures for Playwright / integration segment creation. */
export const SEGMENT_CREATION_FIXTURES = {
  name: "UI Created Eligible Audience",
  description: "Controlled UI segment creation fixture",
  visibility: "TEAM" as SegmentVisibility,
  cityCriterionField: "city",
  cityCriterionValue: "Munich",
  id: "40000000-0000-0000-0000-00000000s602",
} as const;

export function emptySegmentCreateForm(): SegmentFormPayload {
  return {
    name: "",
    description: "",
    visibility: "PRIVATE",
    criteria: [],
  };
}

/**
 * Validates create/edit segment form fields before API calls (FR-077).
 * Incomplete criteria rows (missing field+value) are reported when partially filled.
 */
export function validateSegmentForm(value: SegmentFormPayload): SegmentFormErrors {
  const errors: SegmentFormErrors = {};
  const name = value.name.trim();
  if (name.length === 0) {
    errors.name = segmentFormValidationMessages.nameRequired;
  } else if (value.name.length > 255) {
    errors.name = segmentFormValidationMessages.nameMaxLength;
  }

  if (value.visibility == null || String(value.visibility).trim().length === 0) {
    errors.visibility = segmentFormValidationMessages.visibilityRequired;
  }

  for (const criterion of value.criteria) {
    const field = criterion.fieldName.trim();
    const criterionValue = criterion.value.trim();
    if (field.length === 0 && criterionValue.length === 0) {
      continue;
    }
    if (field.length === 0) {
      errors.criteria = segmentFormValidationMessages.criterionFieldRequired;
      break;
    }
    if (criterionValue.length === 0) {
      errors.criteria = segmentFormValidationMessages.criterionValueRequired;
      break;
    }
  }

  return errors;
}

export function hasSegmentFormErrors(errors: SegmentFormErrors): boolean {
  return Object.values(errors).some((message) => message != null && message.length > 0);
}

export function segmentViewToForm(segment: SegmentView): SegmentFormPayload {
  return {
    name: segment.name,
    description: segment.description ?? "",
    visibility: segment.visibility,
    criteria: segment.criteria.map((criterion) => ({
      fieldName: criterion.fieldName,
      operator: criterion.operator,
      value: criterion.value,
      logicalGroup: criterion.logicalGroup ?? "",
      joinOperator: criterion.joinOperator,
    })),
  };
}

export function canCreateSegmentsThroughUi(roles: readonly SystemRoleName[]): boolean {
  return roles.some((role) => SEGMENT_CREATE_UI_ROLES.includes(role));
}

export function segmentCreationStepIdsInOrder(): SegmentCreationStepId[] {
  return [...SEGMENT_CREATION_FLOW_STEPS]
    .sort((left, right) => left.index - right.index)
    .map((step) => step.id);
}

export function formatSegmentCreationJourney(
  steps: readonly SegmentCreationStepDefinition[] = SEGMENT_CREATION_FLOW_STEPS,
): string {
  return steps
    .slice()
    .sort((left, right) => left.index - right.index)
    .map((step) => step.title)
    .join(" → ");
}

export function isValidSegmentCreationOrder(
  observed: readonly SegmentCreationStepId[],
): boolean {
  const expected = segmentCreationStepIdsInOrder();
  if (observed.length !== expected.length) {
    return false;
  }
  return observed.every((stepId, index) => stepId === expected[index]);
}
