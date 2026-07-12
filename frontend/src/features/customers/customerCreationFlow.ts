/**
 * Customer creation UI flow (KB FR-011 / NFR-005 / item 599).
 *
 * Pure rules for creating customers/prospects through the React Customers page:
 * form defaults, client-side validation, success copy, and acceptance step order.
 */

import type {
  CustomerAgeGroup,
  CustomerFormPayload,
  CustomerStatus,
  CustomerType,
  CustomerView,
} from "@/api/customers";
import type { SystemRoleName } from "@/auth/sessionStorageStrategy";

/** Roles that may create customers/prospects through the UI (matches permissions). */
export const CUSTOMER_CREATE_UI_ROLES: SystemRoleName[] = ["ADMIN", "CUSTOMER_SERVICE_AGENT"];

export const CUSTOMER_CREATE_PAGE_HEADING = "Customers and prospects";
export const CUSTOMER_CREATE_SECTION_HEADING = "Create customer";
export const CUSTOMER_CREATE_SECTION_HINT = "Customer, prospect, or beneficiary profile";
export const CUSTOMER_CREATE_SUBMIT_LABEL = "Create customer";
export const CUSTOMER_CREATED_NOTICE = "Customer created.";
export const CUSTOMER_CREATE_FORM_ARIA_LABEL = "Create customer form";
export const CUSTOMER_LIST_TABLE_ARIA_LABEL = "Customer list table";

export const customerFormValidationMessages = {
  customerTypeRequired: "Customer type is required.",
  firstNameRequired: "First name is required.",
  lastNameRequired: "Last name is required.",
  emailInvalid: "Enter a valid email address.",
  phoneInvalid: "Use 7 to 50 digits, spaces, parentheses, hyphens, and an optional +.",
  dateOfBirthFuture: "Date of birth cannot be in the future.",
  maxLength: (maxLength: number) => `Must be ${maxLength} characters or fewer.`,
} as const;

export type CustomerFormErrors = Partial<Record<keyof CustomerFormPayload, string>>;

export type CustomerCreationStepId =
  | "open-customers"
  | "fill-create-form"
  | "submit-create"
  | "see-created-customer";

export type CustomerCreationStepDefinition = {
  id: CustomerCreationStepId;
  index: number;
  title: string;
  description: string;
};

/** UI acceptance steps for “Customer creation works through UI” (item 599). */
export const CUSTOMER_CREATION_FLOW_STEPS: CustomerCreationStepDefinition[] = [
  {
    id: "open-customers",
    index: 0,
    title: "Open Customers",
    description: "Authorized employee opens /customers and sees the create panel.",
  },
  {
    id: "fill-create-form",
    index: 1,
    title: "Fill create form",
    description: "Enter type, name, contact details, status, and optional demographics.",
  },
  {
    id: "submit-create",
    index: 2,
    title: "Submit create",
    description: "Client validation then POST /api/customers with the form payload.",
  },
  {
    id: "see-created-customer",
    index: 3,
    title: "See created customer",
    description: "Success notice appears and the new row is available in the customer list.",
  },
];

/** Deterministic fixtures for Playwright / integration customer creation. */
export const CUSTOMER_CREATION_FIXTURES = {
  firstName: "UI",
  lastName: "Created",
  email: "ui.created.customer@bayer-westphalian.test",
  phone: "+49-555-0199",
  city: "Munich",
  country: "Germany",
  source: "UI_CUSTOMER_CREATION",
  customerType: "CUSTOMER" as CustomerType,
  status: "ACTIVE" as CustomerStatus,
  fullName: "UI Created",
  id: "20000000-0000-0000-0000-00000000c199",
} as const;

export const emptyCustomerForm = (): CustomerFormPayload => ({
  customerType: "CUSTOMER",
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  addressLine: "",
  city: "",
  country: "",
  dateOfBirth: "",
  ageGroup: "",
  status: "ACTIVE",
  doNotContact: false,
  source: "",
});

/**
 * Validates create/edit customer form fields before API calls (FR-011 / form UX).
 */
