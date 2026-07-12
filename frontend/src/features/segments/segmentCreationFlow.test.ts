import { describe, expect, it } from "vitest";
import {
  canCreateSegmentsThroughUi,
  emptySegmentCreateForm,
  formatSegmentCreationJourney,
  hasSegmentFormErrors,
  isValidSegmentCreationOrder,
  SEGMENT_CREATE_UI_ROLES,
  SEGMENT_CREATED_NOTICE,
  SEGMENT_CREATION_FIXTURES,
  segmentCreationStepIdsInOrder,
  segmentFormValidationMessages,
  validateSegmentForm,
} from "@/features/segments/segmentCreationFlow";

describe("segmentCreationFlow (item 602)", () => {
  it("documents the UI segment creation journey", () => {
    expect(segmentCreationStepIdsInOrder()).toEqual([
      "open-segments",
      "fill-create-form",
      "submit-create",
      "see-created-segment",
    ]);
    expect(formatSegmentCreationJourney()).toBe(
      "Open Segments → Fill create form → Submit create → See created segment",
    );
    expect(isValidSegmentCreationOrder(segmentCreationStepIdsInOrder())).toBe(true);
    expect(isValidSegmentCreationOrder(["submit-create"] as never)).toBe(false);
  });

  it("allows only admin and campaign manager to create through the UI", () => {
    expect(SEGMENT_CREATE_UI_ROLES).toEqual(["ADMIN", "CAMPAIGN_MANAGER"]);
    expect(canCreateSegmentsThroughUi(["ADMIN"])).toBe(true);
    expect(canCreateSegmentsThroughUi(["CAMPAIGN_MANAGER"])).toBe(true);
    expect(canCreateSegmentsThroughUi(["BI_ANALYST"])).toBe(false);
    expect(canCreateSegmentsThroughUi(["PRODUCT_MANAGER"])).toBe(false);
  });

  it("requires a segment name before create is posted", () => {
    const errors = validateSegmentForm(emptySegmentCreateForm());
    expect(errors.name).toBe(segmentFormValidationMessages.nameRequired);
    expect(hasSegmentFormErrors(errors)).toBe(true);
  });

  it("rejects partially filled criteria rows", () => {
    const errors = validateSegmentForm({
      ...emptySegmentCreateForm(),
      name: "Audience",
      criteria: [
        {
          fieldName: "city",
          operator: "EQUALS",
          value: "",
          joinOperator: "AND",
        },
      ],
    });
    expect(errors.criteria).toBe(segmentFormValidationMessages.criterionValueRequired);
  });

  it("accepts a well-formed create payload with criteria", () => {
    const errors = validateSegmentForm({
      name: SEGMENT_CREATION_FIXTURES.name,
      description: SEGMENT_CREATION_FIXTURES.description,
      visibility: SEGMENT_CREATION_FIXTURES.visibility,
      criteria: [
        {
          fieldName: SEGMENT_CREATION_FIXTURES.cityCriterionField,
          operator: "EQUALS",
          value: SEGMENT_CREATION_FIXTURES.cityCriterionValue,
          joinOperator: "AND",
        },
      ],
    });
    expect(errors).toEqual({});
    expect(hasSegmentFormErrors(errors)).toBe(false);
  });

  it("allows empty criteria list (all-active audience until filters are added)", () => {
    const errors = validateSegmentForm({
      ...emptySegmentCreateForm(),
      name: "Open audience",
      criteria: [],
    });
    expect(errors).toEqual({});
  });

  it("pins success notice and fixture identity for UI tests", () => {
    expect(SEGMENT_CREATED_NOTICE).toBe("Segment created.");
    expect(SEGMENT_CREATION_FIXTURES.name).toContain("UI Created");
  });
});
