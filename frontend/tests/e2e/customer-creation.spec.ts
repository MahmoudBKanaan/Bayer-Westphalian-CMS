import { expect, test } from "@playwright/test";
import {
  CUSTOMER_CREATE_FORM_ARIA_LABEL,
  CUSTOMER_CREATE_SECTION_HEADING,
  CUSTOMER_CREATE_SUBMIT_LABEL,
  CUSTOMER_CREATED_NOTICE,
  CUSTOMER_CREATION_FIXTURES,
  CUSTOMER_LIST_TABLE_ARIA_LABEL,
  customerFormValidationMessages,
} from "../../src/features/customers/customerCreationFlow";
import { installHappyPathApiMock } from "./helpers/installHappyPathApiMock";
import { loginAsHappyPathAdmin } from "./helpers/uiActions";

/**
 * Item 599 — Customer creation works through UI (Playwright).
 *
 * Focused acceptance for FR-011 create flow after login.
 */
test.describe("Customer creation through UI (item 599)", () => {
  test.beforeEach(async ({ page }) => {
    await installHappyPathApiMock(page);
    await loginAsHappyPathAdmin(page);
  });

  test("creates a customer from the Customers page", async ({ page }) => {
    await page.getByRole("link", { name: "Customers" }).click();
    await expect(page.getByRole("heading", { name: CUSTOMER_CREATE_SECTION_HEADING })).toBeVisible();

    const form = page.getByRole("form", { name: CUSTOMER_CREATE_FORM_ARIA_LABEL });
    await form.getByLabel("First name").fill(CUSTOMER_CREATION_FIXTURES.firstName);
    await form.getByLabel("Last name").fill(CUSTOMER_CREATION_FIXTURES.lastName);
    await form.getByLabel("Email", { exact: true }).fill(CUSTOMER_CREATION_FIXTURES.email);
    await form.getByLabel("Phone").fill(CUSTOMER_CREATION_FIXTURES.phone);
    await form.getByLabel("City").fill(CUSTOMER_CREATION_FIXTURES.city);
    await form.getByLabel("Country").fill(CUSTOMER_CREATION_FIXTURES.country);
    await form.getByLabel("Source").fill(CUSTOMER_CREATION_FIXTURES.source);
    await form.getByRole("button", { name: CUSTOMER_CREATE_SUBMIT_LABEL }).click();

    await expect(page.getByText(CUSTOMER_CREATED_NOTICE)).toBeVisible();
    await expect(
      page.getByRole("table", { name: CUSTOMER_LIST_TABLE_ARIA_LABEL }).getByText(
        CUSTOMER_CREATION_FIXTURES.fullName,
      ),
    ).toBeVisible();
  });

  test("shows create validation without posting incomplete data", async ({ page }) => {
    await page.getByRole("link", { name: "Customers" }).click();
    const form = page.getByRole("form", { name: CUSTOMER_CREATE_FORM_ARIA_LABEL });
    await form.getByLabel("Email", { exact: true }).fill("not-an-email");
    await form.getByLabel("Phone").fill("CALLME");
    await form.getByRole("button", { name: CUSTOMER_CREATE_SUBMIT_LABEL }).click();

    await expect(page.getByText(customerFormValidationMessages.firstNameRequired)).toBeVisible();
    await expect(page.getByText(customerFormValidationMessages.lastNameRequired)).toBeVisible();
    await expect(page.getByText(customerFormValidationMessages.emailInvalid)).toBeVisible();
    await expect(page.getByText(customerFormValidationMessages.phoneInvalid)).toBeVisible();
    await expect(page.getByText(CUSTOMER_CREATED_NOTICE)).toHaveCount(0);
  });
});
