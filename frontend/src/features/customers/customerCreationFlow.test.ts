import { describe, expect, it } from "vitest";
import {
  canCreateCustomersThroughUi,
  CUSTOMER_CREATE_UI_ROLES,
  CUSTOMER_CREATED_NOTICE,
  CUSTOMER_CREATION_FIXTURES,
  customerCreationStepIdsInOrder,
  customerFormValidationMessages,
  emptyCustomerForm,
  formatCustomerCreationJourney,
  hasCustomerFormErrors,
  isValidCustomerCreationOrder,
  validateCustomerForm,
} from "@/features/customers/customerCreationFlow";

describe("customerCreationFlow (item 599)", () => {
  it("documents the UI customer creation journey", () => {
    expect(customerCreationStepIdsInOrder()).toEqual([
      "open-customers",
      "fill-create-form",
      "submit-create",
      "see-created-customer",
    ]);
    expect(formatCustomerCreationJourney()).toBe(
      "Open Customers → Fill create form → Submit create → See created customer",
    );
    expect(isValidCustomerCreationOrder(customerCreationStepIdsInOrder())).toBe(true);
    expect(
      isValidCustomerCreationOrder(["submit-create", "open-customers"] as never),
    ).toBe(false);
  });

  it("allows only admin and customer-service roles to create through the UI", () => {
    expect(CUSTOMER_CREATE_UI_ROLES).toEqual(["ADMIN", "CUSTOMER_SERVICE_AGENT"]);
    expect(canCreateCustomersThroughUi(["ADMIN"])).toBe(true);
    expect(canCreateCustomersThroughUi(["CUSTOMER_SERVICE_AGENT"])).toBe(true);
    expect(canCreateCustomersThroughUi(["CAMPAIGN_MANAGER"])).toBe(false);
    expect(canCreateCustomersThroughUi(["BI_ANALYST", "COMPLIANCE_OFFICER"])).toBe(false);
  });

  it("requires first and last name before create is posted", () => {
    const errors = validateCustomerForm(emptyCustomerForm(), true);
    expect(errors.firstName).toBe(customerFormValidationMessages.firstNameRequired);
    expect(errors.lastName).toBe(customerFormValidationMessages.lastNameRequired);
    expect(hasCustomerFormErrors(errors)).toBe(true);
  });

  it("validates optional email and phone when provided", () => {
    const errors = validateCustomerForm(
      {
        ...emptyCustomerForm(),
        firstName: "Ada",
        lastName: "Lovelace",
        email: "not-an-email",
        phone: "CALLME",
      },
      true,
    );
    expect(errors.email).toBe(customerFormValidationMessages.emailInvalid);
    expect(errors.phone).toBe(customerFormValidationMessages.phoneInvalid);
  });

  it("accepts a well-formed create payload", () => {
    const errors = validateCustomerForm(
      {
        ...emptyCustomerForm(),
        customerType: CUSTOMER_CREATION_FIXTURES.customerType,
        firstName: CUSTOMER_CREATION_FIXTURES.firstName,
        lastName: CUSTOMER_CREATION_FIXTURES.lastName,
        email: CUSTOMER_CREATION_FIXTURES.email,
        phone: CUSTOMER_CREATION_FIXTURES.phone,
        city: CUSTOMER_CREATION_FIXTURES.city,
        country: CUSTOMER_CREATION_FIXTURES.country,
        status: CUSTOMER_CREATION_FIXTURES.status,
        source: CUSTOMER_CREATION_FIXTURES.source,
      },
      true,
    );
    expect(errors).toEqual({});
    expect(hasCustomerFormErrors(errors)).toBe(false);
  });

  it("rejects future dates of birth and overlong fields", () => {
    const future = new Date();
    future.setFullYear(future.getFullYear() + 1);
    const iso = future.toISOString().slice(0, 10);
    const errors = validateCustomerForm(
      {
        ...emptyCustomerForm(),
        firstName: "A".repeat(101),
        lastName: "Ok",
        dateOfBirth: iso,
      },
      true,
    );
    expect(errors.firstName).toBe(customerFormValidationMessages.maxLength(100));
    expect(errors.dateOfBirth).toBe(customerFormValidationMessages.dateOfBirthFuture);
  });

  it("pins success notice and fixture identity for UI tests", () => {
    expect(CUSTOMER_CREATED_NOTICE).toBe("Customer created.");
    expect(CUSTOMER_CREATION_FIXTURES.fullName).toBe(
      `${CUSTOMER_CREATION_FIXTURES.firstName} ${CUSTOMER_CREATION_FIXTURES.lastName}`,
    );
  });
});
