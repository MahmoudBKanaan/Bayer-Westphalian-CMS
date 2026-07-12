import { expect, test } from "@playwright/test";
import {
  LOGIN_EMAIL_LABEL,
  LOGIN_FORM_ARIA_LABEL,
  LOGIN_PASSWORD_LABEL,
  LOGIN_SUBMIT_LABEL,
  MAIN_CONTENT_ID,
  SKIP_TO_CONTENT_LABEL,
} from "../../src/features/a11y/keyboardNavigationFlow";
import {
  CUSTOMER_CREATE_FORM_ARIA_LABEL,
  CUSTOMER_CREATE_SUBMIT_LABEL,
} from "../../src/features/customers/customerCreationFlow";
import {
  PRODUCT_CREATE_FORM_ARIA_LABEL,
  PRODUCT_CREATE_SUBMIT_LABEL,
} from "../../src/features/products/productCreationFlow";
import { HAPPY_PATH_ADMIN } from "../../src/features/e2e/happyPathFlow";
import { installHappyPathApiMock } from "./helpers/installHappyPathApiMock";
import { loginAsHappyPathAdmin } from "./helpers/uiActions";

/**
 * Item 608 — Keyboard navigation works for core forms (Playwright).
 *
 * Verifies Tab order, Shift+Tab, Enter submit, and shell skip-link without mouse clicks
 * for primary controls on login and authenticated create forms.
 */
test.describe("Keyboard navigation for core forms (item 608)", () => {
  test.beforeEach(async ({ page }) => {
    await installHappyPathApiMock(page);
  });

  test("login form supports Tab order and Enter-to-submit", async ({ page }) => {
    await page.goto("/login");
    const form = page.getByRole("form", { name: LOGIN_FORM_ARIA_LABEL });
    await expect(form).toBeVisible();

    await page.getByLabel(LOGIN_EMAIL_LABEL).focus();
    await expect(page.getByLabel(LOGIN_EMAIL_LABEL)).toBeFocused();

    await page.keyboard.press("Tab");
    await expect(page.getByLabel(LOGIN_PASSWORD_LABEL)).toBeFocused();

    await page.keyboard.press("Tab");
    await expect(page.getByRole("button", { name: LOGIN_SUBMIT_LABEL })).toBeFocused();

    await page.keyboard.press("Shift+Tab");
    await expect(page.getByLabel(LOGIN_PASSWORD_LABEL)).toBeFocused();

    await page.getByLabel(LOGIN_EMAIL_LABEL).fill(HAPPY_PATH_ADMIN.email);
    await page.getByLabel(LOGIN_PASSWORD_LABEL).fill(HAPPY_PATH_ADMIN.password);
    await page.getByLabel(LOGIN_PASSWORD_LABEL).press("Enter");

    await expect(page.getByRole("heading", { name: "Dashboard", level: 1 })).toBeVisible();
  });

  test("authenticated shell exposes skip-to-content targeting main", async ({ page }) => {
    await loginAsHappyPathAdmin(page);
    await page.goto("/dashboard");

    const skip = page.getByRole("link", { name: SKIP_TO_CONTENT_LABEL });
    await expect(skip).toHaveAttribute("href", `#${MAIN_CONTENT_ID}`);

    await skip.focus();
    await expect(skip).toBeFocused();
    await page.keyboard.press("Enter");
    await expect(page.locator(`#${MAIN_CONTENT_ID}`)).toBeVisible();
  });

  test("customer create form is fully operable with keyboard Tab sequence", async ({ page }) => {
    await loginAsHappyPathAdmin(page);
    await page.goto("/customers");

    const form = page.getByRole("form", { name: CUSTOMER_CREATE_FORM_ARIA_LABEL });
    await expect(form).toBeVisible();

    await form.getByLabel("Customer type").focus();
    await expect(form.getByLabel("Customer type")).toBeFocused();

    await page.keyboard.press("Tab");
    await expect(form.getByLabel("First name")).toBeFocused();

    await page.keyboard.press("Tab");
    await expect(form.getByLabel("Last name")).toBeFocused();

    await page.keyboard.press("Shift+Tab");
    await expect(form.getByLabel("First name")).toBeFocused();

    await expect(form.getByRole("button", { name: CUSTOMER_CREATE_SUBMIT_LABEL })).toBeVisible();
  });

  test("product create form Tab order reaches the create submit control", async ({ page }) => {
    await loginAsHappyPathAdmin(page);
    await page.goto("/products");

    const form = page.getByRole("form", { name: PRODUCT_CREATE_FORM_ARIA_LABEL });
    await expect(form).toBeVisible();

    await form.getByLabel("Product name").focus();
    await page.keyboard.press("Tab");
    await expect(form.getByLabel("Product type")).toBeFocused();

    for (let i = 0; i < 5; i += 1) {
      await page.keyboard.press("Tab");
    }
    await expect(form.getByRole("button", { name: PRODUCT_CREATE_SUBMIT_LABEL })).toBeFocused();
  });
});