export function validateCustomerForm(
  value: CustomerFormPayload,
  includeCustomerType: boolean,
): CustomerFormErrors {
  const errors: CustomerFormErrors = {};
  if (includeCustomerType && value.customerType == null) {
    errors.customerType = customerFormValidationMessages.customerTypeRequired;
  }
  if (value.firstName.trim().length === 0) {
    errors.firstName = customerFormValidationMessages.firstNameRequired;
  }
  if (value.lastName.trim().length === 0) {
    errors.lastName = customerFormValidationMessages.lastNameRequired;
  }
  if (value.email.trim().length > 0 && !isValidCustomerEmail(value.email)) {
    errors.email = customerFormValidationMessages.emailInvalid;
  }
  if (value.phone.trim().length > 0 && !isValidCustomerPhone(value.phone)) {
    errors.phone = customerFormValidationMessages.phoneInvalid;
  }
  if (value.dateOfBirth !== "" && new Date(value.dateOfBirth) > startOfToday()) {
    errors.dateOfBirth = customerFormValidationMessages.dateOfBirthFuture;
  }
  addMaxLengthError(errors, "firstName", value.firstName, 100);
  addMaxLengthError(errors, "lastName", value.lastName, 100);
  addMaxLengthError(errors, "email", value.email, 255);
  addMaxLengthError(errors, "phone", value.phone, 50);
  addMaxLengthError(errors, "addressLine", value.addressLine, 255);
  addMaxLengthError(errors, "city", value.city, 100);
  addMaxLengthError(errors, "country", value.country, 100);
  addMaxLengthError(errors, "source", value.source, 100);
  return errors;
}

export function hasCustomerFormErrors(errors: CustomerFormErrors): boolean {
  return Object.values(errors).some((message) => message != null && message.length > 0);
}

export function customerViewToForm(customer: CustomerView): CustomerFormPayload {
  return {
    customerType: customer.customerType,
    firstName: customer.firstName,
    lastName: customer.lastName,
    email: customer.email ?? "",
    phone: customer.phone ?? "",
    addressLine: customer.addressLine ?? "",
    city: customer.city ?? "",
    country: customer.country ?? "",
    dateOfBirth: customer.dateOfBirth ?? "",
    ageGroup: (customer.ageGroup as CustomerAgeGroup | null) ?? "",
    status: customer.status,
    doNotContact: customer.doNotContact,
    source: customer.source ?? "",
  };
}

export function customerCreationStepIdsInOrder(): CustomerCreationStepId[] {
  return [...CUSTOMER_CREATION_FLOW_STEPS]
    .sort((left, right) => left.index - right.index)
    .map((step) => step.id);
}

export function formatCustomerCreationJourney(
  steps: readonly CustomerCreationStepDefinition[] = CUSTOMER_CREATION_FLOW_STEPS,
): string {
  return steps
    .slice()
    .sort((left, right) => left.index - right.index)
    .map((step) => step.title)
    .join(" → ");
}

export function isValidCustomerCreationOrder(
  observed: readonly CustomerCreationStepId[],
): boolean {
  const expected = customerCreationStepIdsInOrder();
  if (observed.length !== expected.length) {
    return false;
  }
  return observed.every((stepId, index) => stepId === expected[index]);
}

export function canCreateCustomersThroughUi(roles: readonly SystemRoleName[]): boolean {
  return roles.some((role) => CUSTOMER_CREATE_UI_ROLES.includes(role));
}

export function isValidCustomerEmail(value: string): boolean {
  return /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(value.trim());
}

export function isValidCustomerPhone(value: string): boolean {
  return /^\+?[0-9 ()-]{7,50}$/.test(value.trim());
}

function addMaxLengthError(
  errors: CustomerFormErrors,
  field: keyof CustomerFormPayload,
  value: string,
  maxLength: number,
) {
  if (value.length > maxLength) {
    errors[field] = customerFormValidationMessages.maxLength(maxLength);
  }
}

function startOfToday() {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return today;
}
