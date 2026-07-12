import { describe, expect, it } from "vitest";
import {
  canCreateProductsThroughUi,
  emptyProductForm,
  formatProductCreationJourney,
  hasProductFormErrors,
  isValidProductCreationOrder,
  PRODUCT_CREATE_UI_ROLES,
  PRODUCT_CREATED_NOTICE,
  PRODUCT_CREATION_FIXTURES,
  productCreationStepIdsInOrder,
  productFormValidationMessages,
  validateProductForm,
} from "@/features/products/productCreationFlow";

describe("productCreationFlow (item 601)", () => {
  it("documents the UI product creation journey", () => {
    expect(productCreationStepIdsInOrder()).toEqual([
      "open-products",
      "fill-create-form",
      "submit-create",
      "see-created-product",
    ]);
    expect(formatProductCreationJourney()).toBe(
      "Open Products → Fill create form → Submit create → See created product",
    );
    expect(isValidProductCreationOrder(productCreationStepIdsInOrder())).toBe(true);
    expect(isValidProductCreationOrder(["submit-create"] as never)).toBe(false);
  });

  it("allows only admin and product manager to create through the UI", () => {
    expect(PRODUCT_CREATE_UI_ROLES).toEqual(["ADMIN", "PRODUCT_MANAGER"]);
    expect(canCreateProductsThroughUi(["ADMIN"])).toBe(true);
    expect(canCreateProductsThroughUi(["PRODUCT_MANAGER"])).toBe(true);
    expect(canCreateProductsThroughUi(["CAMPAIGN_MANAGER"])).toBe(false);
    expect(canCreateProductsThroughUi(["BI_ANALYST"])).toBe(false);
  });

  it("requires a product name before create is posted", () => {
    const errors = validateProductForm(emptyProductForm());
    expect(errors.name).toBe(productFormValidationMessages.nameRequired);
    expect(hasProductFormErrors(errors)).toBe(true);
  });

  it("validates price and duration formats when provided", () => {
    const errors = validateProductForm({
      ...emptyProductForm(),
      name: "Cover",
      price: "-10",
      durationMonths: "abc",
    });
    expect(errors.price).toBe(productFormValidationMessages.priceInvalid);
    expect(errors.durationMonths).toBe(productFormValidationMessages.durationInvalid);
  });

  it("accepts a well-formed create payload", () => {
    const errors = validateProductForm({
      ...emptyProductForm(),
      name: PRODUCT_CREATION_FIXTURES.name,
      productType: PRODUCT_CREATION_FIXTURES.productType,
      description: PRODUCT_CREATION_FIXTURES.description,
      price: PRODUCT_CREATION_FIXTURES.price,
      durationMonths: PRODUCT_CREATION_FIXTURES.durationMonths,
      expirationPolicy: PRODUCT_CREATION_FIXTURES.expirationPolicy,
    });
    expect(errors).toEqual({});
    expect(hasProductFormErrors(errors)).toBe(false);
  });

  it("pins success notice and fixture identity for UI tests", () => {
    expect(PRODUCT_CREATED_NOTICE).toBe("Product created.");
    expect(PRODUCT_CREATION_FIXTURES.name).toContain("UI Created");
  });
});
