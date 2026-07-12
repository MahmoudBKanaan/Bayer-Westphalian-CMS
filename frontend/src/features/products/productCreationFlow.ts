/**
 * Product creation UI flow (KB product CRUD / FR product management / item 601).
 *
 * Pure rules for creating insurance and investment products through the React
 * Products page: form defaults, client-side validation, success copy, roles.
 */

import type { ProductFormPayload, ProductType, ProductView } from "@/api/products";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";
import { PRODUCT_TYPE_FILTERS } from "@/features/products/productSearch";

/** Roles that may create products through the UI (matches PRODUCT_MANAGE_ROLES). */
export const PRODUCT_CREATE_UI_ROLES: SystemRoleName[] = ["ADMIN", "PRODUCT_MANAGER"];

export const PRODUCT_PAGE_HEADING = "Products";
export const PRODUCT_CREATE_SECTION_HEADING = "Create product";
export const PRODUCT_CREATE_SECTION_HINT = "Insurance or investment offer for campaigns";
export const PRODUCT_CREATE_SUBMIT_LABEL = "Create product";
export const PRODUCT_CREATE_FORM_ARIA_LABEL = "Create product form";
export const PRODUCT_CREATED_NOTICE = "Product created.";
export const PRODUCT_LIST_TABLE_ARIA_LABEL = "Products table";

export const PRODUCT_CREATE_TYPES: ProductType[] = PRODUCT_TYPE_FILTERS.filter(
  (type): type is ProductType => type !== "ALL",
);

export const productFormValidationMessages = {
  nameRequired: "Product name is required.",
  nameMaxLength: "Product name must be 255 characters or fewer.",
  productTypeRequired: "Product type is required.",
  priceInvalid: "Enter a valid price (non-negative number).",
  durationInvalid: "Duration must be a positive whole number of months.",
  expirationPolicyMaxLength: "Expiration policy must be 100 characters or fewer.",
} as const;

export type ProductFormErrors = Partial<Record<keyof ProductFormPayload, string>>;

export type ProductCreationStepId =
  | "open-products"
  | "fill-create-form"
  | "submit-create"
  | "see-created-product";

export type ProductCreationStepDefinition = {
  id: ProductCreationStepId;
  index: number;
  title: string;
  description: string;
};

/** UI acceptance steps for “Product creation works through UI” (item 601). */
export const PRODUCT_CREATION_FLOW_STEPS: ProductCreationStepDefinition[] = [
  {
    id: "open-products",
    index: 0,
    title: "Open Products",
    description: "Authorized product manager or admin opens /products and sees the create panel.",
  },
  {
    id: "fill-create-form",
    index: 1,
    title: "Fill create form",
    description: "Enter name, type, description, price, duration, and expiration policy.",
  },
  {
    id: "submit-create",
    index: 2,
    title: "Submit create",
    description: "Client validation then POST /api/products with the product payload.",
  },
  {
    id: "see-created-product",
    index: 3,
    title: "See created product",
    description: "Success notice appears and the product is listed in the catalog.",
  },
];

/** Deterministic fixtures for Playwright / integration product creation. */
export const PRODUCT_CREATION_FIXTURES = {
  name: "UI Created Life Cover",
  productType: "LIFE_INSURANCE" as ProductType,
  description: "Controlled UI product creation fixture",
  price: "149.99",
  durationMonths: "12",
  expirationPolicy: "Annual renewal",
  id: "30000000-0000-0000-0000-00000000p601",
} as const;

export function emptyProductForm(): ProductFormPayload {
  return {
    name: "",
    productType: "LIFE_INSURANCE",
    description: "",
    price: "",
    durationMonths: "",
    expirationPolicy: "",
    active: true,
  };
}

/**
 * Validates create/edit product form fields before API calls.
 */
export function validateProductForm(value: ProductFormPayload): ProductFormErrors {
  const errors: ProductFormErrors = {};
  const name = value.name.trim();
  if (name.length === 0) {
    errors.name = productFormValidationMessages.nameRequired;
  } else if (value.name.length > 255) {
    errors.name = productFormValidationMessages.nameMaxLength;
  }

  if (value.productType == null || String(value.productType).trim().length === 0) {
    errors.productType = productFormValidationMessages.productTypeRequired;
  }

  const priceTrimmed = value.price.trim();
  if (priceTrimmed.length > 0) {
    const parsed = Number(priceTrimmed);
    if (!Number.isFinite(parsed) || parsed < 0) {
      errors.price = productFormValidationMessages.priceInvalid;
    }
  }

  const durationTrimmed = value.durationMonths.trim();
  if (durationTrimmed.length > 0) {
    if (!/^\d+$/.test(durationTrimmed) || Number.parseInt(durationTrimmed, 10) <= 0) {
      errors.durationMonths = productFormValidationMessages.durationInvalid;
    }
  }

  if (value.expirationPolicy.length > 100) {
    errors.expirationPolicy = productFormValidationMessages.expirationPolicyMaxLength;
  }

  return errors;
}

export function hasProductFormErrors(errors: ProductFormErrors): boolean {
  return Object.values(errors).some((message) => message != null && message.length > 0);
}

export function productViewToForm(product: ProductView): ProductFormPayload {
  return {
    name: product.name,
    productType: product.productType,
    description: product.description ?? "",
    price: product.price == null ? "" : String(product.price),
    durationMonths: product.durationMonths == null ? "" : String(product.durationMonths),
    expirationPolicy: product.expirationPolicy ?? "",
    active: product.active,
  };
}

export function canCreateProductsThroughUi(roles: readonly SystemRoleName[]): boolean {
  return roles.some((role) => PRODUCT_CREATE_UI_ROLES.includes(role));
}

export function productCreationStepIdsInOrder(): ProductCreationStepId[] {
  return [...PRODUCT_CREATION_FLOW_STEPS]
    .sort((left, right) => left.index - right.index)
    .map((step) => step.id);
}

export function formatProductCreationJourney(
  steps: readonly ProductCreationStepDefinition[] = PRODUCT_CREATION_FLOW_STEPS,
): string {
  return steps
    .slice()
    .sort((left, right) => left.index - right.index)
    .map((step) => step.title)
    .join(" → ");
}

export function isValidProductCreationOrder(
  observed: readonly ProductCreationStepId[],
): boolean {
  const expected = productCreationStepIdsInOrder();
  if (observed.length !== expected.length) {
    return false;
  }
  return observed.every((stepId, index) => stepId === expected[index]);
}
